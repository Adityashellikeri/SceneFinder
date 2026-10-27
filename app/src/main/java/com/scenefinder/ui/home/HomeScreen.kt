package com.scenefinder.ui.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.scenefinder.data.local.dao.EnrichedMediaDao
import com.scenefinder.data.local.entity.EnrichedMediaEntity
import com.scenefinder.data.prefs.AppSettings
import com.scenefinder.data.repository.MediaEnrichmentRepository
import com.scenefinder.ui.components.AgeRatingBadge
import com.scenefinder.ui.components.CyberpunkGridBackground
import com.scenefinder.ui.components.NeonPulsingDot
import com.scenefinder.ui.components.ProgressTrackingDialog
import com.scenefinder.ui.components.InAppUpdateMessage
import com.scenefinder.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Cyberpunk HUD & Neon Glass Design System
private val GlassDark = Color(0xF2090D1C)
private val GlassLight = Color(0xF8FFFFFF)
private val CyanAccent = NeonCyan
private val BlueAccent = Color(0xFF0A84FF)
private val PurpleAccent = NeonViolet
private val MagentaAccent = NeonMagenta
private val GoldAccent = NeonYellowGreen
private val GreenAccent = NeonGreenLime
private val OrangeAccent = NeonOrangePink
private val DarkBg = CyberBlack

private val HeroRimBrush = Brush.linearGradient(
    listOf(NeonCyan.copy(alpha = 0.50f), NeonMagenta.copy(alpha = 0.35f), Color.Transparent)
)
private val HeroScrimBrush = Brush.verticalGradient(
    listOf(Color.Black.copy(alpha = 0.30f), Color.Transparent, Color.Black.copy(alpha = 0.75f), Color.Black.copy(alpha = 0.96f))
)
private val CompactRimBrush = Brush.linearGradient(
    listOf(NeonCyan.copy(alpha = 0.40f), NeonViolet.copy(alpha = 0.25f), Color.Transparent)
)
private val CompactPosterScrim = Brush.verticalGradient(
    listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.75f))
)

// Pre-computed rank badge gradients — allocated once, never reallocated on recomposition
private val RankBrushGold   = Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))
private val RankBrushSilver = Brush.horizontalGradient(listOf(Color(0xFFE0E0E0), Color(0xFFB0B0B0)))
private val RankBrushBronze = Brush.horizontalGradient(listOf(Color(0xFFCD7F32), Color(0xFF8B4513)))
private val RankBrushDefault = Brush.horizontalGradient(
    listOf(Color.Black.copy(alpha = 0.82f), Color.Black.copy(alpha = 0.82f))
)

// Guard so database deduplication only runs once per app session (prevents Room invalidation on tab switch)
private var hasRunDeduplicationOnce = false

