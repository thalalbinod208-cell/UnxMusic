package com.maxrave.domain.selfheal

import kotlinx.serialization.json.Json

object BugSolutionParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(raw: String): BugSolution {
        val cleaned = Regex("```(?:json)?\\s*([\\s\\S]*?)```").find(raw)?.groupValues?.getOrNull(1) ?: raw
        return json.decodeFromString(cleaned.trim())
    }
}
