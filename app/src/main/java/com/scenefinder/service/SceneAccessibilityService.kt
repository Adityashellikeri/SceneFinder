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
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.scenefinder.ui.overlay.TextSearchActivity
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

    private var selectionPillView: View? = null
    private var pillDismissJob: Job? = null
    private var clipManager: ClipboardManager? = null

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        try {
            val clip = clipManager?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0)?.text?.toString()?.trim()
                if (!text.isNullOrBlank() && text.length in 2..150) {
                    showSelectionPill(text, Rect())
                }
            }
        } catch (_: Exception) {}
    }

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
        configureServiceInfo(armedForCts = false)

        try {
            clipManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipManager?.addPrimaryClipChangedListener(clipListener)
        } catch (_: Exception) {}

        serviceScope.launch {
            sessionManager.state.collect { state ->
                when (state) {
                    is SessionState.Armed -> configureServiceInfo(armedForCts = true)
                    is SessionState.Disarmed,
                    is SessionState.Captured,
                    is SessionState.Cancelled,
                    is SessionState.TimedOut -> configureServiceInfo(armedForCts = false)
                }
            }
        }
    }

    private fun configureServiceInfo(armedForCts: Boolean) {
        val info = serviceInfo ?: AccessibilityServiceInfo()
        if (armedForCts) {
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
            info.packageNames = arrayOf(GOOGLE_SEARCHBOX_PACKAGE)
        } else {
            info.eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            info.packageNames = null // ALL apps including Chrome, Incognito, Circle to Search
        }
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        info.notificationTimeout = 50
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Handle text selection in any app (Chrome, Incognito, Circle to Search, etc.)
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            handleTextSelectionEvent(event)
            return
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            hideSelectionPill()
        }

        // STRICT GUARD: If not explicitly armed by our shortcut/side-panel, ignore everything!
        val currentState = sessionManager.state.value
        if (currentState !is SessionState.Armed) return

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

    private fun handleTextSelectionEvent(event: AccessibilityEvent) {
        val fromIndex = event.fromIndex
        val toIndex = event.toIndex

        if (fromIndex == toIndex || fromIndex < 0 || toIndex < 0) {
            hideSelectionPill()
            return
        }

        val textCandidate = extractSelectedText(event)
        if (textCandidate.isNullOrBlank() || textCandidate.length < 2) {
            hideSelectionPill()
            return
        }

        val rect = Rect()
        val node = event.source
        if (node != null) {
            try {
                node.getBoundsInScreen(rect)
            } catch (_: Exception) {}
            finally {
                try { node.recycle() } catch (_: Exception) {}
            }
        }

        showSelectionPill(textCandidate, rect)
    }

    private fun extractSelectedText(event: AccessibilityEvent): String? {
        val from = minOf(event.fromIndex, event.toIndex)
        val to = maxOf(event.fromIndex, event.toIndex)

        // Try source node text
        val source = event.source
        if (source != null) {
            val sourceText = source.text?.toString()
            if (!sourceText.isNullOrBlank()) {
                val safeFrom = from.coerceIn(0, sourceText.length)
                val safeTo = to.coerceIn(0, sourceText.length)
                if (safeTo > safeFrom) {
                    val substr = sourceText.substring(safeFrom, safeTo).trim()
                    if (substr.isNotBlank()) return substr
                }
            }
        }

        // Try event text list
        val listText = event.text?.joinToString(" ")?.trim()
        if (!listText.isNullOrBlank()) {
            val safeFrom = from.coerceIn(0, listText.length)
            val safeTo = to.coerceIn(0, listText.length)
            if (safeTo > safeFrom) {
                val substr = listText.substring(safeFrom, safeTo).trim()
                if (substr.isNotBlank()) return substr
            }
            return listText
        }

        val desc = event.contentDescription?.toString()?.trim()
        if (!desc.isNullOrBlank()) {
            return desc
        }

        return null
    }

    private fun showSelectionPill(selectedText: String, bounds: Rect) {
        hideSelectionPill()

        val displayMetrics = resources.displayMetrics
        val density = displayMetrics.density
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val pillLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val hPad = (13 * density).toInt()
            val vPad = (8 * density).toInt()
            setPadding(hPad, vPad, hPad, vPad)

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24 * density
                setColor(Color.parseColor("#1B1A30"))
                setStroke((1.5f * density).toInt(), Color.parseColor("#FF6B35"))
            }
            elevation = 16 * density

            val iconView = ImageView(context).apply {
                val iconSize = (18 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                    marginEnd = (7 * density).toInt()
                }
                setImageResource(android.R.drawable.ic_menu_search)
                setColorFilter(Color.parseColor("#FF6B35"))
            }
            addView(iconView)

            val textView = TextView(context).apply {
                text = "SceneFinder"
                setTextColor(Color.WHITE)
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                maxLines = 1
            }
            addView(textView)

            val arrowView = TextView(context).apply {
                text = " ›"
                setTextColor(Color.parseColor("#FF9800"))
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
            }
            addView(arrowView)

            isClickable = true
            isFocusable = false

            setOnClickListener {
                try {
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                } catch (_: Exception) {}

                hideSelectionPill()

                val intent = Intent(this@SceneAccessibilityService, TextSearchActivity::class.java).apply {
                    action = Intent.ACTION_PROCESS_TEXT
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(Intent.EXTRA_PROCESS_TEXT, selectedText)
                }
                startActivity(intent)
            }
        }

        val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return

        val pillHeight = (42 * density).toInt()
        val pillWidth = (145 * density).toInt()

        val targetX = if (bounds.width() > 0) {
            bounds.left.coerceIn((16 * density).toInt(), screenWidth - pillWidth - (16 * density).toInt())
        } else {
            (screenWidth - pillWidth) / 2
        }

        val targetY = if (bounds.height() > 0) {
            if (bounds.top > (pillHeight + 90 * density)) {
                // Place above the selection / standard popup
                (bounds.top - pillHeight - 60 * density).toInt().coerceAtLeast((25 * density).toInt())
            } else {
                // Place below the selection
                (bounds.bottom + 16 * density).toInt().coerceAtMost(screenHeight - pillHeight - (40 * density).toInt())
            }
        } else {
            (screenHeight * 0.22).toInt()
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = targetX
            y = targetY
        }

        try {
            wm.addView(pillLayout, params)
            selectionPillView = pillLayout

            pillDismissJob?.cancel()
            pillDismissJob = serviceScope.launch {
                delay(7000L)
                hideSelectionPill()
            }
        } catch (e: Exception) {
            android.util.Log.e("SceneAccessibility", "Failed to add selection pill", e)
        }
    }

    private fun hideSelectionPill() {
        pillDismissJob?.cancel()
        pillDismissJob = null
        val view = selectionPillView ?: return
        selectionPillView = null
        try {
            val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            wm?.removeView(view)
        } catch (_: Exception) {}
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
                            val softwareBitmap = try {
                                bitmap.copy(Bitmap.Config.ARGB_8888, false)
                            } finally {
                                try { bitmap.recycle() } catch (_: Exception) {}
                            }
                            if (softwareBitmap != null) {
                                serviceScope.launch(Dispatchers.Default) {
                                    try {
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
                                    } finally {
                                        try { softwareBitmap.recycle() } catch (_: Exception) {}
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
                                val softwareBitmap = try {
                                    bitmap.copy(Bitmap.Config.ARGB_8888, false)
                                } finally {
                                    try { bitmap.recycle() } catch (_: Exception) {}
                                }
                                if (softwareBitmap != null) {
                                    serviceScope.launch(Dispatchers.Default) {
                                        try {
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
                                        } finally {
                                            try { softwareBitmap.recycle() } catch (_: Exception) {}
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
        hideSelectionPill()
        try {
            clipManager?.removePrimaryClipChangedListener(clipListener)
        } catch (_: Exception) {}
        serviceScope.cancel()
    }
}
