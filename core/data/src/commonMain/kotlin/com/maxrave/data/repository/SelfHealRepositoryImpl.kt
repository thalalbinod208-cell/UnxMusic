package com.maxrave.data.repository

import com.maxrave.data.db.BugTicketDao
import com.maxrave.domain.data.entities.BugTicketEntity
import com.maxrave.domain.repository.SelfHealRepository
import com.maxrave.domain.selfheal.ErrorFingerprintGenerator
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock

internal class SelfHealRepositoryImpl(
    private val dao: BugTicketDao,
    private val httpClient: HttpClient,
) : SelfHealRepository {
    override val tickets: Flow<List<BugTicketEntity>> = dao.observeAll()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun record(ticket: BugTicketEntity) = withContext(Dispatchers.IO) { dao.record(ticket) }

    override suspend fun recordSelfHeal(errorHash: String, succeeded: Boolean, at: Long) =
        withContext(Dispatchers.IO) { dao.recordSelfHeal(errorHash, if (succeeded) 1 else 0, at) }

    override suspend fun saveSolution(errorHash: String, rootCause: String, steps: String, patch: String, action: String) =
        withContext(Dispatchers.IO) {
            dao.saveSolution(errorHash, rootCause, steps, patch, action, Clock.System.now().toEpochMilliseconds())
        }

    override suspend fun deleteGitHubIssues() = withContext(Dispatchers.IO) { dao.deleteGitHubIssues() }

    override suspend fun syncGitHubIssues(repository: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val repo = repository.trim().removePrefix("https://github.com/").trimEnd('/')
            require(repo.matches(Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+"))) { "Enter a GitHub repository as owner/name" }
            var page = 1
            var count = 0
            while (true) {
                val response = httpClient.get("https://api.github.com/repos/$repo/issues") {
                    url {
                        parameters.append("state", "all")
                        parameters.append("per_page", "100")
                        parameters.append("page", page.toString())
                    }
                    header(HttpHeaders.Accept, "application/vnd.github+json")
                    header("X-GitHub-Api-Version", "2022-11-28")
                    header(HttpHeaders.UserAgent, "UnX-Music-Android")
                }
                check(response.status.value in 200..299) { "GitHub returned HTTP ${response.status.value}" }
                val rows = json.decodeFromString<JsonArray>(response.bodyAsText())
                if (rows.isEmpty()) break
                rows.filterIsInstance<JsonObject>()
                    .filterNot { it.containsKey("pull_request") }
                    .forEach { issue -> dao.put(issue.toTicket(repo)) }
                count += rows.count { it is JsonObject && !it.containsKey("pull_request") }
                if (rows.size < 100) break
                page++
            }
            count
        }
    }

    private fun JsonObject.toTicket(repository: String): BugTicketEntity {
        val number = this["number"]?.jsonPrimitive?.intOrNull ?: 0
        val title = this["title"]?.jsonPrimitive?.contentOrNull.orEmpty().take(500)
        val body = this["body"]?.jsonPrimitive?.contentOrNull.orEmpty().take(24_000)
        val state = this["state"]?.jsonPrimitive?.contentOrNull ?: "open"
        val updated = this["updated_at"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val category = classify(title + "\n" + body)
        val createdAt = this["created_at"]?.jsonPrimitive?.contentOrNull?.toEpochMillisOrNull() ?: Clock.System.now().toEpochMilliseconds()
        val updatedAt = updated.toEpochMillisOrNull() ?: createdAt
        val fingerprint = ErrorFingerprintGenerator.generate("GITHUB_ISSUE", "$repository#$number")
        return BugTicketEntity(
            errorHash = fingerprint,
            source = "GITHUB_ISSUE",
            githubIssueNumber = number,
            title = title.ifBlank { "GitHub issue #$number" },
            category = category,
            severity = severityFor(title, body),
            stackTrace = body,
            deviceContext = "${this["html_url"]?.jsonPrimitive?.contentOrNull.orEmpty()}\nstate=$state",
            status = if (state == "closed") "CLOSED" else "UNSOLVED",
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun classify(text: String): String = when {
        "403" in text || "playback" in text || "stream" in text || "audio" in text -> "PLAYBACK_403"
        "login" in text || "auth" in text || "cookie" in text || "account" in text -> "AUTH_SYNC"
        "lyric" in text || "translation" in text -> "LYRICS_UI"
        "crash" in text || "exception" in text || "fatal" in text -> "CRITICAL_CRASH"
        "slow" in text || "timeout" in text || "network" in text || "loading" in text -> "NETWORK_PERF"
        else -> "LYRICS_UI"
    }

    private fun severityFor(title: String, body: String): String {
        val text = "$title $body".lowercase()
        return when {
            "crash" in text || "won't open" in text || "doesn't open" in text -> "CRITICAL"
            "playback" in text || "login" in text || "download" in text -> "HIGH"
            "slow" in text || "timeout" in text || "not loading" in text -> "MEDIUM"
            else -> "LOW"
        }
    }

    private fun String.toEpochMillisOrNull(): Long? =
        runCatching { kotlinx.datetime.Instant.parse(this).toEpochMilliseconds() }.getOrNull()
}
