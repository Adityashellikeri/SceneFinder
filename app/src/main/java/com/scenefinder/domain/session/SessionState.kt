package com.scenefinder.domain.session

import com.scenefinder.domain.model.MediaMatchCandidate

sealed interface SessionState {
    data object Disarmed : SessionState

    data class Armed(
        val startedAtMillis: Long = System.currentTimeMillis(),
        val remainingSeconds: Int = 45
    ) : SessionState

    data class Captured(
        val nodeCount: Int,
        val timestamp: Long = System.currentTimeMillis(),
        val dumpFilePath: String? = null,
        val sampleText: String = "",
        val candidates: List<MediaMatchCandidate> = emptyList()
    ) : SessionState

    data class Cancelled(val reason: String = "User cancelled") : SessionState

    data class TimedOut(val message: String = "Session timed out after 45s") : SessionState
}
