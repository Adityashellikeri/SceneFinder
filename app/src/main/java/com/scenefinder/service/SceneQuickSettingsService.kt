package com.scenefinder.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.scenefinder.domain.session.SessionManager
import com.scenefinder.domain.session.SessionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SceneQuickSettingsService : TileService() {

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var launcher: CircleToSearchLauncher

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTile(sessionManager.state.value)
        scope.launch {
            sessionManager.state.collect { state ->
                updateTile(state)
            }
        }
    }

    private fun updateTile(state: SessionState) {
        val tile = qsTile ?: return
        when (state) {
            is SessionState.Armed -> {
                tile.state = Tile.STATE_ACTIVE
                tile.subtitle = "Listening... (${state.remainingSeconds}s)"
            }
            else -> {
                tile.state = Tile.STATE_INACTIVE
                tile.subtitle = "Tap to Arm"
            }
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val current = sessionManager.state.value
        if (current is SessionState.Armed) {
            sessionManager.cancel("Tile toggle")
        } else {
            sessionManager.arm(45)
            // Attempt to trigger Assist on arming
            launcher.tryActionAssist(applicationContext)
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        scope.cancel()
    }
}
