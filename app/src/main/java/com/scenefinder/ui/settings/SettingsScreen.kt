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

    var apiKeyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var clientIdInput by remember(currentClientId) { mutableStateOf(currentClientId) }
    var clientSecretInput by remember(currentClientSecret) { mutableStateOf(currentClientSecret) }
    var redirectUriInput by remember(currentRedirectUri) { mutableStateOf(currentRedirectUri) }

    var syncStatus by remember { mutableStateOf("") }
    var isSyncing by remember { mutableStateOf(false) }
    var tmdbLoginPending by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

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
                    text = "Configure your SceneFinder accounts and preferences",
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

        // Clear History & Library option at the very last
        item {
            IGSettingsCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                IGSettingsSectionHeader(
                    icon = Icons.Default.DeleteOutline,
                    title = "Library & History Storage",
                    subtitle = "Manage your scanned entries and local library"
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { showClearDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IGRed.copy(alpha = 0.15f),
                        contentColor = IGRed
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, IGRed.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Clear History",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Clear All History & Library",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(IGSurface)
            .border(0.5.dp, IGDivider, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column { content() }
    }
}

@Composable
private fun IGSettingsSectionHeader(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(IGCard),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = IGGradMid, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, color = IGTextPrimary, fontSize = 15.sp)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = IGTextSecondary)
        }
    }
}
