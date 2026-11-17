package com.maxrave.domain.selfheal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ErrorFingerprintGeneratorTest {
    @Test fun fingerprintIsStableAndUsesErrorCode() {
        val trace = "failure\n at package.First.call(First.kt:1)\n at package.Second.call(Second.kt:2)"
        assertEquals(
            ErrorFingerprintGenerator.generate("IOException", trace, "403"),
            ErrorFingerprintGenerator.generate("IOException", trace, "403"),
        )
        assertNotEquals(
            ErrorFingerprintGenerator.generate("IOException", trace, "403"),
            ErrorFingerprintGenerator.generate("IOException", trace, "429"),
        )
    }
}