@Composable
fun HomeScreen(
    dao: EnrichedMediaDao,
    repository: MediaEnrichmentRepository,
    settings: AppSettings,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onShowWhatsNew: (() -> Unit)? = null,
    onNotifyUpdate: ((InAppUpdateMessage) -> Unit)? = null,
    onCardClick: (EnrichedMediaEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val colors = LocalAppColors.current

    val anilistToken by settings.anilistToken.collectAsState()
    val apiKey by settings.tmdbApiKey.collectAsState()
    val tmdbSessionId by settings.tmdbSessionId.collectAsState()
    val tmdbAccountId by settings.tmdbAccountId.collectAsState()

    val userLibrary by dao.getAllHistory().collectAsState(initial = emptyList())

    // Deduplicate user library entries favoring COMPLETED over PLANNING
    val cleanLibrary = remember(userLibrary) {
        userLibrary.groupBy { it.cleanTitle.trim().lowercase() }.values.map { group ->
            group.maxWithOrNull(
                compareBy<EnrichedMediaEntity> {
                    when (it.userStatus.uppercase()) {
                        "COMPLETED" -> 4
                        "CURRENT" -> 3
                        "PAUSED", "REPEATING" -> 2
                        "PLANNING" -> 1
                        else -> 0
                    }
                }.thenBy { it.cachedAt }
            ) ?: group.first()
        }
    }

    // Fast O(1) matching between feed items and user's saved library
    val libraryById = remember(cleanLibrary) { cleanLibrary.associateBy { it.id } }
    val libraryByTitle = remember(cleanLibrary) {
        cleanLibrary.associateBy { it.cleanTitle.trim().lowercase() }
    }

    fun findMatchingLibraryEntry(media: EnrichedMediaEntity): EnrichedMediaEntity? {
        return libraryById[media.id] ?: libraryByTitle[media.cleanTitle.trim().lowercase()]
    }

    val cachedRecs = remember { repository.getCachedHomeRecommendations() ?: emptyList() }
    val cachedAnime = remember { repository.getCachedTopAnime() ?: emptyList() }
    val cachedManga = remember { repository.getCachedTopManga() ?: emptyList() }
    val cachedManhwa = remember { repository.getCachedTopManhwa() ?: emptyList() }
    val cachedMovies = remember { repository.getCachedTopMovies() ?: emptyList() }
    val cachedTv = remember { repository.getCachedTopTv() ?: emptyList() }

    val hasPreloadedData = cachedRecs.isNotEmpty() || cachedAnime.isNotEmpty()

    var recommendations by remember { mutableStateOf(cachedRecs) }
    var topAnime by remember { mutableStateOf(cachedAnime) }
    var topManga by remember { mutableStateOf(cachedManga) }
    var topManhwa by remember { mutableStateOf(cachedManhwa) }
    var topMovies by remember { mutableStateOf(cachedMovies) }
    var topTv by remember { mutableStateOf(cachedTv) }
    var isLoading by remember { mutableStateOf(!hasPreloadedData) }
    var selectedMediaForDetail by remember { mutableStateOf<EnrichedMediaEntity?>(null) }
    var trackingDialogMedia by remember { mutableStateOf<EnrichedMediaEntity?>(null) }
    var trackingTotalEpisodes by remember { mutableStateOf<Int?>(null) }
    var trackingTotalChapters by remember { mutableStateOf<Int?>(null) }

    var isRefreshingRecs by remember { mutableStateOf(false) }
    var selectedDiscoverCategory by remember { mutableStateOf("Anime") }
    val initialDiscover = remember { repository.getCachedDiscover("Anime") ?: emptyList() }
    var discoverItems by remember { mutableStateOf(initialDiscover) }
    var isDiscoverLoading by remember { mutableStateOf(initialDiscover.isEmpty()) }
    val discoverJobRef = remember { mutableStateOf<Job?>(null) }

    val fetchDiscover: (String) -> Unit = { category ->
        discoverJobRef.value?.cancel()
        // If we don't have items, show shimmer; if we do have items, keep showing them while loading in background
        if (discoverItems.isEmpty()) {
            isDiscoverLoading = true
        }
        val job = scope.launch(Dispatchers.IO) {
            try {
                val items = repository.getRandomDiscover(category, apiKey)
                if (isActive) {
                    withContext(Dispatchers.Main) {
                        if (items.isNotEmpty()) {
                            discoverItems = items
                        }
                        isDiscoverLoading = false
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { isDiscoverLoading = false }
            }
        }
        discoverJobRef.value = job
    }

    val handleRefreshRecommendations: () -> Unit = {
        if (!isRefreshingRecs) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            isRefreshingRecs = true
            scope.launch(Dispatchers.IO) {
                repository.clearRecommendationsCache()
                val fresh = repository.getHomeRecommendations(userLibrary, forceRefresh = true)
                withContext(Dispatchers.Main) {
                    if (fresh.isNotEmpty()) {
                        recommendations = fresh
                    }
                    isRefreshingRecs = false
                }
            }
        }
    }

    val handleOpenTracking: (EnrichedMediaEntity) -> Unit = { item ->
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        trackingDialogMedia = item
        trackingTotalEpisodes = item.episode
        trackingTotalChapters = item.chapter
        scope.launch(Dispatchers.IO) {
            val totals = repository.getTotalEpisodesOrChapters(item, apiKey)
            withContext(Dispatchers.Main) {
                if (totals.first != null) trackingTotalEpisodes = totals.first
                if (totals.second != null) trackingTotalChapters = totals.second
            }
        }
    }

    val handleMarkWatchedOrRead: (EnrichedMediaEntity) -> Unit = { item ->
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val targetItem = findMatchingLibraryEntry(item) ?: item
        scope.launch(Dispatchers.IO) {
            val totals = repository.getTotalEpisodesOrChapters(targetItem, apiKey)
            val totalEp = totals.first ?: targetItem.episode ?: (if (targetItem.mediaType.equals("Movie", true)) 1 else null)
            val totalCh = totals.second ?: targetItem.chapter

            val completedItem = targetItem.copy(
                userStatus = "COMPLETED",
                episode = totalEp,
                chapter = totalCh
            )
            repository.saveToLibrary(
                completedItem,
                anilistToken,
                apiKey,
                tmdbSessionId,
                tmdbAccountId
            )
            repository.updateMediaProgress(
                media = targetItem,
                newStatus = "COMPLETED",
                episode = totalEp,
                chapter = totalCh,
                anilistToken = anilistToken,
                tmdbApiKey = apiKey,
                tmdbSessionId = tmdbSessionId,
                tmdbAccountId = tmdbAccountId
            )
            launch(Dispatchers.Main) {
                val isManga = targetItem.mediaType.equals("Manga", true) || targetItem.mediaType.equals("Manhwa", true)
                val countText = if (isManga) {
                    if (totalCh != null) "$totalCh chapters" else "all chapters"
                } else {
                    if (totalEp != null) "$totalEp episodes" else "all episodes"
                }
                val verb = if (isManga) "Read" else "Watched"
                val msg = "Marked as $verb ($countText) & Synced!"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                onNotifyUpdate?.invoke(
                    InAppUpdateMessage(
                        title = "Marked as $verb",
                        description = "${targetItem.cleanTitle} ($countText)",
                        accentColor = Color(0xFF10B981)
                    )
                )
            }
        }
    }

    // Instead of immediately saving as PLANNING, opens the rich Library Popup Dialog!
    val handleAddToLibrary: (EnrichedMediaEntity) -> Unit = { item ->
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        selectedMediaForDetail = findMatchingLibraryEntry(item) ?: item
    }

    val handleStatusChange: (EnrichedMediaEntity, String) -> Unit = { media, newStatus ->
        selectedMediaForDetail = null
        val target = findMatchingLibraryEntry(media) ?: media
        if (newStatus == "CURRENT") {
            handleOpenTracking(target.copy(userStatus = "CURRENT"))
        }
        if (newStatus == "COMPLETED") {
            handleMarkWatchedOrRead(target)
        } else {
            scope.launch(Dispatchers.IO) {
                val updated = target.copy(
                    userStatus = newStatus,
                    cachedAt = System.currentTimeMillis()
                )
                val synced = repository.saveToLibrary(
                    updated,
                    anilistToken,
                    apiKey,
                    tmdbSessionId,
                    tmdbAccountId
                )
                repository.updateMediaStatus(
                    media = updated,
                    newStatus = newStatus,
                    anilistToken = anilistToken,
                    tmdbApiKey = apiKey,
                    tmdbSessionId = tmdbSessionId,
                    tmdbAccountId = tmdbAccountId
                )
                launch(Dispatchers.Main) {
                    val isRead = media.mediaType.equals("Manga", true) || media.mediaType.equals("Manhwa", true)
                    val label = when (newStatus) {
                        "CURRENT" -> if (isRead) "Reading" else "Watching"
                        "COMPLETED" -> "Completed"
                        "DROPPED" -> "Dropped"
                        else -> if (isRead) "Plan to Read" else "Plan to Watch"
                    }
                    val isAnime = media.mediaType.equals("Anime", true) || media.mediaType.equals("Manga", true)
                    val target = if (isAnime) "AniList" else "TMDB"
                    val msg = if (synced) "Added as $label & synced to $target!" else "Saved as $label"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    onNotifyUpdate?.invoke(
                        InAppUpdateMessage(
                            title = "Status: $label",
                            description = media.cleanTitle,
                            accentColor = Color(0xFF00F2FE)
                        )
                    )
                }
            }
        }
    }

    val handleRemoveFromLibrary: (EnrichedMediaEntity) -> Unit = { media ->
        scope.launch(Dispatchers.IO) {
            repository.removeFromLibrary(
                media = media,
                anilistToken = anilistToken,
                tmdbApiKey = apiKey,
                tmdbSessionId = tmdbSessionId,
                tmdbAccountId = tmdbAccountId
            )
            launch(Dispatchers.Main) {
                Toast.makeText(context, "Removed from Library", Toast.LENGTH_SHORT).show()
                onNotifyUpdate?.invoke(
                    InAppUpdateMessage(
                        title = "Removed from Library",
                        description = media.cleanTitle,
                        accentColor = Color(0xFFEF4444)
                    )
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasRunDeduplicationOnce) {
            hasRunDeduplicationOnce = true
            withContext(Dispatchers.IO) {
                repository.cleanupDuplicateHistory()
            }
        }
        if (discoverItems.isEmpty()) {
            fetchDiscover("Anime")
        }
        if (!hasPreloadedData) {
            withContext(Dispatchers.IO) {
                try {
                    coroutineScope {
                        val recsDeferred = async { runCatching { repository.getHomeRecommendations(userLibrary) }.getOrDefault(emptyList()) }
                        val animeDeferred = async { runCatching { repository.getTopAnime() }.getOrDefault(emptyList()) }
                        val mangaDeferred = async { runCatching { repository.getTopManga() }.getOrDefault(emptyList()) }
                        val manhwaDeferred = async { runCatching { repository.getTopManhwa() }.getOrDefault(emptyList()) }
                        val moviesDeferred = async { runCatching { repository.getTopMovies(apiKey) }.getOrDefault(emptyList()) }
                        val tvDeferred = async { runCatching { repository.getTopTvSeries(apiKey) }.getOrDefault(emptyList()) }

                        var recs = recsDeferred.await()
                        val anime = animeDeferred.await()
                        val manga = mangaDeferred.await()
                        val manhwa = manhwaDeferred.await()
                        val movies = moviesDeferred.await()
                        val tv = tvDeferred.await()

                        // Safe fallback for personalized recommendations if network returned empty
                        if (recs.isEmpty() && userLibrary.isNotEmpty()) {
                            recs = userLibrary.shuffled().take(10)
                        }

                        withContext(Dispatchers.Main) {
                            recommendations = recs
                            topAnime = anime
                            topManga = manga
                            topManhwa = manhwa
                            topMovies = movies
                            topTv = tv
                            isLoading = false
                        }
                    }
                } catch (_: Throwable) {
                    withContext(Dispatchers.Main) {
                        if (recommendations.isEmpty() && userLibrary.isNotEmpty()) {
                            recommendations = userLibrary.shuffled().take(10)
                        }
                        isLoading = false
                    }
                }
            }
        }
    }

    // Safe Top Bar calculation to guarantee no status bar overlap on any phone
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val safeTopPadding = maxOf(statusBarTop, 44.dp)

    // Ambient glow colours — radial center is set inside drawBehind so we can use real pixel coords
    // Using Float.MAX_VALUE as center causes Skia/Vulkan float overflow → GPU stall every frame
    val isDarkMode = colors.isDark

    val listState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (colors.isDark) DarkBg else Color(0xFFF2F2F7))
    ) {
        // Ambient glow drawn directly on canvas — no extra Box composables, zero overdraw cost
        // Glow centres are now in real canvas pixel coords to avoid shader NaN/overflow on LTPO panels
        if (isDarkMode) {
            CyberpunkGridBackground()
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Header Top Bar with Safe Insets, Custom App Logo, and Stylish Typography
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = safeTopPadding + 6.dp, start = 20.dp, end = 20.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Custom App Icon Badge
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .border(
                                        1.dp,
                                        Brush.linearGradient(listOf(CyanAccent, MagentaAccent)),
                                        RoundedCornerShape(9.dp)
                                    )
                            ) {
                                Image(
                                    painter = painterResource(id = com.scenefinder.R.mipmap.ic_launcher_scenefinder),
                                    contentDescription = "SceneFinder Logo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Stylish Cyberpunk App Name Typography
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Scene",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.8).sp,
                                    style = androidx.compose.ui.text.TextStyle(
                                        brush = Brush.horizontalGradient(
                                            listOf(CyanAccent, MagentaAccent)
                                        )
                                    )
                                )
                                Text(
                                    text = "Finder",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = 0.4.sp,
                                    color = colors.textPrimary
                                )
                            }

                            // Cyberpunk Frosted Glass Pill Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(CyanAccent.copy(alpha = 0.14f))
                                    .border(
                                        0.75.dp,
                                        CyanAccent.copy(alpha = 0.65f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    NeonPulsingDot(color = CyanAccent, size = 6.dp)
                                    Text(
                                        text = "AI FEED",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CyanAccent,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "Discover personalized anime, manga & movies",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }

                    // Frosted Glass Action Buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onShowWhatsNew != null) {
                            GlassCircularButton(
                                icon = Icons.Default.AutoAwesome,
                                contentDescription = "What's New",
                                isDark = colors.isDark
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onShowWhatsNew()
                            }
                        }
                        GlassCircularButton(
                            icon = Icons.Default.Settings,
                            contentDescription = "Settings",
                            isDark = colors.isDark
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNavigateToSettings()
                        }
                    }
                }
            }

            // Interactive What's New Release Card
            if (onShowWhatsNew != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        CyanAccent.copy(alpha = 0.12f),
                                        BlueAccent.copy(alpha = 0.16f)
                                    )
                                )
                            )
                            .border(
                                0.75.dp,
                                CyanAccent.copy(alpha = 0.40f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onShowWhatsNew()
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(CyanAccent.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "What's New in v2.4.2",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CyanAccent.copy(alpha = 0.2f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "UPDATES",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = CyanAccent
                                        )
                                    }
                                }
                                Text(
                                    text = "Expandable synopsis, live engine health, storage analytics & real-time updates",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                        Text(
                            text = "View →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    ShimmerLoadingFeed(isDark = colors.isDark)
                }
            } else {
                // Section 1: Hero Carousel (Recommendations)
                if (recommendations.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Recommended For You",
                            subtitle = "Personalized from trending anime, manga & movies",
                            accentColor = CyanAccent,
                            icon = Icons.Default.AutoAwesome,
                            trailingContent = {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CyanAccent.copy(alpha = 0.15f))
                                        .border(0.75.dp, CyanAccent.copy(alpha = 0.50f), RoundedCornerShape(10.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = handleRefreshRecommendations
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        RotatingRefreshIcon(
                                            isRefreshing = isRefreshingRecs,
                                            tint = CyanAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isRefreshingRecs) "Refreshing" else "Refresh",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyanAccent
                                        )
                                    }
                                }
                            }
                        )
                    }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            items(recommendations, key = { it.id }, contentType = { "HeroCard" }) { item ->
                                val libraryEntry = findMatchingLibraryEntry(item)
                                val effectiveItem = libraryEntry ?: item
                                val isSaved = libraryEntry != null

                                CinematicHeroCard(
                                    media = effectiveItem,
                                    libraryMatch = libraryEntry,
                                    isSaved = isSaved,
                                    isDark = colors.isDark,
                                    onCardClick = { selectedMediaForDetail = effectiveItem },
                                    onAddToLibrary = { handleAddToLibrary(effectiveItem) },
                                    onWatching = { handleOpenTracking(effectiveItem) },
                                    onMarkWatchedOrRead = { handleMarkWatchedOrRead(effectiveItem) },
                                    onToggleLibrary = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        handleAddToLibrary(effectiveItem)
                                    }
                                )
                            }
                        }
                    }
                }

                // Section: Discover & Explore (Category-based Randomizer for Movies, TV, Anime, Manga, Manhwa)
                item {
                    SectionHeader(
                        title = "Discover & Explore",
                        subtitle = "Random handpicked gems in $selectedDiscoverCategory",
                        accentColor = Color(0xFFFF5E7E),
                        icon = Icons.Default.Casino,
                        trailingContent = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFF5E7E).copy(alpha = 0.15f))
                                    .border(0.75.dp, Color(0xFFFF5E7E).copy(alpha = 0.50f), RoundedCornerShape(10.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            fetchDiscover(selectedDiscoverCategory)
                                        }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = Color(0xFFFF5E7E),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Shuffle",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF5E7E)
                                    )
                                }
                            }
                        }
                    )
                }

                // Category Selection Chips Row
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        items(listOf("Anime", "Manga", "Manhwa", "Movie", "TV Series"), contentType = { "CategoryChip" }) { cat ->
                            val isSelected = cat.equals(selectedDiscoverCategory, ignoreCase = true)
                            val chipAccent = when (cat) {
                                "Anime" -> PurpleAccent
                                "Manga" -> GreenAccent
                                "Manhwa" -> Color(0xFF32D74B)
                                "Movie" -> BlueAccent
                                else -> OrangeAccent
                            }
                            val chipIcon = when (cat) {
                                "Anime" -> Icons.Default.Tv
                                "Manga" -> Icons.Default.AutoStories
                                "Manhwa" -> Icons.Default.AutoStories
                                "Movie" -> Icons.Default.Movie
                                else -> Icons.Default.Tv
                            }
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.04f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "cat_scale"
                            )

                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) chipAccent.copy(alpha = 0.22f)
                                        else if (colors.isDark) Color(0x18FFFFFF) else Color(0x0C000000)
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.6.dp,
                                        color = if (isSelected) chipAccent.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            if (selectedDiscoverCategory != cat) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedDiscoverCategory = cat
                                                fetchDiscover(cat)
                                            }
                                        }
                                    )
                                    .padding(horizontal = 13.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = chipIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) chipAccent else colors.textSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = cat,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) chipAccent else colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Discover Items Carousel
                item {
                    if (isDiscoverLoading && discoverItems.isEmpty()) {
                        CompactRowShimmer(isDark = colors.isDark)
                    } else if (discoverItems.isNotEmpty()) {
                        CompactCardRow(
                            items = discoverItems,
                            libraryLookup = ::findMatchingLibraryEntry,
                            isDark = colors.isDark,
                            onCardClick = { selectedMediaForDetail = it },
                            onAddToLibrary = handleAddToLibrary,
                            onWatchingClick = handleOpenTracking,
                            onMarkWatchedOrRead = handleMarkWatchedOrRead
                        )
                    } else {
                        // Resilient retry fallback UI
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (colors.isDark) Color(0x1AFFFFFF) else Color(0x0D000000))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    fetchDiscover(selectedDiscoverCategory)
                                }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Tap to load $selectedDiscoverCategory",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }

                // Section 2: Top Anime
                if (topAnime.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Top Anime",
                            subtitle = "Highest rated & trending anime series",
                            accentColor = PurpleAccent,
                            icon = Icons.AutoMirrored.Filled.TrendingUp
                        )
                    }
                    item {
                        CompactCardRow(
                            items = topAnime,
                            libraryLookup = ::findMatchingLibraryEntry,
                            isDark = colors.isDark,
                            onCardClick = { selectedMediaForDetail = it },
                            onAddToLibrary = handleAddToLibrary,
                            onWatchingClick = handleOpenTracking,
                            onMarkWatchedOrRead = handleMarkWatchedOrRead
                        )
                    }
                }

                // Section 3: Top Manga
                if (topManga.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Top Manga",
                            subtitle = "Legendary manga publications & classics",
                            accentColor = GreenAccent,
                            icon = Icons.Default.AutoStories
                        )
                    }
                    item {
                        CompactCardRow(
                            items = topManga,
                            libraryLookup = ::findMatchingLibraryEntry,
                            isDark = colors.isDark,
                            onCardClick = { selectedMediaForDetail = it },
                            onAddToLibrary = handleAddToLibrary,
                            onWatchingClick = handleOpenTracking,
                            onMarkWatchedOrRead = handleMarkWatchedOrRead
                        )
                    }
                }

                // Section 4: Top Manhwa
                if (topManhwa.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Top Manhwa",
                            subtitle = "Popular Korean webtoons & ongoing hits",
                            accentColor = Color(0xFF32D74B),
                            icon = Icons.Default.AutoStories
                        )
                    }
                    item {
                        CompactCardRow(
                            items = topManhwa,
                            libraryLookup = ::findMatchingLibraryEntry,
                            isDark = colors.isDark,
                            onCardClick = { selectedMediaForDetail = it },
                            onAddToLibrary = handleAddToLibrary,
                            onWatchingClick = handleOpenTracking,
                            onMarkWatchedOrRead = handleMarkWatchedOrRead
                        )
                    }
                }

                // Section 5: Top Movies
                if (topMovies.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Top Movies",
                            subtitle = "Critically acclaimed & box-office hits",
                            accentColor = BlueAccent,
                            icon = Icons.Default.Movie
                        )
                    }
                    item {
                        CompactCardRow(
                            items = topMovies,
                            libraryLookup = ::findMatchingLibraryEntry,
                            isDark = colors.isDark,
                            onCardClick = { selectedMediaForDetail = it },
                            onAddToLibrary = handleAddToLibrary,
                            onWatchingClick = handleOpenTracking,
                            onMarkWatchedOrRead = handleMarkWatchedOrRead
                        )
                    }
                }

                // Section 6: Top TV Series
                if (topTv.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Top TV Series",
                            subtitle = "Binge-worthy drama, sci-fi & comedy shows",
                            accentColor = OrangeAccent,
                            icon = Icons.Default.Tv
                        )
                    }
                    item {
                        CompactCardRow(
                            items = topTv,
                            libraryLookup = ::findMatchingLibraryEntry,
                            isDark = colors.isDark,
                            onCardClick = { selectedMediaForDetail = it },
                            onAddToLibrary = handleAddToLibrary,
                            onWatchingClick = handleOpenTracking,
                            onMarkWatchedOrRead = handleMarkWatchedOrRead
                        )
                    }
                }
            }
        }

        // Episode & Chapter Progress Tracking Dialog
        trackingDialogMedia?.let { media ->
            ProgressTrackingDialog(
                media = media,
                totalEpisodes = trackingTotalEpisodes,
                totalChapters = trackingTotalChapters,
                onDismiss = { trackingDialogMedia = null },
                onMarkAllCompleted = { m ->
                    trackingDialogMedia = null
                    handleMarkWatchedOrRead(m)
                },
                onSaveProgress = { m, ep, ch ->
                    trackingDialogMedia = null
                    scope.launch(Dispatchers.IO) {
                        repository.saveToLibrary(
                            m.copy(userStatus = "CURRENT", episode = ep, chapter = ch),
                            anilistToken,
                            apiKey,
                            tmdbSessionId,
                            tmdbAccountId
                        )
                        repository.updateMediaProgress(
                            media = m,
                            newStatus = "CURRENT",
                            episode = ep,
                            chapter = ch,
                            anilistToken = anilistToken,
                            tmdbApiKey = apiKey,
                            tmdbSessionId = tmdbSessionId,
                            tmdbAccountId = tmdbAccountId
                        )
                        launch(Dispatchers.Main) {
                            val isManga = m.mediaType.equals("Manga", true) ||
                                    m.mediaType.equals("Manhwa", true)
                            val unit = if (isManga) "Ch. ${ch ?: 1}" else "Ep. ${ep ?: 1}"
                            Toast.makeText(context, "Saved — Watching ($unit)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        // Rich Library Modal Detail/Status Dialog (Popup from Add to Library or Card Tap)
        selectedMediaForDetail?.let { detailItem ->
            val latestMedia = findMatchingLibraryEntry(detailItem) ?: detailItem
            HomeMediaDetailDialog(
                media = latestMedia,
                isSaved = findMatchingLibraryEntry(detailItem) != null,
                onDismiss = { selectedMediaForDetail = null },
                onStatusChange = { newStatus ->
                    handleStatusChange(latestMedia, newStatus)
                },
                onTrackProgress = {
                    selectedMediaForDetail = null
                    handleOpenTracking(latestMedia)
                },
                onOpen = {
                    latestMedia.directWebUrl?.let { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
                },
                onDelete = {
                    selectedMediaForDetail = null
                    handleRemoveFromLibrary(latestMedia)
                }
            )
        }
    }
}

// -------------------------------------------------------------
// UI COMPONENTS (Apple Liquid Glass System & Micro-Animations)
// -------------------------------------------------------------

@Composable
private fun RotatingRefreshIcon(
    isRefreshing: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    if (isRefreshing) {
        val transition = rememberInfiniteTransition(label = "spin_isolated")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(850, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spin_angle"
        )
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Refreshing",
            tint = tint,
            modifier = modifier.graphicsLayer { rotationZ = angle }
        )
    } else {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Refresh",
            tint = tint,
            modifier = modifier
        )
    }
}

@Composable
private fun rememberShimmerBrush(isDark: Boolean): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1800f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )
    val shimmerColors = if (isDark) {
        listOf(
            Color(0xFF141628).copy(alpha = 0.50f),
            Color(0xFF262A48).copy(alpha = 0.85f),
            Color(0xFF141628).copy(alpha = 0.50f)
        )
    } else {
        listOf(
            Color(0xFFE2E4EC).copy(alpha = 0.50f),
            Color(0xFFF7F8FC).copy(alpha = 0.90f),
            Color(0xFFE2E4EC).copy(alpha = 0.50f)
        )
    }
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 700f, translateAnim - 700f),
        end = Offset(translateAnim, translateAnim)
    )
}

