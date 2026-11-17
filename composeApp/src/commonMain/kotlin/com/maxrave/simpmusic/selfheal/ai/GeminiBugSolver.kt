package com.maxrave.simpmusic.selfheal.ai

import com.maxrave.domain.data.entities.BugTicketEntity
import com.maxrave.domain.repository.SelfHealRepository
import com.maxrave.domain.selfheal.BugSolution
import org.simpmusic.aiservice.AiClient

/** Reuses the API key and provider settings already configured for UnX Music's AI lyrics feature. */
class GeminiBugSolver(
    private val aiClient: AiClient,
    private val repository: SelfHealRepository,
) {
    suspend fun solve(ticket: BugTicketEntity): Result<BugSolution> {
        val prompt = buildString {
            appendLine("Analyze this UnX Music Android bug ticket.")
            appendLine("Title: ${ticket.title}")
            appendLine("Source: ${ticket.source}; category: ${ticket.category}; severity: ${ticket.severity}")
            appendLine("Occurrences: ${ticket.occurrenceCount}")
            appendLine("Context: ${ticket.deviceContext.take(2_000)}")
            appendLine("Stack trace or public issue body:")
            appendLine(ticket.stackTrace.take(12_000))
            appendLine("Return rootCause, suspectedFiles, stepByStepFix, kotlinPatch, recommendedSelfHealRule.")
        }
        return aiClient.solveBug(prompt).onSuccess { solution ->
            repository.saveSolution(ticket.errorHash, solution.rootCause, solution.stepByStepFix, solution.kotlinPatch, solution.recommendedSelfHealRule)
        }
    }

    suspend fun askFollowUp(ticket: BugTicketEntity, question: String): Result<BugSolution> =
        aiClient.solveBug(
            "Follow-up about ticket '${ticket.title}'. Existing diagnosis: ${ticket.aiRootCause.orEmpty()}. " +
                "Existing fix: ${ticket.aiStepByStepFix.orEmpty()}. Question: ${question.take(2_000)}. " +
                "Respond in the normal structured fields; do not claim to have run code or tests.",
        )
}
