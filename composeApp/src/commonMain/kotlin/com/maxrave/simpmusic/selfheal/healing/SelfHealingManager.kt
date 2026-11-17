package com.maxrave.simpmusic.selfheal.healing

import com.maxrave.domain.repository.SelfHealRepository
import kotlin.time.Clock

/** Provides a bounded, opt-in retry primitive; playback-specific re-extraction stays with Media3. */
class SelfHealingManager(private val repository: SelfHealRepository) {
    suspend fun <T> retryOnce(errorHash: String, retry: suspend () -> T): Result<T> {
        val first = runCatching { retry() }
        if (first.isSuccess) return first
        val second = runCatching { retry() }
        repository.recordSelfHeal(errorHash, second.isSuccess, Clock.System.now().toEpochMilliseconds())
        return second
    }
}
