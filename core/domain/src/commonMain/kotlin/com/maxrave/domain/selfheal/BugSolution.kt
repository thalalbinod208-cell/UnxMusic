package com.maxrave.domain.selfheal

import kotlinx.serialization.Serializable

@Serializable
data class BugSolution(
    val rootCause: String,
    val suspectedFiles: List<String> = emptyList(),
    val stepByStepFix: String,
    val kotlinPatch: String = "",
    val recommendedSelfHealRule: String = "",
)
