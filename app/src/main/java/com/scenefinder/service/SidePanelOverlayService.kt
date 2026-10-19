package com.scenefinder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.scenefinder.domain.session.SessionManager
import com.scenefinder.domain.session.SessionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class SidePanelOverlayService : Service() {

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var launcher: CircleToSearchLauncher

    private var windowManager: WindowManager? = null
    private var edgeView: View? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val ACTION_START = "com.scenefinder.SIDE_PANEL_START"
        const val ACTION_STOP = "com.scenefinder.SIDE_PANEL_STOP"

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, SidePanelOverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SidePanelOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> showEdgeHandle()
            ACTION_STOP -> {
                removeEdgeHandle()
                stopSelf()
            }
            else -> showEdgeHandle()
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val channelId = "side_panel_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Side Panel Trigger Service",
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("SceneFinder Active")
            .setContentText("Tap floating button to trigger Circle to Search")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(4001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(4001, notification)
        }
    }

    private fun showEdgeHandle() {
        if (!Settings.canDrawOverlays(this)) return
        if (edgeView != null) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 0
            y = 400
        }

        // Single Circle-to-Search button pill
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(10, 18, 10, 18)
            elevation = 20f
        }

        val ctsBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            setColorFilter(Color.parseColor("#F58529"))
            contentDescription = "Trigger Circle to Search"
            setPadding(12, 12, 12, 12)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#33F58529"))
            }
        }

        container.addView(ctsBtn)

        serviceScope.launch {
            sessionManager.state.collect { state ->
                val isArmed = state is SessionState.Armed
                val borderColor = if (isArmed) Color.parseColor("#0DC143") else Color.parseColor("#F58529")
                val ctsIconColor = if (isArmed) Color.parseColor("#0DC143") else Color.parseColor("#F58529")

                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadii = floatArrayOf(32f, 32f, 0f, 0f, 0f, 0f, 32f, 32f)
                    setColor(Color.parseColor("#F0000000"))
                    setStroke(2, borderColor)
                }
                container.background = bg
                ctsBtn.setColorFilter(ctsIconColor)
            }
        }

        var initialY = 0
        var initialTouchY = 0f
        var isDragging = false

        container.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = params.y
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    if (abs(deltaY) > 12) {
                        isDragging = true
                        params.y = initialY + deltaY
                        windowManager?.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) onCtsTapped()
                    true
                }
                else -> false
            }
        }

        edgeView = container
        try {
            windowManager?.addView(edgeView, params)
        } catch (_: Exception) {}
    }

    private fun onCtsTapped() {
        triggerHaptic()

        sessionManager.arm(45)
        sessionManager.log("Side Panel CTS clicked: Session Armed (45s)")

        Toast.makeText(
            applicationContext,
            "Circle to Search Triggered: Circle what you want to find",
            Toast.LENGTH_SHORT
        ).show()

        val res = launcher.triggerSearch(applicationContext)
        sessionManager.log("Circle to Search launch attempt: $res")
    }

    private fun triggerHaptic() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(45)
        }
    }

    private fun removeEdgeHandle() {
        edgeView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        edgeView = null
    }

    override fun onDestroy() {
        removeEdgeHandle()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