@Composable
private fun ShimmerLoadingFeed(isDark: Boolean) {
    val shimmerBrush = rememberShimmerBrush(isDark)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // Hero Section Skeleton
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerBrush)
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(260.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmerBrush)
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .width(280.dp)
                            .height(340.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(shimmerBrush)
                    )
                }
            }
        }

        // Section Row Skeleton
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerBrush)
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(146.dp)
                            .height(260.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(shimmerBrush)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactRowShimmer(isDark: Boolean) {
    val shimmerBrush = rememberShimmerBrush(isDark)
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
    ) {
        items(5) {
            Box(
                modifier = Modifier
                    .width(146.dp)
                    .height(240.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

@Composable
private fun PulsingLiveDot(
    color: Color = CyanAccent,
    modifier: Modifier = Modifier
) {
    // Use a slow 2.4s cycle (vs 0.9s) — cuts GPU wake frequency by 2.7×.
    // The dot still visually pulses but at a pace that lets the display idle between frames.
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_dot_transition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .size(6.dp)
            .graphicsLayer { this.alpha = alpha }
            .clip(CircleShape)
            .background(color)
    )
}


@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cyberpunk HUD Icon container with neon border
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.16f))
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(accentColor, accentColor.copy(alpha = 0.35f))),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    letterSpacing = (-0.2).sp
                )
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = colors.textSecondary
                )
            }
        }
        if (trailingContent != null) {
            trailingContent()
        }
    }
}

