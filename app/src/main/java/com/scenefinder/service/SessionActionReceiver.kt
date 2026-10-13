package com.scenefinder.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.scenefinder.domain.session.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SessionActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CANCEL_SESSION) {
            sessionManager.cancel("Cancelled via notification")
        }
    }

    companion object {
        const val ACTION_CANCEL_SESSION = "com.scenefinder.ACTION_CANCEL_SESSION"
    }
}
