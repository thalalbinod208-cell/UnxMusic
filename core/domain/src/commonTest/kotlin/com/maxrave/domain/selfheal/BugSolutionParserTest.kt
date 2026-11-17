package com.maxrave.domain.selfheal

import kotlin.test.Test
import kotlin.test.assertEquals

class BugSolutionParserTest {
    @Test fun parsesStructuredAndFencedJson() {
        val raw = """```json
            {"rootCause":"expired URL","suspectedFiles":["Player.kt"],"stepByStepFix":"refresh source","kotlinPatch":"diff","recommendedSelfHealRule":"retry once"}
        ```"""
        val result = BugSolutionParser.parse(raw)
        assertEquals("expired URL", result.rootCause)
        assertEquals(listOf("Player.kt"), result.suspectedFiles)
        assertEquals("retry once", result.recommendedSelfHealRule)
    }
}
