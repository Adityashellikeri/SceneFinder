package com.scenefinder.domain.session

import com.scenefinder.domain.model.MediaMatchCandidate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val dispatcher: CoroutineDispatcher
) {
    private val scope = CoroutineScope(dispatcher)
    private var timeoutJob: Job? = null

    private val _state = MutableStateFlow<SessionState>(SessionState.Disarmed)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    fun arm(timeoutSeconds: Int = 45) {
        timeoutJob?.cancel()
        _state.value = SessionState.Armed(remainingSeconds = timeoutSeconds)
        log("Session armed ($timeoutSeconds s timeout)")

        timeoutJob = scope.launch {
            for (remaining in (timeoutSeconds - 1) downTo 0) {
                delay(1000L)
                val current = _state.value
                if (current is SessionState.Armed) {
                    _state.value = current.copy(remainingSeconds = remaining)
                } else {
                    return@launch
                }
            }
            if (_state.value is SessionState.Armed) {
                _state.value = SessionState.TimedOut()
                log("Session timed out")
            }
        }
    }

    fun cancel(reason: String = "User cancelled") {
        timeoutJob?.cancel()
        timeoutJob = null
        _state.value = SessionState.Cancelled(reason)
        log("Session cancelled: $reason")
    }

    fun onCaptured(
        nodeCount: Int,
        filePath: String?,
        preview: String,
        candidates: List<MediaMatchCandidate> = emptyList()
    ) {
        timeoutJob?.cancel()
        timeoutJob = null
        _state.value = SessionState.Captured(
            nodeCount = nodeCount,
            dumpFilePath = filePath,
            sampleText = preview,
            candidates = candidates
        )
        val bestCandidate = candidates.firstOrNull()?.cleanTitle ?: "None"
        log("Session captured $nodeCount text nodes ($filePath). Best candidate: $bestCandidate")
    }

    fun disarm() {
        timeoutJob?.cancel()
        timeoutJob = null
        _state.value = SessionState.Disarmed
        log("Session disarmed")
    }

    fun log(message: String) {
        val entry = "[${java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())}] $message"
        _logs.value = (_logs.value + entry).takeLast(500)
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}
