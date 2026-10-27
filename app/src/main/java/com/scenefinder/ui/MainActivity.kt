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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Surface
import com.scenefinder.ui.theme.IGDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import com.scenefinder.ui.home.HomeScreen
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
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
import com.scenefinder.ui.components.WhatsNewDialog
import com.scenefinder.ui.components.InAppUpdateBanner
import com.scenefinder.ui.components.InAppUpdateMessage
import com.scenefinder.ui.theme.IGBlack
import com.scenefinder.ui.theme.IGBlue
import com.scenefinder.ui.theme.IGSurface
import com.scenefinder.ui.theme.IGTextPrimary
import com.scenefinder.ui.theme.IGTextSecondary
import com.scenefinder.ui.theme.SceneFinderTheme
import com.scenefinder.ui.theme.LocalAppColors
import com.scenefinder.data.prefs.ThemeMode
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
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

    private var lastAutoSyncTimestamp = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        applyNativeMaxRefreshRate()
        handleArmIntent(intent)
        handleOAuthCallback(intent)

        try {
            if (PermissionHelper.canDrawOverlays(this) && settings.sidePanelEnabled.value) {
                SidePanelOverlayService.start(this)
            } else {
                SidePanelOverlayService.stop(this)
            }
        } catch (_: Throwable) {}

        setContent {
            val themeMode by settings.themeMode.collectAsState()
            SceneFinderTheme(themeMode = themeMode) {
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
        try {
            if (PermissionHelper.canDrawOverlays(this) && settings.sidePanelEnabled.value) {
                SidePanelOverlayService.start(this)
            } else {
                SidePanelOverlayService.stop(this)
            }
        } catch (_: Throwable) {}

        // Check if TMDB approval was pending
        val pendingTmdb = settings.tmdbSessionId.value
        val key = settings.tmdbApiKey.value.ifBlank { MediaEnrichmentRepository.DEFAULT_TMDB_API_KEY }
        if (pendingTmdb.startsWith("__pending__:")) {
            val token = pendingTmdb.removePrefix("__pending__:")
            lifecycleScope.launch {
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

        // Silent background sync of libraries with cooldown so startup is 100% lag-free
        val now = System.currentTimeMillis()
        if (now - lastAutoSyncTimestamp > 5 * 60 * 1000L) {
            lastAutoSyncTimestamp = now
            val anilistTok = settings.anilistToken.value.trim().removePrefix("Bearer ").trim()
            val tmdbSess = settings.tmdbSessionId.value
            val tmdbAcc = settings.tmdbAccountId.value
            lifecycleScope.launch(Dispatchers.IO) {
                kotlinx.coroutines.delay(2000L)
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
                lifecycleScope.launch {
                    val count = withContext(Dispatchers.IO) { repository.syncAniListLibrary(directToken) }
                    Toast.makeText(this@MainActivity, "AniList synced ($count items)", Toast.LENGTH_SHORT).show()
                }
                return
            }

            if (!code.isNullOrBlank()) {
                android.util.Log.d("AniListOAuth", "Received code=$code")
                lifecycleScope.launch {
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
                lifecycleScope.launch {
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

    /**
     * Request a smooth refresh rate without *locking* to max.
     *
     * Locking preferredDisplayModeId = highestMode.modeId forces the display to run at maximum
     * refresh rate (e.g. 120 Hz / 144 Hz) 100 % of the time, even when the UI is completely
     * static. On modern LTPO panels this kills battery by preventing the hardware from throttling
     * down to 1–30 Hz between frames.
     *
     * Using preferredRefreshRate lets the OS negotiate the best rate: fast during scrolling /
     * animation, slow during idle — giving us smooth animations *and* good battery life.
     */
    private fun applyNativeMaxRefreshRate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = this.display
                val modes = display?.supportedModes ?: emptyArray()
                val highestRate = modes.maxByOrNull { it.refreshRate }?.refreshRate ?: return
                val lp = window.attributes
                // Hint, not a lock — the compositor can honour or ignore it per vsync budget
                lp.preferredRefreshRate = highestRate
                window.attributes = lp
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val modes = windowManager.defaultDisplay?.supportedModes ?: emptyArray()
                val highestRate = modes.maxByOrNull { it.refreshRate }?.refreshRate ?: return
                val lp = window.attributes
                lp.preferredRefreshRate = highestRate
                window.attributes = lp
            }
        } catch (e: Throwable) {
            android.util.Log.w("RefreshRate", "Failed to set preferred refresh rate: ${e.message}")
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

    val lastSeenWhatsNew by settings.lastSeenWhatsNewVersion.collectAsState()
    var showWhatsNewDialog by remember { mutableStateOf(false) }
    var updateNotification by remember { mutableStateOf<InAppUpdateMessage?>(null) }

    LaunchedEffect(lastSeenWhatsNew) {
        if (lastSeenWhatsNew != AppSettings.CURRENT_APP_VERSION) {
            showWhatsNewDialog = true
        }
    }

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
        NavItem("Home",     Icons.Outlined.Home,          Icons.Filled.Home),
        NavItem("Library",  Icons.Outlined.VideoLibrary,  Icons.Filled.VideoLibrary),
        NavItem("Search",   Icons.Outlined.Search,        Icons.Filled.Search)
    )

    val haptic = LocalHapticFeedback.current
    val colors = LocalAppColors.current

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (selectedIndex in 0..2) {
                // ── Cyberpunk Neon Nav Bar ──
                val cyberNavBg = if (colors.isDark) Color(0xF0060912) else Color(0xF8FFFFFF)
                val cyberBorderBrush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF00F5FF).copy(alpha = 0.70f),
                        Color(0xFFBF00FF).copy(alpha = 0.40f),
                        Color(0xFFFF00C8).copy(alpha = 0.60f),
                        Color(0xFF00F5FF).copy(alpha = 0.55f)
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = cyberNavBg,
                        border = BorderStroke(1.dp, cyberBorderBrush),
                        shadowElevation = 32.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            navItems.forEachIndexed { index, item ->
                                val selected = selectedIndex == index
                                val tabAccent = when (index) {
                                    0 -> Color(0xFF00F5FF)   // Home = cyan
                                    1 -> Color(0xFFBF00FF)   // Library = violet
                                    else -> Color(0xFFFF00C8) // Search = magenta
                                }
                                val iconScale by animateFloatAsState(
                                    targetValue = if (selected) 1.18f else 1.0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "tab_scale_$index"
                                )
                                val bgAlpha by animateFloatAsState(
                                    targetValue = if (selected) 1f else 0f,
                                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                                    label = "tab_bg_alpha_$index"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(20.dp))
                                        .then(
                                            if (selected) {
                                                Modifier
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(
                                                                tabAccent.copy(alpha = 0.22f * bgAlpha),
                                                                tabAccent.copy(alpha = 0.08f * bgAlpha)
                                                            )
                                                        )
                                                    )
                                                    .border(
                                                        1.dp,
                                                        Brush.horizontalGradient(
                                                            listOf(
                                                                tabAccent.copy(alpha = 0.85f),
                                                                tabAccent.copy(alpha = 0.30f)
                                                            )
                                                        ),
                                                        RoundedCornerShape(20.dp)
                                                    )
                                            } else Modifier
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            navigateToTab(index)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.icon,
                                            contentDescription = item.label,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .graphicsLayer {
                                                    scaleX = iconScale
                                                    scaleY = iconScale
                                                },
                                            tint = if (selected) tabAccent
                                                   else if (colors.isDark) Color(0xFF4E5580) else Color(0xFF8B92B8)
                                        )
                                        AnimatedVisibility(
                                            visible = selected,
                                            enter = fadeIn(tween(120)),
                                            exit  = fadeOut(tween(60))
                                        ) {
                                            Text(
                                                text          = item.label.uppercase(),
                                                fontSize      = 8.sp,
                                                fontWeight    = FontWeight.ExtraBold,
                                                letterSpacing = 1.5.sp,
                                                maxLines      = 1,
                                                softWrap      = false,
                                                color         = tabAccent
                                            )
                                        }
                                    }
                                }
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
                .background(colors.background)
        ) {
            when (selectedIndex) {
                0 -> HomeScreen(
                    dao = dao,
                    repository = repository,
                    settings = settings,
                    onNavigateToSearch = { navigateToTab(2) },
                    onNavigateToSettings = { navigateToTab(4) },
                    onShowWhatsNew = { showWhatsNewDialog = true },
                    onNotifyUpdate = { msg -> updateNotification = msg }
                )
                1 -> HistoryScreen(
                    dao = dao,
                    repository = repository,
                    settings = settings,
                    onNavigateToDiscover = { navigateToTab(2) },
                    onNavigateToSettings = { navigateToTab(4) },
                    onNotifyUpdate = { msg -> updateNotification = msg }
                )
                2 -> HistoryScreen(
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
                    },
                    onNavigateToDiscover = { navigateToTab(2) },
                    onNavigateToSettings = { navigateToTab(4) },
                    onNotifyUpdate = { msg -> updateNotification = msg }
                )
                3 -> OnboardingScreen(onContinueToDashboard = {
                    if (tabBackStack.size > 1) {
                        tabBackStack.removeAt(tabBackStack.lastIndex)
                    } else {
                        navigateToTab(0)
                    }
                })
                4 -> SettingsScreen(
                    settings = settings,
                    repository = repository,
                    dao = dao,
                    onBack = {
                        if (tabBackStack.size > 1) {
                            tabBackStack.removeAt(tabBackStack.lastIndex)
                        } else {
                            navigateToTab(0)
                        }
                    },
                    onShowWhatsNew = { showWhatsNewDialog = true },
                    onNotifyUpdate = { msg -> updateNotification = msg }
                )
            }

            // Floating In-App Update Notification Banner
            InAppUpdateBanner(
                message = updateNotification,
                onDismiss = { updateNotification = null },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // What's New Release Tour Dialog
            if (showWhatsNewDialog) {
                WhatsNewDialog(
                    version = AppSettings.CURRENT_APP_VERSION,
                    onDismiss = {
                        settings.setLastSeenWhatsNewVersion(AppSettings.CURRENT_APP_VERSION)
                        showWhatsNewDialog = false
                    }
                )
            }
        }
    }
}