@Composable
private fun GlassCircularButton(
    icon: ImageVector,
    contentDescription: String,
    isDark: Boolean,
    size: Dp = 40.dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = tween(100),
        label = "btn_scale"
    )

    val rim = Brush.linearGradient(
        listOf(
            NeonCyan.copy(alpha = if (isDark) 0.65f else 0.35f),
            NeonMagenta.copy(alpha = if (isDark) 0.45f else 0.20f)
        )
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(if (isDark) Color(0xD00D1020) else Color(0xCCFFFFFF))
            .border(BorderStroke(1.dp, rim), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isDark) NeonCyan else Color(0xFF141624),
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Cinematic Full-Bleed Hero Card (with Age Rating & Library Status Sync)
 */
@Composable
private fun CinematicHeroCard(
    media: EnrichedMediaEntity,
    libraryMatch: EnrichedMediaEntity?,
    isSaved: Boolean,
    isDark: Boolean,
    onCardClick: () -> Unit = {},
    onAddToLibrary: () -> Unit,
    onWatching: () -> Unit,
    onMarkWatchedOrRead: () -> Unit,
    onToggleLibrary: () -> Unit
) {
    val context = LocalContext.current
    val isReadType = media.mediaType.equals("Manga", true) ||
            media.mediaType.equals("Manhwa", true)

    val currentStatus = libraryMatch?.userStatus?.uppercase()
    val isCompleted = currentStatus == "COMPLETED"

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "hero_scale"
    )

    val auraColor = when (media.mediaType.lowercase()) {
        "anime" -> CyanAccent
        "manga" -> GreenAccent
        "manhwa" -> Color(0xFF32D74B)
        "movie" -> BlueAccent
        else -> PurpleAccent
    }

    Box(
        modifier = Modifier
            .width(286.dp)
            .height(348.dp)
    ) {
        // Ambient soft backdrop glow drawn on canvas (zero extra composables or overdraw)
        if (isDark) {
            val auraBrush = remember(auraColor) {
                Brush.radialGradient(
                    colors = listOf(auraColor.copy(alpha = 0.16f), Color.Transparent)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .drawBehind {
                        drawCircle(brush = auraBrush, radius = size.width * 0.75f, center = center)
                    }
            )
        }

        Surface(
            shape = RoundedCornerShape(26.dp),
            color = if (isDark) GlassDark else GlassLight,
            border = BorderStroke(1.dp, HeroRimBrush),
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onCardClick
                )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Full-bleed Image Background
                val url = media.bannerUrl ?: media.posterUrl
                if (!url.isNullOrBlank()) {
                    val req = remember(url) {
                        ImageRequest.Builder(context)
                            .data(url)
                            .size(572, 696)
                            .crossfade(180)
                            .allowHardware(true)
                            .allowRgb565(true)
                            .build()
                    }
                    AsyncImage(
                        model = req,
                        contentDescription = media.cleanTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Cinematic Scrim Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(HeroScrimBrush)
                )

                // Top Badges Row: Media Type, Age Rating, Library Status & Rating Star
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Media Type Glass Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.72f))
                                .border(
                                    0.75.dp,
                                    auraColor.copy(alpha = 0.7f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = media.mediaType.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = auraColor,
                                letterSpacing = 0.6.sp
                            )
                        }

                        // Age Rating Badge (18+, 16+, 13+, PG-13, etc.)
                        if (!media.ageRating.isNullOrBlank()) {
                            AgeRatingBadge(rating = media.ageRating, fontSize = 9.sp)
                        }

                        // Library Status Badge at top
                        if (libraryMatch != null) {
                            val (sColor, sText) = when (currentStatus) {
                                "CURRENT" -> CyanAccent to if (isReadType) "Reading" else "Watching"
                                "COMPLETED" -> GreenAccent to "Done"
                                "DROPPED" -> Color(0xFFFF453A) to "Dropped"
                                else -> PurpleAccent to "Plan"
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.78f))
                                    .border(0.7.dp, sColor.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(text = sText, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = sColor)
                            }
                        }
                    }

                    // Star Rating Glass Pill
                    media.rating?.let { score ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.72f))
                                .border(
                                    0.7.dp,
                                    GoldAccent.copy(alpha = 0.55f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "%.1f".format(score),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Bottom Content on Scrim
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    // Full Title
                    Text(
                        text = media.cleanTitle,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(3.dp))

                    // Logical metadata text
                    val metaText = when {
                        isCompleted -> {
                            val count = if (isReadType) (libraryMatch?.chapter ?: media.chapter) else (libraryMatch?.episode ?: media.episode)
                            if (count != null && count > 0) "Completed • All $count ${if (isReadType) "chapters" else "episodes"}"
                            else "Completed • ${media.mediaType}"
                        }
                        currentStatus == "CURRENT" -> {
                            val ep = if (isReadType) libraryMatch?.chapter else libraryMatch?.episode
                            val total = if (isReadType) media.chapter else media.episode
                            if (ep != null && total != null && total > 0) "Watching: ${if (isReadType) "Ch" else "Ep"} $ep of $total"
                            else if (ep != null) "Watching: ${if (isReadType) "Ch" else "Ep"} $ep"
                            else "Watching • ${media.mediaType}"
                        }
                        currentStatus == "PLANNING" -> {
                            val total = if (isReadType) media.chapter else media.episode
                            if (total != null && total > 0) "Plan to ${if (isReadType) "Read" else "Watch"} • $total ${if (isReadType) "chapters" else "episodes"}"
                            else "Plan to ${if (isReadType) "Read" else "Watch"} • ${media.mediaType}"
                        }
                        else -> {
                            if (media.year != null) "${media.year} • ${media.mediaType}" else media.mediaType
                        }
                    }
                    Text(
                        text = metaText,
                        fontSize = 11.5.sp,
                        color = Color(0xFFB0B0C4),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = media.overview ?: "Personalized recommendation",
                        fontSize = 11.sp,
                        color = Color(0xFFC8C8DC),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp
                    )
                    Spacer(Modifier.height(13.dp))

                    // Action Pill: single button — Add to Library or status pill, both open popup
                    when {
                        !isSaved -> {
                            // Unsaved: "+ Add to Library" -> opens popup
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.horizontalGradient(listOf(CyanAccent, BlueAccent))
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onAddToLibrary
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color(0xFF07080E),
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = "Add to Library",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF07080E)
                                    )
                                }
                            }
                        }

                        else -> {
                            // Saved: single color-coded status pill -> tapping opens popup
                            val (pillColor, pillIcon, pillLabel) = when (currentStatus) {
                                "CURRENT" -> {
                                    val ep = if (isReadType) libraryMatch?.chapter else libraryMatch?.episode
                                    val label = if (ep != null) {
                                        if (isReadType) "Ch. $ep" else "Ep. $ep"
                                    } else {
                                        if (isReadType) "Reading" else "Watching"
                                    }
                                    Triple(CyanAccent, if (isReadType) Icons.Default.AutoStories else Icons.Default.PlayArrow, label)
                                }
                                "COMPLETED" ->
                                    Triple(GreenAccent, Icons.Default.Check, "Completed")
                                "DROPPED" ->
                                    Triple(Color(0xFFFF453A), Icons.Default.Check, "Dropped")
                                else -> // PLANNING or anything else
                                    Triple(PurpleAccent, Icons.Default.BookmarkAdded, if (isReadType) "Plan to Read" else "Plan to Watch")
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(pillColor.copy(alpha = 0.18f))
                                    .border(0.8.dp, pillColor.copy(alpha = 0.60f), RoundedCornerShape(14.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onAddToLibrary // Opens popup!
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        pillIcon,
                                        contentDescription = null,
                                        tint = pillColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = pillLabel,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = pillColor
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

/**
 * Compact Cards Row with Top 10 Ranks & Library Matching
 */
@Composable
private fun CompactCardRow(
    items: List<EnrichedMediaEntity>,
    libraryLookup: (EnrichedMediaEntity) -> EnrichedMediaEntity?,
    isDark: Boolean,
    onCardClick: (EnrichedMediaEntity) -> Unit = {},
    onAddToLibrary: (EnrichedMediaEntity) -> Unit,
    onWatchingClick: (EnrichedMediaEntity) -> Unit,
    onMarkWatchedOrRead: (EnrichedMediaEntity) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
    ) {
        itemsIndexed(
            items,
            key = { _, it -> it.id },
            contentType = { _, _ -> "CompactGlassCard" }
        ) { index, item ->
            val libraryMatch = libraryLookup(item)
            val effectiveItem = libraryMatch ?: item
            val isSaved = libraryMatch != null

            CompactGlassCard(
                rank = index + 1,
                media = effectiveItem,
                libraryMatch = libraryMatch,
                isSaved = isSaved,
                isDark = isDark,
                onCardClick = { onCardClick(effectiveItem) },
                onAddToLibrary = { onAddToLibrary(effectiveItem) },
                onWatchingClick = { onWatchingClick(effectiveItem) },
                onMarkWatchedOrRead = { onMarkWatchedOrRead(effectiveItem) }
            )
        }
    }
}

/**
 * Refined Compact Glass Card with Full Title, Age Rating & Synced Status
 */
@Composable
private fun CompactGlassCard(
    rank: Int,
    media: EnrichedMediaEntity,
    libraryMatch: EnrichedMediaEntity?,
    isSaved: Boolean,
    isDark: Boolean,
    onCardClick: () -> Unit = {},
    onAddToLibrary: () -> Unit,
    onWatchingClick: () -> Unit,
    onMarkWatchedOrRead: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isReadType = media.mediaType.equals("Manga", true) ||
            media.mediaType.equals("Manhwa", true)

    val currentStatus = libraryMatch?.userStatus?.uppercase()
    val isCompleted = currentStatus == "COMPLETED"

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.955f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "compact_scale"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) GlassDark else GlassLight,
        border = BorderStroke(1.dp, CompactRimBrush),
        shadowElevation = 8.dp,
        modifier = Modifier
            .width(146.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onCardClick
            )
    ) {
        Column(modifier = Modifier.padding(9.dp)) {
            // Poster Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(194.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(0xFF161828))
            ) {
                if (!media.posterUrl.isNullOrBlank()) {
                    val req = remember(media.posterUrl) {
                        ImageRequest.Builder(context)
                            .data(media.posterUrl)
                            .size(292, 388)
                            .crossfade(140)
                            .allowHardware(true)
                            .allowRgb565(true)
                            .build()
                    }
                    AsyncImage(
                        model = req,
                        contentDescription = media.cleanTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Poster Gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CompactPosterScrim)
                )

                // Top-Left: Rank Badge
                val rankBg = when (rank) {
                    1 -> RankBrushGold
                    2 -> RankBrushSilver
                    3 -> RankBrushBronze
                    else -> RankBrushDefault
                }
                val rankTextColor = if (rank in 1..3) Color.Black else CyanAccent

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(rankBg)
                        .border(
                            0.6.dp,
                            if (rank in 1..3) Color.White.copy(alpha = 0.5f) else CyanAccent.copy(alpha = 0.5f),
                            RoundedCornerShape(7.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "#$rank",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = rankTextColor
                    )
                }

                // Top-Right: Library Status Badge if saved
                if (libraryMatch != null) {
                    val (sColor, sText) = when (currentStatus) {
                        "CURRENT" -> CyanAccent to if (isReadType) "Reading" else "Watching"
                        "COMPLETED" -> GreenAccent to "Done"
                        "DROPPED" -> Color(0xFFFF453A) to "Drop"
                        else -> PurpleAccent to "Plan"
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.82f))
                            .border(0.6.dp, sColor.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(text = sText, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = sColor)
                    }
                }

                // Bottom Row on Poster: Age Rating (Left) & Star Rating (Right)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!media.ageRating.isNullOrBlank()) {
                        AgeRatingBadge(rating = media.ageRating, fontSize = 8.sp)
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    media.rating?.let { score ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.82f))
                                .border(0.5.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(9.dp)
                            )
                            Text(
                                text = "%.1f".format(score),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Streaming-Style Watch Progress Indicator Bar at Bottom of Poster
                if (currentStatus == "CURRENT") {
                    val ep = if (isReadType) libraryMatch?.chapter else libraryMatch?.episode
                    val total = if (isReadType) media.chapter else media.episode
                    if (ep != null && total != null && total > 0) {
                        val progress = (ep.toFloat() / total.toFloat()).coerceIn(0.05f, 1f)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress)
                                    .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                                    .background(
                                        Brush.horizontalGradient(listOf(CyanAccent, BlueAccent))
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Multi-line Title
            Text(
                text = media.cleanTitle,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color.White else Color(0xFF0D0F1A),
                maxLines = 2,
                minLines = 2,
                lineHeight = 16.sp,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = (-0.1).sp
            )

            Spacer(Modifier.height(3.dp))

            // Subtitle: Displays total count or category, avoiding repeating button text!
            val sub = when {
                isCompleted -> {
                    val count = if (isReadType) (libraryMatch?.chapter ?: media.chapter) else (libraryMatch?.episode ?: media.episode)
                    if (count != null && count > 0) "All $count ${if (isReadType) "chapters" else "episodes"}" else "Completed"
                }
                currentStatus == "CURRENT" -> {
                    val ep = if (isReadType) libraryMatch?.chapter else libraryMatch?.episode
                    val total = if (isReadType) media.chapter else media.episode
                    if (ep != null && total != null && total > 0) "${if (isReadType) "Ch" else "Ep"} $ep of $total"
                    else if (ep != null) "At ${if (isReadType) "Ch" else "Ep"} $ep"
                    else "Watching"
                }
                media.episode != null && media.episode > 0 -> "${media.episode} Episodes"
                media.chapter != null && media.chapter > 0 -> "${media.chapter} Chapters"
                media.year != null -> "${media.year} • ${media.mediaType}"
                else -> media.mediaType
            }
            Text(
                text = sub,
                fontSize = 10.5.sp,
                color = if (isDark) Color(0xFF8E8EA0) else Color(0xFF636375),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            // Action Pill: single button — Add to Library or status pill, both open popup
            when {
                !isSaved -> {
                    // Unsaved: "+ Add to Library" -> opens popup
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.horizontalGradient(listOf(CyanAccent, BlueAccent))
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onAddToLibrary
                            )
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color(0xFF07080E),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Add to Library",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF07080E)
                            )
                        }
                    }
                }

                else -> {
                    // Saved: single color-coded status pill -> tapping opens popup
                    val (pillColor, pillIcon, pillLabel) = when (currentStatus) {
                        "CURRENT" -> {
                            val ep = if (isReadType) libraryMatch?.chapter else libraryMatch?.episode
                            val label = if (ep != null) {
                                if (isReadType) "Ch. $ep" else "Ep. $ep"
                            } else {
                                if (isReadType) "Reading" else "Watching"
                            }
                            Triple(CyanAccent, if (isReadType) Icons.Default.AutoStories else Icons.Default.PlayArrow, label)
                        }
                        "COMPLETED" ->
                            Triple(GreenAccent, Icons.Default.Check, "Completed")
                        "DROPPED" ->
                            Triple(Color(0xFFFF453A), Icons.Default.Check, "Dropped")
                        else -> // PLANNING or anything else
                            Triple(PurpleAccent, Icons.Default.BookmarkAdded, if (isReadType) "Plan to Read" else "Plan to Watch")
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(pillColor.copy(alpha = 0.16f))
                            .border(0.75.dp, pillColor.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onWatchingClick   // opens popup
                            )
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                pillIcon,
                                contentDescription = null,
                                tint = pillColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = pillLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = pillColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getMediaTypeColor(type: String): Color = when (type.lowercase()) {
    "anime" -> CyanAccent
    "manga" -> GreenAccent
    "manhwa" -> OrangeAccent
    "movie" -> PurpleAccent
    "tv", "series" -> BlueAccent
    else -> CyanAccent
}

/**
 * Rich Library Modal Detail & Status Dialog for HomeScreen (Popup)
 */
@Composable
private fun HomeMediaDetailDialog(
    media: EnrichedMediaEntity,
    isSaved: Boolean,
    onDismiss: () -> Unit,
    onStatusChange: (String) -> Unit,
    onTrackProgress: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val isReadType = media.mediaType.equals("Manga", true) ||
            media.mediaType.equals("Manhwa", true)
    val isEpisodic = media.mediaType.equals("Anime", true) ||
            media.mediaType.equals("Manga", true) ||
            media.mediaType.equals("Manhwa", true) ||
            media.mediaType.contains("TV", true) ||
            media.mediaType.contains("Series", true)

    val rim = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.28f),
            Color.White.copy(alpha = 0.06f),
            Color.Transparent
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xF2121524),
            border = BorderStroke(1.dp, rim),
            shadowElevation = 20.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with Poster thumbnail, details, and dedicated Dismiss button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 66.dp, height = 96.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF161828))
                            .border(0.75.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    ) {
                        if (!media.posterUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = media.posterUrl,
                                contentDescription = media.cleanTitle,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(media.mediaType.take(1), fontSize = 28.sp, color = Color(0xFF6B6E82))
                            }
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        val typeColor = getMediaTypeColor(media.mediaType)
                        Text(
                            media.mediaType.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = typeColor,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            media.cleanTitle,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            letterSpacing = (-0.2).sp
                        )
                        Spacer(Modifier.height(5.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            media.year?.let {
                                Text("$it", fontSize = 12.sp, color = Color(0xFFA0A3B8))
                            }
                            media.rating?.let { score ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, null, tint = GoldAccent, modifier = Modifier.size(11.dp))
                                    Text(" ${"%.1f".format(score)}", fontSize = 12.sp, color = GoldAccent, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (!media.ageRating.isNullOrBlank()) {
                                AgeRatingBadge(rating = media.ageRating, fontSize = 9.sp)
                            }
                            Text("• ${media.sourceService}", fontSize = 11.sp, color = Color(0xFF6B6E82))
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFA0A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Stats chips: episodes, chapters, year, source
                val isMangaTypeHome = media.mediaType.equals("Manga", true) || media.mediaType.equals("Manhwa", true)
                val hasHomeStats = media.episode != null || media.chapter != null || media.year != null
                if (hasHomeStats) {
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        media.episode?.takeIf { it > 0 }?.let { epCount ->
                            item {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BlueAccent.copy(alpha = 0.14f),
                                    border = BorderStroke(0.75.dp, BlueAccent.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, null, tint = BlueAccent, modifier = Modifier.size(10.dp))
                                        Text("$epCount Episodes", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = BlueAccent)
                                    }
                                }
                            }
                        }
                        media.chapter?.takeIf { it > 0 }?.let { chCount ->
                            item {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = GreenAccent.copy(alpha = 0.14f),
                                    border = BorderStroke(0.75.dp, GreenAccent.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.AutoStories, null, tint = GreenAccent, modifier = Modifier.size(10.dp))
                                        Text("$chCount Chapters", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = GreenAccent)
                                    }
                                }
                            }
                        }
                        media.year?.let { yr ->
                            item {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PurpleAccent.copy(alpha = 0.14f),
                                    border = BorderStroke(0.75.dp, PurpleAccent.copy(alpha = 0.45f))
                                ) {
                                    Text("$yr", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = PurpleAccent,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp))
                                }
                            }
                        }
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF6B6E82).copy(alpha = 0.14f),
                                border = BorderStroke(0.75.dp, Color(0xFF6B6E82).copy(alpha = 0.30f))
                            ) {
                                Text(
                                    media.sourceService,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF9B9EB4),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Expandable synopsis card
                media.overview?.takeIf { it.isNotBlank() }?.let { overview ->
                    var synopsisExpanded by remember { mutableStateOf(false) }
                    Spacer(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x14FFFFFF))
                            .border(0.5.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                            .clickable { synopsisExpanded = !synopsisExpanded }
                            .padding(10.dp)
                    ) {
                        Text(
                            "SYNOPSIS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B6E82),
                            letterSpacing = 0.7.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            overview,
                            fontSize = 12.sp,
                            color = Color(0xFFB0B3C8),
                            maxLines = if (synopsisExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 17.sp
                        )
                        if (!synopsisExpanded && overview.length > 120) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Read more \u25be",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyanAccent
                            )
                        } else if (synopsisExpanded) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Show less \u25b4",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyanAccent
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "LIBRARY STATUS",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B6E82),
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(8.dp))

                val statusOptions = listOf(
                    Triple("CURRENT", if (isReadType) "Reading" else "Watching", CyanAccent),
                    Triple("PLANNING", if (isReadType) "Plan to Read" else "Plan to Watch", PurpleAccent),
                    Triple("COMPLETED", "Completed", GreenAccent),
                    Triple("DROPPED", "Dropped", Color(0xFFFF453A))
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    statusOptions.forEach { (code, label, color) ->
                        val isSelected = isSaved && media.userStatus.equals(code, ignoreCase = true)
                        val rowBg by animateColorAsState(
                            targetValue = if (isSelected) color.copy(alpha = 0.20f) else Color(0x33000000),
                            animationSpec = tween(180),
                            label = "status_bg"
                        )
                        val rowBorder by animateColorAsState(
                            targetValue = if (isSelected) color else Color.White.copy(alpha = 0.12f),
                            animationSpec = tween(180),
                            label = "status_border"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(rowBg)
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.75.dp,
                                    color = rowBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onStatusChange(code)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        label,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFFD0D3E2)
                                    )
                                    val subText = when (code) {
                                        "CURRENT" -> if (isReadType) "Currently reading chapter" else "Currently watching episode"
                                        "PLANNING" -> "Saved to your watchlist"
                                        "COMPLETED" -> if (isReadType) "All chapters finished" else "All episodes finished"
                                        else -> "Stopped watching / reading"
                                    }
                                    Text(subText, fontSize = 10.5.sp, color = Color(0xFF7E8299))
                                }
                            }
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn(tween(140)) + scaleIn(tween(140)),
                                exit = fadeOut(tween(100)) + scaleOut(tween(100))
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = color,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }

                // Interactive Progress Tracker Row
                if (isEpisodic) {
                    val currentVal = if (isReadType) media.chapter ?: 0 else media.episode ?: 0
                    val unitName = if (isReadType) "Chapter" else "Episode"

                    Spacer(Modifier.height(10.dp))
                    Surface(
                        onClick = onTrackProgress,
                        shape = RoundedCornerShape(12.dp),
                        color = CyanAccent.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "EPISODE / CHAPTER PROGRESS",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent,
                                    letterSpacing = 0.7.sp
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (currentVal > 0) "$unitName $currentVal" else "Not started yet",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CyanAccent,
                                modifier = Modifier.height(28.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Text(
                                        "Track Progress",
                                        color = Color(0xFF07080E),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Action buttons: Open in Web & Remove from Library
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (media.directWebUrl != null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(11.dp))
                                .background(BlueAccent.copy(alpha = 0.15f))
                                .border(1.dp, BlueAccent.copy(alpha = 0.4f), RoundedCornerShape(11.dp))
                                .clickable { onOpen() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = BlueAccent, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Open Web", color = BlueAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isSaved) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color(0xFFFF453A).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFFFF453A).copy(alpha = 0.4f), RoundedCornerShape(11.dp))
                                .clickable { onDelete() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, null, tint = Color(0xFFFF453A), modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Remove", color = Color(0xFFFF453A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
