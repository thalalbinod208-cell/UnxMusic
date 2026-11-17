package com.maxrave.simpmusic.selfheal.catcher

import com.maxrave.domain.data.entities.BugTicketEntity
import com.maxrave.domain.repository.SelfHealRepository
import com.maxrave.domain.selfheal.ErrorFingerprintGenerator
import kotlin.time.Clock

/** Safe reporting entry point for Ktor/OkHttp boundaries. Auth headers and URL query values are never persisted. */
class NetworkErrorInterceptor(private val repository: SelfHealRepository) {
    suspend fun recordFailure(url: String, statusCode: Int?, message: String, stackTrace: String = "") {
        val host = url.substringAfter("://", "").substringBefore('/').substringBefore('?').take(120)
        val sanitized = message
            .replace(Regex("(?i)(authorization|cookie|set-cookie|token|key)\\s*[:=]\\s*[^\\s,;]+"), "$1=[redacted]")
            .take(2_000)
        val now = Clock.System.now().toEpochMilliseconds()
        val code = statusCode?.toString() ?: "TIMEOUT"
        val hash = ErrorFingerprintGenerator.generate("NETWORK_ERROR", "$host $sanitized", code)
        repository.record(
            BugTicketEntity(
                errorHash = hash,
                source = "NETWORK_ERROR",
                title = "Network failure${statusCode?.let { " (HTTP $it)" } ?: " (timeout)"}",
                category = if (statusCode == 401 || statusCode == 429) "AUTH_SYNC" else "NETWORK_PERF",
                severity = if (statusCode == 401 || statusCode == 403) "HIGH" else "MEDIUM",
                stackTrace = stackTrace.take(12_000),
                deviceContext = "host=$host; code=$code",
                createdAt = now,
                updatedAt = now,
            ),
        )
    }
}
