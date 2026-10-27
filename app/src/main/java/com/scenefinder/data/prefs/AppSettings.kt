package com.scenefinder.data.prefs

import android.content.Context
import com.scenefinder.service.SidePanelOverlayService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("scenefinder_prefs", Context.MODE_PRIVATE)

    private val _tmdbApiKey = MutableStateFlow(prefs.getString(KEY_TMDB_API_KEY, "") ?: "")
    val tmdbApiKey: StateFlow<String> = _tmdbApiKey.asStateFlow()

    private val _tmdbSessionId = MutableStateFlow(prefs.getString(KEY_TMDB_SESSION_ID, "") ?: "")
    val tmdbSessionId: StateFlow<String> = _tmdbSessionId.asStateFlow()

    private val _tmdbAccountId = MutableStateFlow(prefs.getInt(KEY_TMDB_ACCOUNT_ID, -1))
    val tmdbAccountId: StateFlow<Int> = _tmdbAccountId.asStateFlow()

    private val _anilistToken = MutableStateFlow(prefs.getString(KEY_ANILIST_TOKEN, "") ?: "")
    val anilistToken: StateFlow<String> = _anilistToken.asStateFlow()

    private val _anilistClientId = MutableStateFlow(prefs.getString(KEY_ANILIST_CLIENT_ID, "") ?: "")
    val anilistClientId: StateFlow<String> = _anilistClientId.asStateFlow()

    private val _anilistClientSecret = MutableStateFlow(prefs.getString(KEY_ANILIST_CLIENT_SECRET, "") ?: "")
    val anilistClientSecret: StateFlow<String> = _anilistClientSecret.asStateFlow()

    private val _anilistRedirectUri = MutableStateFlow(prefs.getString(KEY_ANILIST_REDIRECT_URI, "scenefinder://anilist-callback") ?: "scenefinder://anilist-callback")
    val anilistRedirectUri: StateFlow<String> = _anilistRedirectUri.asStateFlow()

    private val _autoOverlay = MutableStateFlow(prefs.getBoolean(KEY_AUTO_OVERLAY, true))
    val autoOverlay: StateFlow<Boolean> = _autoOverlay.asStateFlow()

    private val _sidePanelEnabled = MutableStateFlow(prefs.getBoolean(KEY_SIDE_PANEL, false))
    val sidePanelEnabled: StateFlow<Boolean> = _sidePanelEnabled.asStateFlow()

    private val _armTimeoutSeconds = MutableStateFlow(prefs.getInt(KEY_ARM_TIMEOUT, 45))
    val armTimeoutSeconds: StateFlow<Int> = _armTimeoutSeconds.asStateFlow()

    fun setTmdbApiKey(key: String) {
        prefs.edit().putString(KEY_TMDB_API_KEY, key.trim()).apply()
        _tmdbApiKey.value = key.trim()
    }

    fun setTmdbSessionId(sessionId: String) {
        prefs.edit().putString(KEY_TMDB_SESSION_ID, sessionId.trim()).apply()
        _tmdbSessionId.value = sessionId.trim()
    }

    fun setTmdbAccountId(accountId: Int) {
        prefs.edit().putInt(KEY_TMDB_ACCOUNT_ID, accountId).apply()
        _tmdbAccountId.value = accountId
    }

    fun setAnilistToken(token: String) {
        prefs.edit().putString(KEY_ANILIST_TOKEN, token.trim()).apply()
        _anilistToken.value = token.trim()
    }

    fun setAnilistClientId(id: String) {
        prefs.edit().putString(KEY_ANILIST_CLIENT_ID, id.trim()).apply()
        _anilistClientId.value = id.trim()
    }

    fun setAnilistClientSecret(secret: String) {
        prefs.edit().putString(KEY_ANILIST_CLIENT_SECRET, secret.trim()).apply()
        _anilistClientSecret.value = secret.trim()
    }

    fun setAnilistRedirectUri(uri: String) {
        prefs.edit().putString(KEY_ANILIST_REDIRECT_URI, uri.trim()).apply()
        _anilistRedirectUri.value = uri.trim()
    }

    fun setAutoOverlay(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_OVERLAY, enabled).apply()
        _autoOverlay.value = enabled
    }

    fun setSidePanelEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SIDE_PANEL, enabled).apply()
        _sidePanelEnabled.value = enabled
        if (enabled) {
            SidePanelOverlayService.start(context)
        } else {
            SidePanelOverlayService.stop(context)
        }
    }

    fun setArmTimeoutSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_ARM_TIMEOUT, seconds).apply()
        _armTimeoutSeconds.value = seconds
    }

    private val _isPageWiseLayout = MutableStateFlow(prefs.getBoolean(KEY_PAGE_WISE_LAYOUT, false))
    val isPageWiseLayout: StateFlow<Boolean> = _isPageWiseLayout.asStateFlow()

    fun setPageWiseLayout(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PAGE_WISE_LAYOUT, enabled).apply()
        _isPageWiseLayout.value = enabled
    }

    private val _themeMode = MutableStateFlow(
        runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name)
        }.getOrDefault(ThemeMode.DARK)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    private val _lastSeenWhatsNewVersion = MutableStateFlow(prefs.getString(KEY_LAST_SEEN_WHATS_NEW, "") ?: "")
    val lastSeenWhatsNewVersion: StateFlow<String> = _lastSeenWhatsNewVersion.asStateFlow()

    fun setLastSeenWhatsNewVersion(version: String) {
        prefs.edit().putString(KEY_LAST_SEEN_WHATS_NEW, version).apply()
        _lastSeenWhatsNewVersion.value = version
    }

    companion object {
        const val CURRENT_APP_VERSION = "2.4.2"
        private const val KEY_TMDB_API_KEY = "tmdb_api_key"
        private const val KEY_TMDB_SESSION_ID = "tmdb_session_id"
        private const val KEY_TMDB_ACCOUNT_ID = "tmdb_account_id"
        private const val KEY_ANILIST_TOKEN = "anilist_token"
        private const val KEY_ANILIST_CLIENT_ID = "anilist_client_id"
        private const val KEY_ANILIST_CLIENT_SECRET = "anilist_client_secret"
        private const val KEY_ANILIST_REDIRECT_URI = "anilist_redirect_uri"
        private const val KEY_AUTO_OVERLAY = "auto_overlay"
        private const val KEY_SIDE_PANEL = "side_panel_enabled"
        private const val KEY_ARM_TIMEOUT = "arm_timeout"
        private const val KEY_PAGE_WISE_LAYOUT = "page_wise_layout"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LAST_SEEN_WHATS_NEW = "last_seen_whats_new"
    }
}

enum class ThemeMode(val title: String, val icon: String) {
    SYSTEM("System Default", "📱"),
    DARK("Dark Mode", "🌙"),
    LIGHT("Light Mode", "☀️")
}

