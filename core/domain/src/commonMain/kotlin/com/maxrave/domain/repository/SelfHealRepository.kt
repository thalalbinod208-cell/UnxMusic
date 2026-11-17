package com.maxrave.domain.repository

import com.maxrave.domain.data.entities.BugTicketEntity
import kotlinx.coroutines.flow.Flow

interface SelfHealRepository {
    val tickets: Flow<List<BugTicketEntity>>
    suspend fun record(ticket: BugTicketEntity)
    suspend fun recordSelfHeal(errorHash: String, succeeded: Boolean, at: Long)
    suspend fun saveSolution(errorHash: String, rootCause: String, steps: String, patch: String, action: String)
    suspend fun syncGitHubIssues(repository: String): Result<Int>
    suspend fun deleteGitHubIssues()
}
