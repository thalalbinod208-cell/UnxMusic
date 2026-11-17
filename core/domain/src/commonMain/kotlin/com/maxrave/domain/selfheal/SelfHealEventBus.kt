package com.maxrave.domain.selfheal

/** Lightweight bridge for Media3 and network layers that must not depend on the app or database. */
object SelfHealEventBus {
    @Volatile
    var onPlaybackFailure: ((title: String, mediaId: String, code: Int, message: String) -> Unit)? = null

    fun reportPlaybackFailure(title: String, mediaId: String, code: Int, message: String) {
        onPlaybackFailure?.invoke(title, mediaId, code, message)
    }
}
