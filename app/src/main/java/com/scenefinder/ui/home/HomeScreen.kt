package com.scenefinder.ui.home

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.scenefinder.ui.components.ProgressTrackingDialog
import com.scenefinder.ui.theme.LocalAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Apple Liquid Glass Design System
private val GlassDark = Color(0xB8131627)
private val GlassLight = Color(0xEEFFFFFF)
private val CyanAccent = Color(0xFF00F2FE)
private val BlueAccent = Color(0xFF0A84FF)
private val PurpleAccent = Color(0xFFBF5AF2)
private val GoldAccent = Color(0xFFFFD60A)
private val GreenAccent = Color(0xFF30D158)
private val OrangeAccent = Color(0xFFFF9F0A)
private val DarkBg = Color(0xFF07080E)

@Composable
fun HomeScreen(
    dao: EnrichedMediaDao,
    repository: MediaEnrichmentRepository,
    settings: AppSettings,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
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

    // Fast O(1) matching between feed items and user's saved library
    val libraryById = remember(userLibrary) { userLibrary.associateBy { it.id } }
    val libraryByTitle = remember(userLibrary) {
        userLibrary.associateBy { it.cleanTitle.trim().lowercase() }
    }

    fun findMatchingLibraryEntry(media: EnrichedMediaEntity): EnrichedMediaEntity? {
        return libraryById[media.id] ?: libraryByTitle[media.cleanTitle.trim().lowercase()]
    }

    var recommendations by remember { mutableStateOf<List<EnrichedMediaEntity>>(emptyList()) }
    var topAnime by remember { mutableStateOf<List<EnrichedMediaEntity>>(emptyList()) }
    var topManga by remember { mutableStateOf<List<EnrichedMediaEntity>>(emptyList()) }
    var topManhwa by remember { mutableStateOf<List<EnrichedMediaEntity>>(emptyList()) }
    var topMovies by remember { mutableStateOf<List<EnrichedMediaEntity>>(emptyList()) }
    var topTv by remember { mutableStateOf<List<EnrichedMediaEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var trackingDialogMedia by remember { mutableStateOf<EnrichedMediaEntity?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val recs = repository.getHomeRecommendations(userLibrary)
            val anime = repository.getTopAnime()
            val manga = repository.getTopManga()
            val manhwa = repository.getTopManhwa()
            val movies = repository.getTopMovies(apiKey)
            val tv = repository.getTopTvSeries(apiKey)
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
    }

    // Safe Top Bar calculation to guarantee no status bar overlap on any phone
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val safeTopPadding = maxOf(statusBarTop, 44.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (colors.isDark) DarkBg else Color(0xFFF2F2F7))
    ) {
        // Ambient Liquid Glass Glows (giving real refraction & depth to cards)
        if (colors.isDark) {
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(CyanAccent.copy(alpha = 0.12f), Color.Transparent),
                            radius = 450f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(360.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(PurpleAccent.copy(alpha = 0.08f), Color.Transparent),
                            radius = 500f
                        )
                    )
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Header Top Bar with Safe Insets
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "SceneFinder",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.textPrimary,
                                letterSpacing = (-0.6).sp
                            )
                            // Apple-style Frosted Glass Pill Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(CyanAccent.copy(alpha = 0.14f))
                                    .border(
                                        0.75.dp,
                                        CyanAccent.copy(alpha = 0.45f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 9.dp, vertical = 3.5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(CyanAccent)
                                    )
                                    Text(
                                        text = "AI FEED",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CyanAccent,
                                        letterSpacing = 0.6.sp
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(2.dp))
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
                        GlassCircularButton(
                            icon = Icons.Default.Search,
                            contentDescription = "Search",
                            isDark = colors.isDark
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNavigateToSearch()
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

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(
                                color = CyanAccent,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = "Curating your personalized feed\u2026",
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            } else {
                // Section 1: Hero Carousel (Recommendations)
                if (recommendations.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Recommended For You",
                            subtitle = "Personalized from trending anime, manga & movies",
                            accentColor = CyanAccent,
                            icon = Icons.Default.AutoAwesome
                        )
                    }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            items(recommendations, key = { it.id }) { item ->
                                val libraryEntry = findMatchingLibraryEntry(item)
                                val effectiveItem = libraryEntry ?: item
                                val isSaved = libraryEntry != null

                                CinematicHeroCard(
                                    media = effectiveItem,
                                    libraryMatch = libraryEntry,
                                    isSaved = isSaved,
                                    isDark = colors.isDark,
                                    onWatching = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        trackingDialogMedia = effectiveItem
                                    },
                                    onToggleLibrary = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        scope.launch(Dispatchers.IO) {
                                            if (isSaved) {
                                                repository.removeFromLibrary(
                                                    effectiveItem,
                                                    anilistToken,
                                                    apiKey,
                                                    tmdbSessionId,
                                                    tmdbAccountId
                                                )
                                            } else {
                                                repository.saveToLibrary(
                                                    effectiveItem.copy(userStatus = "PLANNING"),
                                                    anilistToken,
                                                    apiKey,
                                                    tmdbSessionId,
                                                    tmdbAccountId
                                                )
                                            }
                                        }
                                    }
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
                            onWatchingClick = { trackingDialogMedia = it }
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
                            onWatchingClick = { trackingDialogMedia = it }
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
                            onWatchingClick = { trackingDialogMedia = it }
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
                            onWatchingClick = { trackingDialogMedia = it }
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
                            onWatchingClick = { trackingDialogMedia = it }
                        )
                    }
                }
            }
        }

        // Episode & Chapter Progress Tracking Dialog
        trackingDialogMedia?.let { media ->
            ProgressTrackingDialog(
                media = media,
                onDismiss = { trackingDialogMedia = null },
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
                        repository.updateMediaProgress(m, "CURRENT", ep, ch, anilistToken)
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
    }
}

