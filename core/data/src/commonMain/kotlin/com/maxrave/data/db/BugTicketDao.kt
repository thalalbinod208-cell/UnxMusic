package com.maxrave.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.maxrave.domain.data.entities.BugTicketEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BugTicketDao {
    @Query("SELECT * FROM bug_ticket ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<BugTicketEntity>>

    @Query("SELECT * FROM bug_ticket WHERE errorHash = :hash LIMIT 1")
    suspend fun findByHash(hash: String): BugTicketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(ticket: BugTicketEntity)

    @Transaction
    suspend fun record(ticket: BugTicketEntity) {
        val previous = findByHash(ticket.errorHash)
        put(
            if (previous == null) ticket else previous.copy(
                title = ticket.title,
                stackTrace = ticket.stackTrace.ifBlank { previous.stackTrace },
                deviceContext = ticket.deviceContext.ifBlank { previous.deviceContext },
                occurrenceCount = previous.occurrenceCount + 1,
                updatedAt = ticket.updatedAt,
            ),
        )
    }

    @Query("UPDATE bug_ticket SET selfHealAttempts = selfHealAttempts + 1, selfHealSucceeded = selfHealSucceeded + :success, updatedAt = :at WHERE errorHash = :hash")
    suspend fun recordSelfHeal(hash: String, success: Int, at: Long)

    @Query("UPDATE bug_ticket SET aiRootCause = :rootCause, aiStepByStepFix = :steps, aiKotlinPatch = :patch, aiSelfHealAction = :action, status = 'AI_SOLVED', updatedAt = :at WHERE errorHash = :hash")
    suspend fun saveSolution(hash: String, rootCause: String, steps: String, patch: String, action: String, at: Long)

    @Query("DELETE FROM bug_ticket WHERE source = 'GITHUB_ISSUE'")
    suspend fun deleteGitHubIssues()
}
