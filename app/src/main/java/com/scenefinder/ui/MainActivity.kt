package com.scenefinder.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import com.scenefinder.ui.theme.IGDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scenefinder.data.local.dao.EnrichedMediaDao
import com.scenefinder.data.prefs.AppSettings
import com.scenefinder.data.repository.MediaEnrichmentRepository
import com.scenefinder.domain.session.SessionManager
import com.scenefinder.service.CircleToSearchLauncher
import com.scenefinder.service.SidePanelOverlayService
import com.scenefinder.ui.history.HistoryScreen
import com.scenefinder.ui.onboarding.OnboardingScreen
import com.scenefinder.ui.onboarding.PermissionHelper
import com.scenefinder.ui.settings.SettingsScreen
import com.scenefinder.ui.theme.IGBlack
import com.scenefinder.ui.theme.IGBlue
import com.scenefinder.ui.theme.IGSurface
import com.scenefinder.ui.theme.IGTextPrimary
import com.scenefinder.ui.theme.IGTextSecondary
import com.scenefinder.ui.theme.SceneFinderTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var launcher: CircleToSearchLauncher

    @Inject
    lateinit var dao: EnrichedMediaDao

    @Inject
    lateinit var repository: MediaEnrichmentRepository

    @Inject
    lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        applyNativeMaxRefreshRate()
        handleArmIntent(intent)
        handleOAuthCallback(intent)

        if (PermissionHelper.canDrawOverlays(this) && settings.sidePanelEnabled.value) {
            SidePanelOverlayService.start(this)
        }

        setContent {
            SceneFinderTheme {
                MainAppScaffold(
                    sessionManager = sessionManager,
                    launcher = launcher,
                    dao = dao,
                    repository = repository,
                    settings = settings
                )
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyNativeMaxRefreshRate()
    }

    override fun onResume() {
        super.onResume()
        applyNativeMaxRefreshRate()
        if (PermissionHelper.canDrawOverlays(this) && settings.sidePanelEnabled.value) {
            SidePanelOverlayService.start(this)
        }

        // Check if TMDB approval was pending
        val pendingTmdb = settings.tmdbSessionId.value
        val key = settings.tmdbApiKey.value.ifBlank { MediaEnrichmentRepository.DEFAULT_TMDB_API_KEY }
        if (pendingTmdb.startsWith("__pending__:")) {
            val token = pendingTmdb.removePrefix("__pending__:")
            kotlinx.coroutines.MainScope().launch {
                val res = withContext(Dispatchers.IO) { repository.createTmdbSession(key, token) }
                if (res != null) {
                    settings.setTmdbSessionId(res.first)
                    settings.setTmdbAccountId(res.second)
                    Toast.makeText(this@MainActivity, "TMDB connected!", Toast.LENGTH_SHORT).show()
                    val count = withContext(Dispatchers.IO) { repository.syncTmdbLibrary(key, res.first, res.second) }
                    Toast.makeText(this@MainActivity, "TMDB synced ($count items)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Silent background sync of libraries so website changes reflect in the app
        val anilistTok = settings.anilistToken.value.trim().removePrefix("Bearer ").trim()
        val tmdbSess = settings.tmdbSessionId.value
        val tmdbAcc = settings.tmdbAccountId.value
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob()).launch {
            if (anilistTok.isNotBlank()) {
                try {
                    repository.syncAniListLibrary(anilistTok)
                } catch (e: Exception) {
                    android.util.Log.e("AutoSync", "AniList auto-sync error", e)
                }
            }
            if (tmdbSess.isNotBlank() && !tmdbSess.startsWith("__pending__:") && tmdbAcc > 0) {
                try {
                    repository.syncTmdbLibrary(key, tmdbSess, tmdbAcc)
                } catch (e: Exception) {
                    android.util.Log.e("AutoSync", "TMDB auto-sync error", e)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleArmIntent(intent)
        handleOAuthCallback(intent)
    }

    private fun handleArmIntent(intent: Intent?) {
        if (intent?.action == ACTION_ARM) {
            val timeout = settings.armTimeoutSeconds.value
            sessionManager.arm(timeout)
            launcher.tryActionAssist(this)
            Toast.makeText(this, "SceneFinder Armed (${timeout}s)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleOAuthCallback(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "scenefinder" && data.host == "anilist-callback") {
            val code = data.getQueryParameter("code")
            val fragment = data.fragment
            val directToken = if (!fragment.isNullOrBlank()) {
                fragment.split("&").mapNotNull {
                    val parts = it.split("=")
                    if (parts.size == 2 && parts[0] == "access_token") parts[1] else null
                }.firstOrNull()
            } else null

            if (!directToken.isNullOrBlank()) {
                settings.setAnilistToken(directToken)
                Toast.makeText(this@MainActivity, "AniList connected!", Toast.LENGTH_SHORT).show()
                kotlinx.coroutines.MainScope().launch {
                    val count = withContext(Dispatchers.IO) { repository.syncAniListLibrary(directToken) }
                    Toast.makeText(this@MainActivity, "AniList synced ($count items)", Toast.LENGTH_SHORT).show()
                }
                return
            }

            if (!code.isNullOrBlank()) {
                android.util.Log.d("AniListOAuth", "Received code=$code")
                kotlinx.coroutines.MainScope().launch {
                    val token = withContext(Dispatchers.IO) {
                        repository.exchangeAniListCode(
                            code = code,
                            clientId = settings.anilistClientId.value,
                            clientSecret = settings.anilistClientSecret.value,
                            redirectUri = settings.anilistRedirectUri.value
                        )
                    }
                    if (token != null) {
                        settings.setAnilistToken(token)
                        Toast.makeText(this@MainActivity, "AniList connected!", Toast.LENGTH_SHORT).show()
                        val count = withContext(Dispatchers.IO) { repository.syncAniListLibrary(token) }
                        Toast.makeText(this@MainActivity, "AniList synced ($count items)", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "AniList login failed. Check your client secret and redirect URI.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else if (data.scheme == "scenefinder" && data.host == "tmdb-callback") {
            val pending = settings.tmdbSessionId.value
            val token = if (pending.startsWith("__pending__:")) pending.removePrefix("__pending__:") else data.getQueryParameter("request_token").orEmpty()
            val key = settings.tmdbApiKey.value.ifBlank { MediaEnrichmentRepository.DEFAULT_TMDB_API_KEY }
            if (token.isNotBlank()) {
                kotlinx.coroutines.MainScope().launch {
                    val res = withContext(Dispatchers.IO) { repository.createTmdbSession(key, token) }
                    if (res != null) {
                        settings.setTmdbSessionId(res.first)
                        settings.setTmdbAccountId(res.second)
                        Toast.makeText(this@MainActivity, "TMDB connected!", Toast.LENGTH_SHORT).show()
                        val count = withContext(Dispatchers.IO) { repository.syncTmdbLibrary(key, res.first, res.second) }
                        Toast.makeText(this@MainActivity, "TMDB synced ($count items)", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "TMDB login failed. Please approve in browser.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun applyNativeMaxRefreshRate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = this.display
                val modes = display?.supportedModes ?: emptyArray()
                val highestMode = modes.maxByOrNull { it.refreshRate }
                if (highestMode != null) {
                    val lp = window.attributes
                    lp.preferredDisplayModeId = highestMode.modeId
                    window.attributes = lp
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val modes = windowManager.defaultDisplay?.supportedModes ?: emptyArray()
                val highestMode = modes.maxByOrNull { it.refreshRate }
                if (highestMode != null) {
                    val lp = window.attributes
                    lp.preferredDisplayModeId = highestMode.modeId
                    window.attributes = lp
                }
            }
        } catch (e: Throwable) {
            android.util.Log.w("RefreshRate", "Failed to set display refresh rate: ${e.message}")
        }
    }

    companion object {
        const val ACTION_ARM = "com.scenefinder.ACTION_ARM"
    }
}

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

@Composable
private fun MainAppScaffold(
    sessionManager: SessionManager,
    launcher: CircleToSearchLauncher,
    dao: EnrichedMediaDao,
    repository: MediaEnrichmentRepository,
    settings: AppSettings
) {
    val tabBackStack = remember { mutableStateListOf(0) }
    val selectedIndex = tabBackStack.lastOrNull() ?: 0

    // Handle back button: if not on Home (tab 0), pop back to previous tab
    BackHandler(enabled = tabBackStack.size > 1) {
        tabBackStack.removeAt(tabBackStack.lastIndex)
    }

    fun navigateToTab(index: Int) {
        if (selectedIndex != index) {
            tabBackStack.removeAll { it == index }
            tabBackStack.add(index)
        }
    }

    val navItems = listOf(
        NavItem("Library",  Icons.Outlined.Home,    Icons.Filled.Home),
        NavItem("Search",   Icons.Outlined.Search,  Icons.Filled.Search),
        NavItem("Discover", Icons.Outlined.Explore, Icons.Filled.Explore),
        NavItem("Settings", Icons.Outlined.Settings,Icons.Filled.Settings)
    )

    Scaffold(
        containerColor = IGBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .navigationBarsPadding()
            ) {
                // Subtle 0.5dp top border
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(IGDivider)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navItems.forEachIndexed { index, item ->
                        val selected = selectedIndex == index
                        val iconScale by animateFloatAsState(
                            targetValue = if (selected) 1.15f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "tab_scale"
                        )
                        val iconColor by animateColorAsState(
                            targetValue = if (selected) IGTextPrimary else IGTextSecondary,
                            animationSpec = tween(150),
                            label = "tab_color"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { navigateToTab(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .scale(iconScale),
                                    tint = iconColor
                                )
                                Spacer(Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) IGTextPrimary else Color.Transparent)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .background(IGBlack)
        ) {
            when (selectedIndex) {
                0 -> HistoryScreen(
                    dao = dao,
                    repository = repository,
                    settings = settings
                )
                1 -> HistoryScreen(
                    dao = dao,
                    repository = repository,
                    settings = settings,
                    startInSearch = true,
                    onBackToHome = {
                        if (tabBackStack.size > 1) {
                            tabBackStack.removeAt(tabBackStack.lastIndex)
                        } else {
                            navigateToTab(0)
                        }
                    }
                )
                2 -> OnboardingScreen(onContinueToDashboard = { navigateToTab(0) })
                3 -> SettingsScreen(settings = settings, repository = repository)
            }
        }
    }
}
