package com.maxrave.domain.selfheal

import okio.ByteString.Companion.encodeUtf8

/** Stable SHA-256 dedupe key: exception identifier, normalized top frames and transport code. */
object ErrorFingerprintGenerator {
    fun generate(exceptionType: String, stackTrace: String, errorCode: String = ""): String {
        val frames = stackTrace.lineSequence()
            .map(String::trim)
            .filter { it.startsWith("at ") || it.startsWith("#") || it.startsWith("-") }
            .take(3)
            .joinToString("\n")
        return "$exceptionType\n$frames\n$errorCode".encodeUtf8().sha256().hex()
    }
}
