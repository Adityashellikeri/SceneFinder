package com.scenefinder.domain.session

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var sessionManager: SessionManager

    @Before
    fun setup() {
        sessionManager = SessionManager(dispatcher = testDispatcher)
    }

    @Test
    fun `initial state is Disarmed`() {
        assertEquals(SessionState.Disarmed, sessionManager.state.value)
    }

    @Test
    fun `arm transitions state to Armed`() {
        sessionManager.arm(timeoutSeconds = 45)
        val current = sessionManager.state.value
        assertTrue(current is SessionState.Armed)
        assertEquals(45, (current as SessionState.Armed).remainingSeconds)
    }

    @Test
    fun `arm times out after specified seconds`() = runTest(testDispatcher) {
        val manager = SessionManager(dispatcher = testDispatcher)
        manager.arm(timeoutSeconds = 3)

        testScheduler.advanceTimeBy(1000)
        testScheduler.runCurrent()
        assertEquals(2, (manager.state.value as SessionState.Armed).remainingSeconds)

        testScheduler.advanceTimeBy(1000)
        testScheduler.runCurrent()
        assertEquals(1, (manager.state.value as SessionState.Armed).remainingSeconds)

        testScheduler.advanceTimeBy(1001)
        testScheduler.runCurrent()
        assertTrue(manager.state.value is SessionState.TimedOut)
    }

    @Test
    fun `cancel transitions state to Cancelled`() {
        sessionManager.arm(timeoutSeconds = 45)
        sessionManager.cancel("User cancelled")
        val current = sessionManager.state.value
        assertTrue(current is SessionState.Cancelled)
        assertEquals("User cancelled", (current as SessionState.Cancelled).reason)
    }

    @Test
    fun `onCaptured transitions to Captured and cancels timeout`() = runTest(testDispatcher) {
        val manager = SessionManager(dispatcher = testDispatcher)
        manager.arm(timeoutSeconds = 10)

        manager.onCaptured(
            nodeCount = 42,
            filePath = "/data/dump.txt",
            preview = "Sample Text"
        )

        val current = manager.state.value
        assertTrue(current is SessionState.Captured)
        val captured = current as SessionState.Captured
        assertEquals(42, captured.nodeCount)
        assertEquals("/data/dump.txt", captured.dumpFilePath)
        assertEquals("Sample Text", captured.sampleText)

        // Advance time - should stay captured, not time out
        testScheduler.advanceTimeBy(20000)
        assertTrue(manager.state.value is SessionState.Captured)
    }

    @Test
    fun `disarm resets state to Disarmed`() {
        sessionManager.arm(timeoutSeconds = 45)
        sessionManager.disarm()
        assertEquals(SessionState.Disarmed, sessionManager.state.value)
    }
}
