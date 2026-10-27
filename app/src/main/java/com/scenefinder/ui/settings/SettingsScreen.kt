package com.scenefinder.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.AutoAwesome
import com.scenefinder.data.local.dao.EnrichedMediaDao
import com.scenefinder.ui.components.InAppUpdateMessage
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scenefinder.data.prefs.AppSettings
import com.scenefinder.data.repository.MediaEnrichmentRepository
import com.scenefinder.ui.theme.IGBlack
import com.scenefinder.ui.theme.IGBlue
import com.scenefinder.ui.theme.IGCard
import com.scenefinder.ui.theme.IGDivider
import com.scenefinder.ui.theme.IGGradEnd
import com.scenefinder.ui.theme.IGGradMid
import com.scenefinder.ui.theme.IGGradStart
import com.scenefinder.ui.theme.IGGreen
import com.scenefinder.ui.theme.IGRed
import com.scenefinder.ui.theme.IGSurface
import com.scenefinder.ui.theme.IGTextPrimary
import com.scenefinder.ui.theme.IGTextSecondary
import com.scenefinder.ui.theme.IGTextTertiary
import com.scenefinder.ui.theme.LocalAppColors
import com.scenefinder.data.prefs.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val headerGradient = Brush.horizontalGradient(listOf(IGGradStart, IGGradMid, IGGradEnd))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    repository: MediaEnrichmentRepository,
    modifier: Modifier = Modifier,
    dao: EnrichedMediaDao? = null,
    onShowWhatsNew: (() -> Unit)? = null,
    onNotifyUpdate: ((InAppUpdateMessage) -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentApiKey by settings.tmdbApiKey.collectAsState()
    val autoOverlay by settings.autoOverlay.collectAsState()
    val armTimeout by settings.armTimeoutSeconds.collectAsState()

    val currentClientId by settings.anilistClientId.collectAsState()
    val currentClientSecret by settings.anilistClientSecret.collectAsState()
    val currentRedirectUri by settings.anilistRedirectUri.collectAsState()
    val currentToken by settings.anilistToken.collectAsState()
    val tmdbSessionId by settings.tmdbSessionId.collectAsState()
    val tmdbAccountId by settings.tmdbAccountId.collectAsState()

    val userLibrary by dao?.getAllHistory()?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }

    var apiKeyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var clientIdInput by remember(currentClientId) { mutableStateOf(currentClientId) }
    var clientSecretInput by remember(currentClientSecret) { mutableStateOf(currentClientSecret) }
    var redirectUriInput by remember(currentRedirectUri) { mutableStateOf(currentRedirectUri) }

    var syncStatus by remember { mutableStateOf("") }
    var isSyncing by remember { mutableStateOf(false) }
    var tmdbLoginPending by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showHowItWorks by remember { mutableStateOf(false) }

    val isAnilistConnected = currentToken.isNotBlank()
    val isTmdbConnected = tmdbSessionId.isNotBlank() && tmdbAccountId >= 0

    LazyColumn(
        modifier = modifier.fillMaxSize().background(IGBlack),
        contentPadding = PaddingValues(bottom = 64.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IGBlack)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = IGTextPrimary
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = "Settings",
                        style = androidx.compose.ui.text.TextStyle(
                            brush = headerGradient,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Configure your Ark accounts and preferences",
                    color = IGTextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(IGDivider))
            }
        }

        // ── Sync Library Card ──────────────────────────────────────────────────
        item {
            IGSettingsCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IGSettingsSectionHeader(
                        icon = Icons.Default.Sync,
                        title = "Sync Libraries",
                        subtitle = "Import your watch lists from AniList & TMDB"
                    )
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = IGGradMid,
                            strokeWidth = 2.dp
                        )
                    }
                }

                if (syncStatus.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = syncStatus,
                        fontSize = 12.sp,
                        color = if (syncStatus.startsWith("❌")) IGRed else IGGreen,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Sync AniList button
                Button(
                    onClick = {
                        if (currentToken.isBlank()) {
                            syncStatus = "⚠️ Connect AniList first (below)"
                            return@Button
                        }
                        scope.launch {
                            isSyncing = true
                            syncStatus = "🔄 Syncing AniList library…"
                            val count = withContext(Dispatchers.IO) {
                                repository.syncAniListLibrary(currentToken)
                            }
                            syncStatus = if (count > 0)
                                "✅ AniList: synced $count items"
                            else
                                "✅ AniList: synced (library empty)"
                            isSyncing = false
                            onNotifyUpdate?.invoke(
                                InAppUpdateMessage(
                                    title = "AniList Synced",
                                    description = if (count > 0) "Synced $count items to library" else "Library is up to date",
                                    accentColor = IGGradMid
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAnilistConnected) IGGradMid.copy(alpha = 0.15f) else IGCard
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = if (isAnilistConnected) IGGradMid else IGTextSecondary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Sync AniList Library",
                        color = if (isAnilistConnected) IGGradMid else IGTextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Sync TMDB button
                Button(
                    onClick = {
                        if (!isTmdbConnected) {
                            syncStatus = "⚠️ Connect TMDB first (below)"
                            return@Button
                        }
                        scope.launch {
                            isSyncing = true
                            syncStatus = "🔄 Syncing TMDB watchlist…"
                            val count = withContext(Dispatchers.IO) {
                                repository.syncTmdbLibrary(currentApiKey, tmdbSessionId, tmdbAccountId)
                            }
                            syncStatus = if (count > 0)
                                "✅ TMDB: synced $count items"
                            else
                                "✅ TMDB: connected & synced (watchlist empty)"
                            isSyncing = false
                            onNotifyUpdate?.invoke(
                                InAppUpdateMessage(
                                    title = "TMDB Synced",
                                    description = if (count > 0) "Synced $count items to watchlist" else "Watchlist is up to date",
                                    accentColor = IGBlue
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTmdbConnected) IGBlue.copy(alpha = 0.15f) else IGCard
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = if (isTmdbConnected) IGBlue else IGTextSecondary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Sync TMDB Watchlist",
                        color = if (isTmdbConnected) IGBlue else IGTextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // AniList OAuth card
        item {
            IGSettingsCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                // Header with connection status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IGSettingsSectionHeader(
                        icon = Icons.Default.Key,
                        title = "AniList Account",
                        subtitle = if (isAnilistConnected) "Connected — status syncing active" else "Connect to sync watch & read status"
                    )
                    // Status pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isAnilistConnected) IGGreen.copy(alpha = 0.15f) else IGCard)
                            .border(1.dp, if (isAnilistConnected) IGGreen.copy(alpha = 0.3f) else IGDivider, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isAnilistConnected) IGGreen else IGTextSecondary)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                if (isAnilistConnected) "Connected" else "Not linked",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAnilistConnected) IGGreen else IGTextSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // One-click AniList login button
                Button(
                    onClick = {
                        val id = clientIdInput.trim().ifBlank { "53626" }
                        val redirect = redirectUriInput.trim().ifBlank { "scenefinder://anilist-callback" }
                        val url = "https://anilist.co/api/v2/oauth/authorize?client_id=$id&redirect_uri=${Uri.encode(redirect)}&response_type=token"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IGGradMid),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isAnilistConnected) "Re-link AniList Account" else "Login with AniList",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Disconnect option
                if (isAnilistConnected) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            settings.setAnilistToken("")
                            Toast.makeText(context, "Disconnected from AniList", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IGRed.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Disconnect Account", color = IGRed, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Expandable Custom Developer Credentials
                var showAdvancedAniList by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showAdvancedAniList = !showAdvancedAniList }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (showAdvancedAniList) "▲ Custom API Client (Advanced)" else "▼ Custom API Client (Advanced)",
                        fontSize = 12.sp,
                        color = IGTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                AnimatedVisibility(
                    visible = showAdvancedAniList,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Only needed if you want to use your own personal AniList developer app.",
                            fontSize = 11.sp,
                            color = IGTextTertiary
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://anilist.co/settings/developer")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IGCard),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(14.dp), tint = IGGradMid)
                            Spacer(Modifier.width(6.dp))
                            Text("Open AniList Developer Settings", color = IGGradMid, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IGGradMid,
                    unfocusedBorderColor = IGDivider,
                    focusedLabelColor = IGGradMid,
                    unfocusedLabelColor = IGTextSecondary,
                    focusedTextColor = IGTextPrimary,
                    unfocusedTextColor = IGTextPrimary,
                    cursorColor = IGGradMid
                )

                OutlinedTextField(
                    value = clientIdInput,
                    onValueChange = { clientIdInput = it },
                    label = { Text("Client ID") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = clientSecretInput,
                    onValueChange = { clientSecretInput = it },
                    label = { Text("Client Secret") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = redirectUriInput,
                    onValueChange = { redirectUriInput = it },
                    label = { Text("Redirect URI") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    placeholder = { Text("scenefinder://anilist-callback", color = IGTextTertiary, fontSize = 12.sp) }
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Use scenefinder://anilist-callback as the redirect URI in your AniList app settings.",
                    fontSize = 11.sp,
                    color = IGTextTertiary
                )

                Spacer(Modifier.height(14.dp))

                // Save + Connect buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            settings.setAnilistClientId(clientIdInput)
                            settings.setAnilistClientSecret(clientSecretInput)
                            settings.setAnilistRedirectUri(redirectUriInput)
                            Toast.makeText(context, "Credentials saved!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IGCard),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save", color = IGTextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            settings.setAnilistClientId(clientIdInput)
                            settings.setAnilistClientSecret(clientSecretInput)
                            settings.setAnilistRedirectUri(redirectUriInput)
                            val id = clientIdInput.trim()
                            val redirect = redirectUriInput.trim().ifBlank { "scenefinder://anilist-callback" }
                            val url = "https://anilist.co/api/v2/oauth/authorize" +
                                "?client_id=$id&redirect_uri=${Uri.encode(redirect)}&response_type=code"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IGGradMid),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(2f)
                    ) {
                        Text("Connect with AniList", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // Disconnect option
                if (isAnilistConnected) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            settings.setAnilistToken("")
                            Toast.makeText(context, "Disconnected from AniList", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IGRed.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Disconnect Account", color = IGRed, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Optional manual token entry
                var showManualToken by remember { mutableStateOf(false) }
                var manualTokenInput by remember(currentToken) { mutableStateOf(currentToken) }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showManualToken = !showManualToken }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (showManualToken) "▲ Hide manual token field" else "▼ Or enter Access Token directly",
                        fontSize = 12.sp,
                        color = IGBlue,
                        fontWeight = FontWeight.Medium
                    )
                }

                AnimatedVisibility(
                    visible = showManualToken,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = manualTokenInput,
                            onValueChange = { manualTokenInput = it },
                            label = { Text("Personal Access Token") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors
                        )
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = {
                                settings.setAnilistToken(manualTokenInput.trim())
                                Toast.makeText(context, if (manualTokenInput.isNotBlank()) "Token saved!" else "Token cleared", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IGCard),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Access Token", color = IGTextPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // TMDB Account + Watchlist Sync
        item {
            IGSettingsCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IGSettingsSectionHeader(
                        icon = Icons.Default.Key,
                        title = "TMDB Account",
                        subtitle = if (isTmdbConnected) "Connected — watchlist sync active" else "Login to sync your TMDB watchlist"
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isTmdbConnected) IGGreen.copy(alpha = 0.15f) else IGCard)
                            .border(1.dp, if (isTmdbConnected) IGGreen.copy(alpha = 0.3f) else IGDivider, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(6.dp).clip(CircleShape)
                                    .background(if (isTmdbConnected) IGGreen else IGTextSecondary)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                if (isTmdbConnected) "Connected" else "Not linked",
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color = if (isTmdbConnected) IGGreen else IGTextSecondary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Login opens TMDB in your browser. After approving, return to the app to complete login.",
                    fontSize = 11.sp, color = IGTextTertiary
                )
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val key = currentApiKey.ifBlank { com.scenefinder.data.repository.MediaEnrichmentRepository.DEFAULT_TMDB_API_KEY }
                            scope.launch {
                                tmdbLoginPending = true
                                val token = withContext(Dispatchers.IO) {
                                    repository.getTmdbRequestToken(key)
                                }
                                if (token != null) {
                                    val approveUrl = "https://www.themoviedb.org/authenticate/$token?redirect_to=scenefinder://tmdb-callback"
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(approveUrl)).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                    )
                                    // After browser return, user must tap "Complete Login"
                                    settings.setTmdbSessionId("__pending__:$token")
                                } else {
                                    Toast.makeText(context, "Failed to start TMDB login", Toast.LENGTH_SHORT).show()
                                }
                                tmdbLoginPending = false
                            }
                        },
                        enabled = !tmdbLoginPending,
                        colors = ButtonDefaults.buttonColors(containerColor = IGBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(2f)
                    ) {
                        if (tmdbLoginPending) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Login with TMDB", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    if (tmdbSessionId.startsWith("__pending__:")) {
                        Button(
                            onClick = {
                                val key = currentApiKey.ifBlank { com.scenefinder.data.repository.MediaEnrichmentRepository.DEFAULT_TMDB_API_KEY }
                                val token = tmdbSessionId.removePrefix("__pending__:")
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        repository.createTmdbSession(key, token)
                                    }
                                    if (result != null) {
                                        settings.setTmdbSessionId(result.first)
                                        settings.setTmdbAccountId(result.second)
                                        Toast.makeText(context, "TMDB connected!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "TMDB login failed. Did you approve in browser?", Toast.LENGTH_LONG).show()
                                        settings.setTmdbSessionId("")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IGGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Complete", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                if (isTmdbConnected) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            settings.setTmdbSessionId("")
                            settings.setTmdbAccountId(-1)
                            Toast.makeText(context, "Disconnected from TMDB", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IGRed.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Disconnect TMDB Account", color = IGRed, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // TMDB API Key
        item {
            IGSettingsCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                IGSettingsSectionHeader(
                    icon = Icons.Default.Key,
                    title = "TMDB Key (Movies & Web Series)",
                    subtitle = "Enables live metadata, posters & season data for movies & web series"
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("TMDB API Key (v3 auth)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IGBlue,
                        unfocusedBorderColor = IGDivider,
                        focusedLabelColor = IGBlue,
                        unfocusedLabelColor = IGTextSecondary,
                        focusedTextColor = IGTextPrimary,
                        unfocusedTextColor = IGTextPrimary,
                        cursorColor = IGBlue
                    )
                )
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            settings.setTmdbApiKey(apiKeyInput)
                            Toast.makeText(context, "TMDB key saved!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IGBlue),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Key", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.themoviedb.org/settings/api")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IGCard),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(15.dp), tint = IGTextSecondary)
                        Text(" Get Key", color = IGTextSecondary, fontSize = 13.sp)
                    }
                }
            }
        }

        // Auto overlay toggle
        item {
            IGSettingsCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-display Floating Card", fontWeight = FontWeight.SemiBold, color = IGTextPrimary, fontSize = 15.sp)
                        Spacer(Modifier.height(2.dp))
                        Text("Show match result card over your screen automatically after scene capture.", style = MaterialTheme.typography.bodySmall, color = IGTextSecondary)
                    }
                    Switch(
                        checked = autoOverlay,
                        onCheckedChange = { settings.setAutoOverlay(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = IGGreen,
                            uncheckedThumbColor = IGTextSecondary,
                            uncheckedTrackColor = IGCard
                        )
                    )
                }
            }
        }

        // Home page layout: Page-by-Page vs Continuous scroll
        item {
            val isPageWise by settings.isPageWiseLayout.collectAsState()
            IGSettingsCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Page-by-Page Home Layout", fontWeight = FontWeight.SemiBold, color = IGTextPrimary, fontSize = 15.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (isPageWise) "Home library displayed page-by-page with Next/Prev page controls"
                            else "Home library displayed in a continuous single-page feed",
                            style = MaterialTheme.typography.bodySmall,
                            color = IGTextSecondary
                        )
                    }
                    Switch(
                        checked = isPageWise,
                        onCheckedChange = { settings.setPageWiseLayout(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = IGGradMid,
                            uncheckedThumbColor = IGTextSecondary,
                            uncheckedTrackColor = IGCard
                        )
                    )
                }
            }
        }

        // Arm timeout
        item {
            IGSettingsCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                IGSettingsSectionHeader(
                    icon = Icons.Default.Timer,
                    title = "Session Armed Timeout",
                    subtitle = "Auto-cancel duration if Circle to Search is not used after arming"
                )
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 45, 60).forEach { seconds ->
                        val isSelected = armTimeout == seconds
                        val bgColor by animateColorAsState(
                            targetValue = if (isSelected) IGBlue else IGCard,
                            animationSpec = tween(200),
                            label = "btn_bg"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgColor)
                                .clickable { settings.setArmTimeoutSeconds(seconds) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${seconds}s",
                                color = if (isSelected) Color.White else IGTextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // ── Appearance (Dark & Light Mode) ────────────────────────────────────
        item {
            val themeMode by settings.themeMode.collectAsState()
            val colors = LocalAppColors.current

            IGSettingsCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                IGSettingsSectionHeader(
                    icon = Icons.Default.BrightnessMedium,
                    title = "Appearance",
                    subtitle = "Choose Dark or Light mode"
                )
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (colors.isDark) Color(0xFF141416) else Color(0xFFF1F3F5))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ThemeMode.values().forEach { mode ->
                        val isSelected = themeMode == mode
                        val bg by animateColorAsState(
                            targetValue = if (isSelected) (if (colors.isDark) Color(0xFF2C2C34) else Color.White) else Color.Transparent,
                            animationSpec = tween(150),
                            label = "mode_bg"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(bg)
                                .clickable { settings.setThemeMode(mode) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = mode.icon, fontSize = 14.sp)
                                Text(
                                    text = mode.title.replace(" Mode", ""),
                                    color = if (isSelected) colors.primary else colors.textSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Recognition Engines & Service Health ──────────────────────────────
        item {
            val colors = LocalAppColors.current
            IGSettingsCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                IGSettingsSectionHeader(
                    icon = Icons.Default.Hub,
                    title = "Recognition Engines & Health",
                    subtitle = "Live status of connected identification & metadata APIs"
                )
                Spacer(Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // trace.moe
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.card)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("trace.moe Visual AI", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.textPrimary)
                            Text("Anime frame & scene identification", fontSize = 11.sp, color = colors.textSecondary)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(IGGreen.copy(alpha = 0.15f))
                                .border(0.5.dp, IGGreen.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("Operational", color = IGGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // TMDB API
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.card)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("The Movie Database (TMDB)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.textPrimary)
                            Text("Movies, TV shows, seasons & episode guide", fontSize = 11.sp, color = colors.textSecondary)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isTmdbConnected) IGBlue.copy(alpha = 0.15f) else IGGreen.copy(alpha = 0.15f))
                                .border(0.5.dp, if (isTmdbConnected) IGBlue.copy(alpha = 0.4f) else IGGreen.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(if (isTmdbConnected) "Sync Active" else "Ready", color = if (isTmdbConnected) IGBlue else IGGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // AniList GraphQL
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.card)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AniList GraphQL", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.textPrimary)
                            Text("Anime & Manga synchronization", fontSize = 11.sp, color = colors.textSecondary)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isAnilistConnected) IGGreen.copy(alpha = 0.15f) else colors.surface)
                                .border(0.5.dp, if (isAnilistConnected) IGGreen.copy(alpha = 0.4f) else colors.divider, RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(if (isAnilistConnected) "Connected" else "Standby", color = if (isAnilistConnected) IGGreen else colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Accessibility Screen Reader
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.card)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Circle-to-Search Screen Reader", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.textPrimary)
                            Text("Accessibility service node listener", fontSize = 11.sp, color = colors.textSecondary)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(IGGreen.copy(alpha = 0.15f))
                                .border(0.5.dp, IGGreen.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("Armed Engine", color = IGGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── How SceneFinder Works (Expandable Guide) ─────────────────────────
        item {
            val colors = LocalAppColors.current
            IGSettingsCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showHowItWorks = !showHowItWorks },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IGSettingsSectionHeader(
                        icon = Icons.Default.HelpOutline,
                        title = "How Ark Works",
                        subtitle = "Gesture & recognition guide"
                    )
                    Text(
                        if (showHowItWorks) "Hide ▲" else "View ▼",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = IGGradMid
                    )
                }

                AnimatedVisibility(
                    visible = showHowItWorks,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 14.dp)) {
                        listOf(
                            Triple("1", "Arm Ark", "Tap the Ark Quick Settings tile from your Android status bar, or use the floating dock."),
                            Triple("2", "Trigger Circle to Search", "Long-press the bottom navigation pill or Home button on any screen to start Circle to Search."),
                            Triple("3", "Circle Scene or Character", "Draw a circle around the anime scene, movie clip, actor, or manga panel you want to recognize."),
                            Triple("4", "Instant Enrichment & Sync", "Ark captures the scene, queries our AI visual engines, and displays the exact episode timestamp, synopsis, and 1-tap AniList/TMDB sync!")
                        ).forEach { (step, stepTitle, stepDesc) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(IGGradMid),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(step, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stepTitle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.textPrimary)
                                    Spacer(Modifier.height(2.dp))
                                    Text(stepDesc, fontSize = 11.5.sp, color = colors.textSecondary, lineHeight = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Library & History Storage ─────────────────────────────────────────
        item {
            val colors = LocalAppColors.current
            val totalItems = userLibrary.size
            val watchingCount = userLibrary.count { it.userStatus.equals("CURRENT", true) }
            val completedCount = userLibrary.count { it.userStatus.equals("COMPLETED", true) }
            val planningCount = userLibrary.count { it.userStatus.equals("PLANNING", true) }
            val animeCount = userLibrary.count { it.mediaType.equals("Anime", true) }
            val mangaCount = userLibrary.count { it.mediaType.equals("Manga", true) || it.mediaType.equals("Manhwa", true) }
            val movieCount = userLibrary.count { it.mediaType.equals("Movie", true) }
            val tvCount = userLibrary.count { it.mediaType.equals("TV Series", true) || it.mediaType.equals("Web Series", true) }

            IGSettingsCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                IGSettingsSectionHeader(
                    icon = Icons.Default.Storage,
                    title = "Library & Storage Diagnostics",
                    subtitle = "Local SQLite database and cached assets"
                )
                Spacer(Modifier.height(14.dp))

                // Stats grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Total stored
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.card)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$totalItems", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = IGGradMid)
                            Text("Total Saved", fontSize = 10.sp, color = colors.textSecondary)
                        }
                    }
                    // Watching
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.card)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$watchingCount", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0095F6))
                            Text("Watching", fontSize = 10.sp, color = colors.textSecondary)
                        }
                    }
                    // Completed
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.card)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$completedCount", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = IGGreen)
                            Text("Completed", fontSize = 10.sp, color = colors.textSecondary)
                        }
                    }
                    // Planning
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.card)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$planningCount", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFFA855F7))
                            Text("Plan to Watch", fontSize = 10.sp, color = colors.textSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Media type breakdown row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Anime: $animeCount",
                        "Manga: $mangaCount",
                        "Movies: $movieCount",
                        "TV: $tvCount"
                    ).forEach { chipText ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.card)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(chipText, fontSize = 10.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Clear Image Cache button
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            try {
                                context.cacheDir.deleteRecursively()
                            } catch (_: Throwable) {}
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Asset & image cache freed successfully", Toast.LENGTH_SHORT).show()
                                onNotifyUpdate?.invoke(
                                    InAppUpdateMessage(
                                        title = "Cache Cleared",
                                        description = "Temporary asset & image cache freed",
                                        accentColor = Color(0xFF10B981)
                                    )
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.card,
                        contentColor = colors.textPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.5.dp, colors.divider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = "Clear Cache",
                        modifier = Modifier.size(16.dp),
                        tint = IGGradMid
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Clear Temporary Image Cache", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                }

                Spacer(Modifier.height(8.dp))

                // Clear All History & Library button
                Button(
                    onClick = { showClearDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IGRed.copy(alpha = 0.12f),
                        contentColor = IGRed
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, IGRed.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Clear History",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Clear All History & Library",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }
            }
        }

        // ── What's New & Release Notes Card ───────────────────────────────────
        if (onShowWhatsNew != null) {
            item {
                val colors = LocalAppColors.current
                IGSettingsCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IGSettingsSectionHeader(
                            icon = Icons.Default.AutoAwesome,
                            title = "Release Notes & Updates",
                            subtitle = "Explore features in version 2.4.2"
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(IGGradMid.copy(alpha = 0.18f))
                                .border(0.5.dp, IGGradMid, RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text("v2.4.2", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IGGradMid)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onShowWhatsNew,
                        colors = ButtonDefaults.buttonColors(containerColor = IGCard),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, colors.divider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = IGGradMid,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "View What's New Tour",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }

        // ── About SceneFinder & System Specs ──────────────────────────────────
        item {
            val colors = LocalAppColors.current
            IGSettingsCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                IGSettingsSectionHeader(
                    icon = Icons.Default.Info,
                    title = "System & Engine Specifications",
                    subtitle = "Application build and runtime environment"
                )
                Spacer(Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "Ark Engine" to "v2.4.2 (Universal Enrichment)",
                        "Architecture" to "Jetpack Compose 1.7 / Material 3",
                        "Local Database" to "Room SQLite with FTS4 Search",
                        "Display Refresh Engine" to "120Hz LTPO Adaptive Mode Active",
                        "Screen Reader Service" to "Foreground Accessibility Engine"
                    ).forEach { (key, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(key, fontSize = 11.5.sp, color = colors.textSecondary)
                            Text(value, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All History?", color = IGTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will remove all scanned and saved anime, movies, and manga from your local library. Cloud libraries (AniList/TMDB) will remain untouched.",
                    color = IGTextSecondary,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        scope.launch(Dispatchers.IO) {
                            repository.clearAllHistory()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "History cleared successfully", Toast.LENGTH_SHORT).show()
                                onNotifyUpdate?.invoke(
                                    InAppUpdateMessage(
                                        title = "Library Reset",
                                        description = "Local history & scanned entries cleared",
                                        accentColor = Color(0xFFEF4444)
                                    )
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IGRed)
                ) {
                    Text("Clear All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = IGTextSecondary)
                }
            },
            containerColor = IGCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun IGSettingsCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = LocalAppColors.current
    val cardBorder = if (colors.isDark) {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF00F5FF).copy(alpha = 0.25f),
                Color(0xFFBF00FF).copy(alpha = 0.15f),
                Color(0xFF00F5FF).copy(alpha = 0.10f)
            )
        )
    } else {
        Brush.horizontalGradient(listOf(colors.divider, colors.divider))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column { content() }
    }
}

@Composable
private fun IGSettingsSectionHeader(icon: ImageVector, title: String, subtitle: String) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (colors.isDark) Color(0xFF161B32) else colors.card)
                .border(
                    0.8.dp,
                    if (colors.isDark) Color(0xFF00F5FF).copy(alpha = 0.35f) else Color.Transparent,
                    RoundedCornerShape(11.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = IGGradMid, modifier = Modifier.size(19.dp))
        }
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = colors.textPrimary, fontSize = 15.sp)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
}
