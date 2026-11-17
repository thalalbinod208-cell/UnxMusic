package com.maxrave.domain.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Local diagnostics and explicitly synchronized public issue metadata. Never stores auth cookies. */
@Entity(
    tableName = "bug_ticket",
    indices = [Index(value = ["errorHash"], unique = true), Index(value = ["source", "updatedAt"])],
)
data class BugTicketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val errorHash: String,
    val source: String,
    val githubIssueNumber: Int? = null,
    val title: String,
    val category: String,
    val severity: String,
    val stackTrace: String = "",
    val deviceContext: String = "",
    val occurrenceCount: Int = 1,
    val status: String = "UNSOLVED",
    val aiRootCause: String? = null,
    val aiStepByStepFix: String? = null,
    val aiKotlinPatch: String? = null,
    val aiSelfHealAction: String? = null,
    val selfHealAttempts: Int = 0,
    val selfHealSucceeded: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
)