// -------------------------------------------------------------
// UI COMPONENTS (Apple Liquid Glass System)
// -------------------------------------------------------------

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Glass Icon container
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.16f))
                    .border(
                        0.75.dp,
                        accentColor.copy(alpha = 0.45f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(17.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = colors.textSecondary
                )
            }
        }

        // Apple-style "Featured" badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(0.6.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                .padding(horizontal = 9.dp, vertical = 3.dp)
        ) {
            Text(
                text = "FEATURED",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
                letterSpacing = 0.5.sp
            )
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

    val rim = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.28f),
            Color.White.copy(alpha = 0.06f)
        )
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(if (isDark) Color(0x33FFFFFF) else Color(0xCCFFFFFF))
            .border(BorderStroke(0.8.dp, rim), CircleShape)
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
            tint = if (isDark) Color.White else Color(0xFF141624),
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
    onWatching: () -> Unit,
    onToggleLibrary: () -> Unit
) {
    val context = LocalContext.current
    val isReadType = media.mediaType.equals("Manga", true) ||
            media.mediaType.equals("Manhwa", true)

    val rim = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.35f),
            Color.White.copy(alpha = 0.08f),
            Color.Transparent
        )
    )

    val scrim = Brush.verticalGradient(
        listOf(
            Color.Black.copy(alpha = 0.35f),
            Color.Transparent,
            Color.Black.copy(alpha = 0.70f),
            Color.Black.copy(alpha = 0.95f)
        )
    )

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = if (isDark) GlassDark else GlassLight,
        border = BorderStroke(1.dp, rim),
        shadowElevation = 14.dp,
        modifier = Modifier
            .width(280.dp)
            .height(340.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Full-bleed Image Background
            val url = media.bannerUrl ?: media.posterUrl
            if (!url.isNullOrBlank()) {
                val req = remember(url) {
                    ImageRequest.Builder(context)
                        .data(url)
                        .size(560, 680)
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
                    .background(scrim)
            )

            // Top Badges: Media Type, Age Rating Badge & Rating Star
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
                            .background(Color.Black.copy(alpha = 0.70f))
                            .border(
                                0.7.dp,
                                CyanAccent.copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = media.mediaType.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyanAccent,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Age Rating Badge (18+, 16+, 13+, PG-13, etc.)
                    if (!media.ageRating.isNullOrBlank()) {
                        AgeRatingBadge(rating = media.ageRating, fontSize = 9.sp)
                    }
                }

                // Star Rating Glass Pill
                media.rating?.let { score ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.70f))
                            .border(
                                0.7.dp,
                                GoldAccent.copy(alpha = 0.5f),
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
                // Title and Synced Library Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = media.cleanTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = (-0.2).sp,
                        modifier = Modifier.weight(1f)
                    )
                    // Library Status Badge if in user's library
                    if (libraryMatch != null) {
                        Spacer(Modifier.width(6.dp))
                        val status = libraryMatch.userStatus.uppercase()
                        val (sColor, sText) = when (status) {
                            "CURRENT" -> CyanAccent to if (isReadType) "Reading" else "Watching"
                            "COMPLETED" -> GreenAccent to "Completed"
                            "DROPPED" -> Color(0xFFFF453A) to "Dropped"
                            else -> PurpleAccent to "Planning"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(sColor.copy(alpha = 0.22f))
                                .border(0.7.dp, sColor.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = sText, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = sColor)
                        }
                    }
                }

                Spacer(Modifier.height(3.dp))
                val metaText = when {
                    libraryMatch?.episode != null -> "Progress: Ep ${libraryMatch.episode} • ${media.mediaType}"
                    libraryMatch?.chapter != null -> "Progress: Ch ${libraryMatch.chapter} • ${media.mediaType}"
                    media.year != null -> "${media.year} • ${media.mediaType}"
                    else -> media.mediaType
                }
                Text(
                    text = metaText,
                    fontSize = 11.5.sp,
                    color = Color(0xFFB0B0C4),
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = media.overview ?: "Matched by personalized recommendation algorithm",
                    fontSize = 11.sp,
                    color = Color(0xFFC8C8DC),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
                Spacer(Modifier.height(13.dp))

                // Actions: Synced Watching Pill + Glass Bookmark
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentStatus = libraryMatch?.userStatus?.uppercase()
                    val buttonText = when {
                        currentStatus == "CURRENT" -> {
                            val ep = if (isReadType) libraryMatch?.chapter else libraryMatch?.episode
                            if (ep != null) (if (isReadType) "Ch. $ep" else "Ep. $ep") else (if (isReadType) "Reading" else "Watching")
                        }
                        currentStatus == "COMPLETED" -> "Completed ✓"
                        currentStatus == "PLANNING" -> "Plan to Watch"
                        else -> if (isReadType) "Read" else "Watch"
                    }

                    // Apple VisionOS-style Frosted Action Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    if (currentStatus == "COMPLETED") listOf(GreenAccent, Color(0xFF248A3D))
                                    else listOf(CyanAccent, BlueAccent)
                                )
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onWatching
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                if (currentStatus == "COMPLETED") Icons.Default.Check else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (currentStatus == "COMPLETED") Color.White else Color(0xFF07080E),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = buttonText,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentStatus == "COMPLETED") Color.White else Color(0xFF07080E)
                            )
                        }
                    }

                    // Glass Bookmark Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x55000000))
                            .border(
                                0.75.dp,
                                if (isSaved) GreenAccent.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.25f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onToggleLibrary
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                            contentDescription = if (isSaved) "Saved" else "Save",
                            tint = if (isSaved) GreenAccent else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
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
    onWatchingClick: (EnrichedMediaEntity) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
    ) {
        itemsIndexed(items, key = { _, it -> it.id }) { index, item ->
            val libraryMatch = libraryLookup(item)
            val effectiveItem = libraryMatch ?: item
            val isSaved = libraryMatch != null

            CompactGlassCard(
                rank = index + 1,
                media = effectiveItem,
                libraryMatch = libraryMatch,
                isSaved = isSaved,
                isDark = isDark,
                onWatchingClick = { onWatchingClick(effectiveItem) }
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
    onWatchingClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isReadType = media.mediaType.equals("Manga", true) ||
            media.mediaType.equals("Manhwa", true)

    val rim = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.30f),
            Color.White.copy(alpha = 0.05f),
            Color.Transparent
        )
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) GlassDark else GlassLight,
        border = BorderStroke(1.dp, rim),
        shadowElevation = 8.dp,
        modifier = Modifier
            .width(146.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onWatchingClick()
            }
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
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.70f)
                                )
                            )
                        )
                )

                // Top-Left Badges: Rank Badge & Age Rating Badge
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val rankBg = when (rank) {
                        1 -> Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))
                        2 -> Brush.horizontalGradient(listOf(Color(0xFFE0E0E0), Color(0xFFB0B0B0)))
                        3 -> Brush.horizontalGradient(listOf(Color(0xFFCD7F32), Color(0xFF8B4513)))
                        else -> Brush.horizontalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.82f),
                                Color.Black.copy(alpha = 0.82f)
                            )
                        )
                    }
                    val rankTextColor = if (rank in 1..3) Color.Black else CyanAccent

                    Box(
                        modifier = Modifier
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

                    // Age Rating Badge on Poster
                    if (!media.ageRating.isNullOrBlank()) {
                        AgeRatingBadge(rating = media.ageRating, fontSize = 8.sp)
                    }
                }

                // Top-Right: Library Status Badge if saved
                if (libraryMatch != null) {
                    val status = libraryMatch.userStatus.uppercase()
                    val (sColor, sText) = when (status) {
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
                            .background(Color.Black.copy(alpha = 0.80f))
                            .border(0.6.dp, sColor.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(text = sText, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = sColor)
                    }
                }

                // Star Rating
                media.rating?.let { score ->
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.78f))
                            .padding(horizontal = 5.dp, vertical = 2.5.dp),
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

            Spacer(Modifier.height(8.dp))

            // Multi-line Title with Breathing Room (Prevents abrupt truncation)
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

            val sub = when {
                libraryMatch?.episode != null -> "Ep. ${libraryMatch.episode}"
                libraryMatch?.chapter != null -> "Ch. ${libraryMatch.chapter}"
                media.episode != null -> "${media.episode} Episodes"
                media.chapter != null -> "${media.chapter} Chapters"
                media.year != null -> "${media.year}"
                else -> media.mediaType
            }
            Text(
                text = sub,
                fontSize = 10.5.sp,
                color = if (isDark) Color(0xFF8E8EA0) else Color(0xFF636375)
            )

            Spacer(Modifier.height(8.dp))

            // Synced iOS Glass Action Pill
            val currentStatus = libraryMatch?.userStatus?.uppercase()
            val buttonText = when {
                currentStatus == "CURRENT" -> {
                    val ep = if (isReadType) libraryMatch?.chapter else libraryMatch?.episode
                    if (ep != null) (if (isReadType) "Ch. $ep" else "Ep. $ep") else (if (isReadType) "Reading" else "Watching")
                }
                currentStatus == "COMPLETED" -> "Done ✓"
                currentStatus == "PLANNING" -> "Plan"
                else -> if (isReadType) "Read" else "Watch"
            }
            val buttonColor = if (currentStatus == "COMPLETED") GreenAccent else CyanAccent

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(buttonColor.copy(alpha = 0.16f))
                    .border(
                        0.75.dp,
                        buttonColor.copy(alpha = 0.45f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onWatchingClick()
                    }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (currentStatus == "COMPLETED") Icons.Default.Check else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = buttonColor,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = buttonText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = buttonColor
                    )
                }
            }
        }
    }
}
