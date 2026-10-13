package com.scenefinder.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LaunchResult {
    data class Success(val method: String, val details: String) : LaunchResult
    data class Failed(val method: String, val reason: String) : LaunchResult
}

@Singleton
class CircleToSearchLauncher @Inject constructor() {

    fun triggerSearch(context: Context): LaunchResult {
        // 1. Primary: Hardware long-press gesture on bottom navigation bar via Accessibility Service
        val service = SceneAccessibilityService.instance
        if (service != null) {
            val gestureDispatched = service.triggerCircleToSearchGesture()
            if (gestureDispatched) {
                return LaunchResult.Success("Accessibility Gesture", "Triggered Circle to Search bottom long-press gesture")
            }
        }

        // 2. Try targeting Google QuickSearchBox ACTION_ASSIST with CTS flags
        try {
            val googleAssistIntent = Intent(Intent.ACTION_ASSIST).apply {
                setPackage("com.google.android.googlequicksearchbox")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("source", "circle_to_search")
                putExtra("android.intent.extra.ASSIST_INPUT_HINT", 1)
                putExtra("com.google.android.apps.search.assistant.invocation_type", "G_BUTTON")
            }
            if (googleAssistIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(googleAssistIntent)
                return LaunchResult.Success("Google ACTION_ASSIST", "Triggered Google Assist")
            }
        } catch (_: Exception) {}

        // 3. Try generic ACTION_ASSIST
        try {
            val assistIntent = Intent(Intent.ACTION_ASSIST).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (assistIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(assistIntent)
                return LaunchResult.Success("Generic ACTION_ASSIST", "Triggered generic assist")
            }
        } catch (_: Exception) {}

        // 4. Try Accessibility Service Global Action fallback
        if (service != null) {
            val success = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_SHORTCUT)
            if (success) {
                return LaunchResult.Success("Accessibility GLOBAL_ACTION_ACCESSIBILITY_SHORTCUT", "Triggered via accessibility")
            }
        }

        return LaunchResult.Failed("All Launchers", "Could not trigger Circle to Search automatically")
    }

    fun tryActionAssist(context: Context): LaunchResult = triggerSearch(context)

    fun tryVoiceAssist(context: Context): LaunchResult {
        return try {
            val intent = Intent("android.intent.action.VOICE_ASSIST").apply {
                setPackage("com.google.android.googlequicksearchbox")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            LaunchResult.Success("VOICE_ASSIST", "Launched voice assist")
        } catch (e: Exception) {
            LaunchResult.Failed("VOICE_ASSIST", e.message ?: e.toString())
        }
    }

    fun tryGlobalAction(service: AccessibilityService?, actionId: Int, actionName: String): LaunchResult {
        if (service == null) {
            return LaunchResult.Failed(actionName, "Accessibility service is not running")
        }
        val success = service.performGlobalAction(actionId)
        return if (success) {
            LaunchResult.Success(actionName, "performGlobalAction succeeded")
        } else {
            LaunchResult.Failed(actionName, "performGlobalAction returned false")
        }
    }
}
