package com.scenefinder.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.scenefinder.data.prefs.AppSettings
import com.scenefinder.data.repository.MediaEnrichmentRepository
import com.scenefinder.domain.model.AccessibilityDumpNode
import com.scenefinder.domain.ocr.ScreenOcrService
import com.scenefinder.domain.parser.CandidateExtractor
import com.scenefinder.domain.session.SessionManager
import com.scenefinder.domain.session.SessionState
import com.scenefinder.ui.overlay.OverlayActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class SceneAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var candidateExtractor: CandidateExtractor

    @Inject
    lateinit var ocrService: ScreenOcrService

    @Inject
    lateinit var enrichmentRepository: MediaEnrichmentRepository

    @Inject
    lateinit var appSettings: AppSettings

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var debounceJob: Job? = null

    companion object {
        const val GOOGLE_SEARCHBOX_PACKAGE = "com.google.android.googlequicksearchbox"
        private const val MAX_DEPTH = 30
        private const val MAX_NODES = 600
        private const val DEBOUNCE_DELAY_MS = 350L

        var instance: SceneAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        disarmService()

        serviceScope.launch {
            sessionManager.state.collect { state ->
                when (state) {
                    is SessionState.Armed -> armService()
                    is SessionState.Disarmed,
                    is SessionState.Captured,
                    is SessionState.Cancelled,
                    is SessionState.TimedOut -> disarmService()
                }
            }
        }
    }

    private fun armService() {
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.packageNames = arrayOf(GOOGLE_SEARCHBOX_PACKAGE)
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        info.notificationTimeout = 100
        serviceInfo = info
        sessionManager.log("AccessibilityService armed strictly for $GOOGLE_SEARCHBOX_PACKAGE")
    }

    private fun disarmService() {
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = 0
        info.packageNames = null
        serviceInfo = info
        sessionManager.log("AccessibilityService disarmed (ignoring events)")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // STRICT GUARD: If not explicitly armed by our shortcut/side-panel, ignore everything!
        val currentState = sessionManager.state.value
        if (currentState !is SessionState.Armed) return

        if (event == null) return
        val pkg = event.packageName?.toString()
        if (pkg != GOOGLE_SEARCHBOX_PACKAGE) {
            return
        }

        debounceJob?.cancel()
        debounceJob = serviceScope.launch {
            delay(DEBOUNCE_DELAY_MS)
            captureActiveWindow()
        }
    }

    fun captureActiveWindow() {
        val root = rootInActiveWindow ?: run {
            triggerOcrFallback("rootInActiveWindow is null")
            return
        }
        val collected = mutableListOf<AccessibilityDumpNode>()
        try {
            walkTree(root, 0, collected)
        } finally {
            root.recycle()
        }

        val textNodes = collected.filter { !it.text.isNullOrBlank() || !it.contentDescription.isNullOrBlank() }
        if (textNodes.isEmpty()) {
            triggerOcrFallback("No text nodes found in tree")
            return
        }

        processExtractedNodes(collected, textNodes)
    }

    private fun walkTree(
        node: AccessibilityNodeInfo,
        depth: Int,
        outList: MutableList<AccessibilityDumpNode>
    ) {
        if (depth > MAX_DEPTH || outList.size >= MAX_NODES) return

        val bounds = Rect().also { node.getBoundsInScreen(it) }
        val dumpNode = AccessibilityDumpNode(
            className = node.className?.toString() ?: "Unknown",
            viewId = node.viewIdResourceName,
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            bounds = "${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}",
            depth = depth
        )
        outList.add(dumpNode)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                walkTree(child, depth + 1, outList)
            } finally {
                child.recycle()
            }
            if (outList.size >= MAX_NODES) break
        }
    }

    private fun processExtractedNodes(
        allNodes: List<AccessibilityDumpNode>,
        textNodes: List<AccessibilityDumpNode>
    ) {
        val rawLines = textNodes.mapNotNull { it.text?.ifBlank { null } ?: it.contentDescription?.ifBlank { null } }
        val candidates = candidateExtractor.extractCandidates(rawLines, "Circle to Search")

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dumpDir = File(filesDir, "dumps").apply { mkdirs() }
        val dumpFile = File(dumpDir, "dump_$timestamp.txt")

        try {
            val fileContent = buildString {
                appendLine("=== SceneFinder Hierarchy Dump ($timestamp) ===")
                candidates.forEachIndexed { i, c ->
                    appendLine("#${i + 1} [${c.mediaType.displayName}] Clean=\"${c.cleanTitle}\" (Raw: \"${c.rawText}\")")
                }
            }
            dumpFile.writeText(fileContent)
        } catch (_: Exception) {}

        sessionManager.onCaptured(
            nodeCount = textNodes.size,
            filePath = dumpFile.absolutePath,
            preview = candidates.take(3).joinToString("\n") { "� ${it.cleanTitle}" },
            candidates = candidates
        )

        // Verify candidates against AniList, TMDB, MangaDex
        serviceScope.launch(Dispatchers.IO) {
            val apiKey = appSettings.tmdbApiKey.value
            for (candidate in candidates.take(3)) {
                val enriched = enrichmentRepository.enrichCandidate(candidate, apiKey)
                if (enriched != null) {
                    sessionManager.log("Verified Media Match: ${enriched.cleanTitle} [${enriched.mediaType}] (Saved to Library)")
                    if (appSettings.autoOverlay.value) {
                        launch(Dispatchers.Main) {
                            OverlayActivity.show(applicationContext, enriched)
                        }
                    }
                    break // Stop on highest confidence verified match
                }
            }
        }
    }

    private fun triggerOcrFallback(reason: String) {
        sessionManager.log("Triggering OCR fallback ($reason)")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                applicationContext.mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshotResult: ScreenshotResult) {
                        val hardwareBuffer = screenshotResult.hardwareBuffer
                        val colorSpace = screenshotResult.colorSpace
                        val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                        hardwareBuffer.close()

                        if (bitmap != null) {
                            val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                            serviceScope.launch(Dispatchers.Default) {
                                val ocrLines = ocrService.processImage(softwareBitmap)
                                val candidates = candidateExtractor.extractCandidates(ocrLines, "OCR Fallback")

                                for (candidate in candidates.take(3)) {
                                    val apiKey = appSettings.tmdbApiKey.value
                                    val enriched = enrichmentRepository.enrichCandidate(candidate, apiKey)
                                    if (enriched != null) {
                                        sessionManager.log("Verified OCR Match: ${enriched.cleanTitle} [${enriched.mediaType}]")
                                        if (appSettings.autoOverlay.value) {
                                            launch(Dispatchers.Main) {
                                                OverlayActivity.show(applicationContext, enriched)
                                            }
                                        }
                                        break
                                    }
                                }
                            }
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        sessionManager.log("OCR takeScreenshot failed with code $errorCode")
                    }
                }
            )
        }
    }

    fun triggerCircleToSearchGesture(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        // Bottom center where Android navigation pill / home bar sits
        val x = width / 2f
        val y = height - 25f

        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 650)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        sessionManager.log("Dispatching Circle to Search gesture at ($x, $y)")
        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                sessionManager.log("Circle to Search gesture dispatched successfully")
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                sessionManager.log("Circle to Search gesture cancelled")
            }
        }, null)
    }

    fun triggerDirectSceneRecognition() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(45)
        }

        Toast.makeText(
            applicationContext,
            "Scanning scene (Anime, Movie, Manga, Manhwa)...",
            Toast.LENGTH_SHORT
        ).show()
        sessionManager.log("Direct Scene Recognition initiated")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    applicationContext.mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(screenshotResult: ScreenshotResult) {
                            val hardwareBuffer = screenshotResult.hardwareBuffer
                            val colorSpace = screenshotResult.colorSpace
                            val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                            hardwareBuffer.close()

                            if (bitmap != null) {
                                val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                                serviceScope.launch(Dispatchers.Default) {
                                    val apiKey = appSettings.tmdbApiKey.value
                                    val enriched = enrichmentRepository.recognizeSceneFromBitmap(
                                        bitmap = softwareBitmap,
                                        ocrService = ocrService,
                                        candidateExtractor = candidateExtractor,
                                        tmdbApiKey = apiKey
                                    )

                                    launch(Dispatchers.Main) {
                                        if (enriched != null) {
                                            sessionManager.log("Direct Scene Recognized: ${enriched.cleanTitle} [${enriched.mediaType}]")
                                            Toast.makeText(
                                                applicationContext,
                                                "Identified: ${enriched.cleanTitle} (${enriched.mediaType})",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            OverlayActivity.show(applicationContext, enriched)
                                        } else {
                                            sessionManager.log("No direct scene match found")
                                            Toast.makeText(
                                                applicationContext,
                                                "No scene match found. Try Circle to Search!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            sessionManager.log("Direct scene screenshot failed: $errorCode")
                            Toast.makeText(
                                applicationContext,
                                "Screenshot capture error ($errorCode)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            } catch (e: Exception) {
                sessionManager.log("Error initiating screenshot: ${e.message}")
                Toast.makeText(
                    applicationContext,
                    "Screen capture permission needed. Try Circle to Search!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                applicationContext,
                "Screen capture requires Android 11+",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onInterrupt() {
        sessionManager.log("AccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
    }
}
