package com.scenefinder.ui.overlay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.scenefinder.data.local.dao.EnrichedMediaDao
import com.scenefinder.data.local.entity.EnrichedMediaEntity
import com.scenefinder.data.prefs.AppSettings
import com.scenefinder.data.repository.MediaEnrichmentRepository
import com.scenefinder.domain.model.MediaType
import com.scenefinder.domain.parser.CandidateExtractor
import com.scenefinder.ui.components.AgeRatingBadge
import com.scenefinder.ui.theme.DarkSurface
import com.scenefinder.ui.theme.DarkSurfaceVariant
import com.scenefinder.ui.theme.NeonAccent
import com.scenefinder.ui.theme.NeonGreen
import com.scenefinder.ui.theme.NeonOrange
import com.scenefinder.ui.theme.SceneFinderTheme
import com.scenefinder.ui.theme.LocalAppColors
import com.scenefinder.data.prefs.ThemeMode
import com.scenefinder.ui.theme.TextSecondary
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Merged SceneFinder search & detail popup.
 *
 * Triggered from:
 * 1) Android text selection menu (ACTION_PROCESS_TEXT)
 * 2) Direct Share sheet (ACTION_SEND)
 * 3) Quick search / Accessibility overlay
 *
 * Features:
 * - Pre-filled, live-editable search bar with the selected text
 * - One-click re-search right inside the popup
 * - Multi-format search: searches Anime, Manga, TV Shows, and Movies concurrently!
 * - Displays related adaptations (e.g. Manga and Netflix TV show for "One Piece")
 * - One-tap switching between related formats
 * - Built-in Watchlist / Library management (Watching, Plan to Watch, Completed, Dropped)
 * - Direct external web links (AniList / TMDB)
 */
@AndroidEntryPoint
class TextSearchActivity : ComponentActivity() {

    @Inject lateinit var repository: MediaEnrichmentRepository
    @Inject lateinit var settings: AppSettings
    @Inject lateinit var candidateExtractor: CandidateExtractor
    @Inject lateinit var dao: EnrichedMediaDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rawQuery = when (intent?.action) {
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
                ?: intent?.getStringExtra(Intent.EXTRA_TEXT)
        }?.trim().orEmpty()

        setContent {
            val themeMode by settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            SceneFinderTheme(themeMode = themeMode) {
                TextSearchPopup(
                    initialQuery = rawQuery,
                    repository = repository,
                    settings = settings,
                    candidateExtractor = candidateExtractor,
                    dao = dao,
                    onDismiss = { finish() },
                    onOpenWeb = { url ->
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(browserIntent)
                        finish()
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// State
// ─────────────────────────────────────────────────────────────────────────────

private sealed class SearchState {
    object Idle : SearchState()
    data class Loading(val query: String) : SearchState()
    data class Found(
        val selected: EnrichedMediaEntity,
        val allResults: List<EnrichedMediaEntity>
    ) : SearchState()
    data class NotFound(val query: String) : SearchState()
    data class Error(val message: String) : SearchState()
}

// ─────────────────────────────────────────────────────────────────────────────
// Root composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TextSearchPopup(
    initialQuery: String,
    repository: MediaEnrichmentRepository,
    settings: AppSettings,
    candidateExtractor: CandidateExtractor,
    dao: EnrichedMediaDao,
    onDismiss: () -> Unit,
    onOpenWeb: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf(initialQuery) }
    var activeQuery by remember { mutableStateOf(initialQuery) }
    var state by remember { mutableStateOf<SearchState>(if (initialQuery.isNotBlank()) SearchState.Loading(initialQuery) else SearchState.Idle) }

    val anilistToken by settings.anilistToken.collectAsState()
    val apiKey by settings.tmdbApiKey.collectAsState()
    val tmdbSessionId by settings.tmdbSessionId.collectAsState()
    val tmdbAccountId by settings.tmdbAccountId.collectAsState()

    var isSaved by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var currentMediaForStatus by remember { mutableStateOf<EnrichedMediaEntity?>(null) }

    val colors = LocalAppColors.current

    // Execute search across Anime, Manga, TV shows, and Movies
    LaunchedEffect(activeQuery) {
        val q = activeQuery.trim()
        if (q.isBlank()) {
            state = SearchState.Idle
            return@LaunchedEffect
        }
        state = SearchState.Loading(q)
        withContext(Dispatchers.IO) {
            try {
                val effectiveKey = apiKey.ifBlank { MediaEnrichmentRepository.DEFAULT_TMDB_API_KEY }
                val candidates = candidateExtractor.extractCandidates(
                    listOf(q), "Text Selection Search"
                )
                val allMatches = mutableListOf<EnrichedMediaEntity>()
                for (c in candidates) {
                    val related = repository.searchAllRelated(c, effectiveKey)
                    for (r in related) {
                        if (allMatches.none { it.id == r.id }) {
                            allMatches.add(r)
                        }
                    }
                }
                if (allMatches.isNotEmpty()) {
                    val top = allMatches.first()
                    val existing = dao.getById(top.id)
                    isSaved = existing != null
                    state = SearchState.Found(selected = top, allResults = allMatches)
                } else {
                    state = SearchState.NotFound(q)
                }
            } catch (e: Exception) {
                state = SearchState.Error(e.message ?: "Unknown error")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .imePadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn()
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        if (colors.isDark) {
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF1E1E34), Color(0xFF141728))
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFFFFFFF), Color(0xFFF3F4F6))
                            )
                        }
                    )
                    .border(
                        1.dp,
                        if (colors.isDark) Color(0xFF333A56) else Color(0xFFE5E7EB),
                        RoundedCornerShape(26.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .heightIn(max = 640.dp)
                ) {
                    // Header Row
                    PopupHeader(onDismiss = onDismiss)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Editable Search Bar with Selected Text
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = {
                            if (searchQuery.trim().isNotBlank()) {
                                focusManager.clearFocus()
                                activeQuery = searchQuery.trim()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Content Area (Loading, Results, Not Found, Error)
                    AnimatedContent(
                        targetState = state,
                        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(180)) },
                        label = "search_state"
                    ) { s ->
                        when (s) {
                            is SearchState.Idle -> IdleCard()
                            is SearchState.Loading -> SearchingIndicator(s.query)
                            is SearchState.Found -> MergedResultCard(
                                media = s.selected,
                                allResults = s.allResults,
                                isSaved = isSaved,
                                onSelectMedia = { chosen ->
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            val existing = dao.getById(chosen.id)
                                            isSaved = existing != null
                                        }
                                        val currentFound = state as? SearchState.Found
                                        if (currentFound != null) {
                                            state = currentFound.copy(selected = chosen)
                                        }
                                    }
                                },
                                onToggleWatchlist = {
                                    if (isSaved) {
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                repository.removeFromLibrary(
                                                    media = s.selected,
                                                    anilistToken = anilistToken,
                                                    tmdbApiKey = apiKey,
                                                    tmdbSessionId = tmdbSessionId,
                                                    tmdbAccountId = tmdbAccountId
                                                )
                                            }
                                            isSaved = false
                                        }
                                    } else {
                                        currentMediaForStatus = s.selected
                                        showStatusDialog = true
                                    }
                                },
                                onOpenWeb = {
                                    s.selected.directWebUrl?.let { onOpenWeb(it) }
                                },
                                onDismiss = onDismiss
                            )
                            is SearchState.NotFound -> NotFoundCard(
                                query = s.query,
                                onRetryWithQuery = { newQ ->
                                    searchQuery = newQ
                                    activeQuery = newQ
                                }
                            )
                            is SearchState.Error -> ErrorCard(s.message)
                        }
                    }
                }
            }
        }

        // Watchlist Status Picker Dialog
        if (showStatusDialog && currentMediaForStatus != null) {
            val media = currentMediaForStatus!!
            Dialog(onDismissRequest = { showStatusDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Add to Watchlist",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = media.cleanTitle,
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(16.dp))

                        listOf(
                            Triple("CURRENT", "Watching", "Currently watching / reading"),
                            Triple("PLANNING", "Plan to Watch", "Save to watch later"),
                            Triple("COMPLETED", "Completed", "Finished watching"),
                            Triple("DROPPED", "Dropped", "Stopped watching")
                        ).forEach { (code, title, desc) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (colors.isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFFF3F4F6))
                                    .clickable {
                                        showStatusDialog = false
                                        scope.launch {
                                            val toSave = media.copy(
                                                userStatus = code,
                                                cachedAt = System.currentTimeMillis()
                                            )
                                            val synced = withContext(Dispatchers.IO) {
                                                repository.saveToLibrary(
                                                    media = toSave,
                                                    anilistToken = anilistToken,
                                                    tmdbApiKey = apiKey,
                                                    tmdbSessionId = tmdbSessionId,
                                                    tmdbAccountId = tmdbAccountId
                                                )
                                            }
                                            isSaved = true
                                            val statusLabel = when (code) {
                                                "CURRENT" -> "Watching"
                                                "COMPLETED" -> "Completed"
                                                "DROPPED" -> "Dropped"
                                                else -> "Plan to Watch"
                                            }
                                            val isAnime = toSave.mediaType.equals("Anime", ignoreCase = true) ||
                                                    toSave.mediaType.equals("Manga", ignoreCase = true)
                                            val target = if (isAnime) "AniList" else "TMDB"
                                            val msg = if (synced) "Saved as $statusLabel & synced to $target!" else "Saved as $statusLabel"
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val dotColor = when (code) {
                                    "CURRENT" -> Color(0xFF0095F6)
                                    "PLANNING" -> Color(0xFFA855F7)
                                    "COMPLETED" -> Color(0xFF10B981)
                                    else -> Color(0xFFEF4444)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                                    Text(desc, fontSize = 11.sp, color = colors.textSecondary)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        Spacer(Modifier.height(4.dp))
                        TextButton(
                            onClick = { showStatusDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel", color = colors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Search Bar Component
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (colors.isDark) Color(0xFF262940) else Color(0xFFF3F4F6))
            .border(
                1.dp,
                if (colors.isDark) Color(0xFF3F4668) else Color(0xFFE5E7EB),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = NeonAccent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Search media title...",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(NeonAccent),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(NeonAccent)
                .clickable { onSearch() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Run Search",
                tint = Color.Black,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PopupHeader(onDismiss: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFFF6B35), Color(0xFFDD2A7B))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "SceneFinder",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = "Anime · Manga · Movies · TV",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = colors.textSecondary
            )
        }
    }
}

@Composable
private fun IdleCard() {
    val colors = LocalAppColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Text(text = "🎬", fontSize = 36.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Type any title to search",
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Search across AniList, TMDB, MangaDex & more",
            color = colors.textSecondary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SearchingIndicator(query: String) {
    val colors = LocalAppColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(if (colors.isDark) Color(0xFF262D4A).copy(alpha = alpha) else Color(0xFFE2E8F0).copy(alpha = alpha)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(30.dp),
                color = NeonAccent,
                strokeWidth = 3.dp
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(text = "Searching for", color = colors.textSecondary, fontSize = 12.sp)
        Text(
            text = "\"$query\"",
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Checking AniList · TMDB · MangaDex…",
            color = colors.textTertiary,
            fontSize = 11.sp,
            fontStyle = FontStyle.Italic
        )
    }
}

/**
 * Fully merged result card with complete details and action buttons directly integrated,
 * plus a list of related formats & adaptations (Manga, TV Show, Movies).
 */
@Composable
private fun MergedResultCard(
    media: EnrichedMediaEntity,
    allResults: List<EnrichedMediaEntity>,
    isSaved: Boolean,
    onSelectMedia: (EnrichedMediaEntity) -> Unit,
    onToggleWatchlist: () -> Unit,
    onOpenWeb: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    val typeColor = when (media.mediaType.uppercase()) {
        "ANIME" -> Color(0xFF9C27B0)
        "MANGA", "MANHWA" -> Color(0xFF4CAF50)
        "MOVIE" -> Color(0xFF2196F3)
        MediaType.TV_SERIES.name,
        MediaType.TV_SHOWS.name,
        MediaType.WEB_SERIES.name,
        "TV SHOWS",
        "TV" -> Color(0xFFFF9800)
        else -> NeonAccent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Top section: Poster + Primary Details
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!media.posterUrl.isNullOrBlank()) {
                val context = LocalContext.current
                val posterRequest = remember(media.posterUrl) {
                    ImageRequest.Builder(context)
                        .data(media.posterUrl)
                        .size(264, 378)
                        .crossfade(140)
                        .allowHardware(true)
                        .allowRgb565(true)
                        .build()
                }
                AsyncImage(
                    model = posterRequest,
                    contentDescription = media.cleanTitle,
                    modifier = Modifier
                        .width(88.dp)
                        .height(126.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(88.dp)
                        .height(126.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (colors.isDark) Color(0xFF2A2A4A) else Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = media.mediaType.take(2).uppercase(),
                        color = typeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                // Badges row: Media Type + Age Rating + Score
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(typeColor.copy(alpha = 0.20f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = media.mediaType,
                            color = typeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    media.ageRating?.let { age ->
                        AgeRatingBadge(rating = age, fontSize = 9.sp)
                    }

                    // Score (properly scaled to 10 max)
                    media.rating?.let { rawRating ->
                        val displayScore = if (rawRating > 10f) rawRating / 10f else rawRating
                        if (displayScore > 0f) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating",
                                    tint = NeonOrange,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = String.format("%.1f", displayScore),
                                    fontSize = 11.sp,
                                    color = NeonOrange,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Title
                Text(
                    text = media.cleanTitle,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Subtitle metadata
                val extraDetails = listOfNotNull(
                    media.year?.toString(),
                    media.season?.let { "S$it" },
                    media.episode?.let { "EP$it" },
                    media.chapter?.let { "Ch.$it" },
                    media.sourceService
                ).joinToString(" • ")

                if (extraDetails.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = extraDetails,
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }

        // Synopsis
        if (!media.overview.isNullOrBlank()) {
            var isOverviewExpanded by remember { mutableStateOf(false) }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (colors.isDark) Color(0xFF0F1524) else Color(0xFFF3F4F6))
                    .clickable { isOverviewExpanded = !isOverviewExpanded }
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = media.overview!!,
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        maxLines = if (isOverviewExpanded) 12 else 4,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isOverviewExpanded) "Show less ▲" else "Read more ▼",
                        fontSize = 10.5.sp,
                        color = NeonAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Direct Action Buttons (merged from OverlayActivity)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Watchlist Button
            Button(
                onClick = onToggleWatchlist,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSaved) NeonGreen.copy(alpha = 0.2f) else NeonAccent,
                    contentColor = if (isSaved) NeonGreen else Color.Black
                ),
                modifier = Modifier.weight(1.1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isSaved) "In Library" else "+ Add to Library",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // View / Open Web Button
            Button(
                onClick = onOpenWeb,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                modifier = Modifier.weight(0.9f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "View",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // Dismiss Button
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (colors.isDark) DarkSurfaceVariant else Color(0xFFE5E7EB)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Dismiss",
                    color = colors.textPrimary,
                    fontSize = 12.sp
                )
            }
        }

        // Related Formats & Adaptations (Manga, TV Show, Movies)
        val relatedItems = allResults.filter { it.id != media.id }
        if (relatedItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Related Formats & Adaptations",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (colors.isDark) NeonAccent else Color(0xFF0077CC)
                )
                Text(
                    text = "${relatedItems.size} more",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                relatedItems.forEach { item ->
                    val itemTypeColor = when (item.mediaType.uppercase()) {
                        "ANIME" -> Color(0xFF9C27B0)
                        "MANGA", "MANHWA" -> Color(0xFF4CAF50)
                        "MOVIE" -> Color(0xFF2196F3)
                        "TV SHOWS", "TV", "TV_SERIES", "WEB SERIES" -> Color(0xFFFF9800)
                        else -> NeonAccent
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (colors.isDark) Color(0xFF1B1F34) else Color(0xFFF9FAFB))
                            .border(
                                1.dp,
                                if (colors.isDark) Color(0xFF2C3252) else Color(0xFFE5E7EB),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectMedia(item) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Poster thumbnail
                        if (!item.posterUrl.isNullOrBlank()) {
                            val context = LocalContext.current
                            val thumbRequest = remember(item.posterUrl) {
                                ImageRequest.Builder(context)
                                    .data(item.posterUrl)
                                    .size(120, 168)
                                    .crossfade(120)
                                    .allowHardware(true)
                                    .allowRgb565(true)
                                    .build()
                            }
                            AsyncImage(
                                model = thumbRequest,
                                contentDescription = item.cleanTitle,
                                modifier = Modifier
                                    .size(width = 40.dp, height = 56.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(width = 40.dp, height = 56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (colors.isDark) Color(0xFF2A2A4A) else Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.mediaType.take(2).uppercase(),
                                    color = itemTypeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(itemTypeColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = item.mediaType,
                                        color = itemTypeColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                item.ageRating?.let { age ->
                                    AgeRatingBadge(rating = age, fontSize = 8.sp)
                                }

                                item.rating?.let { rawRating ->
                                    val s = if (rawRating > 10f) rawRating / 10f else rawRating
                                    if (s > 0f) {
                                        Text(
                                            text = "⭐ ${String.format("%.1f", s)}",
                                            fontSize = 10.sp,
                                            color = NeonOrange,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = item.cleanTitle,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            val subtitle = listOfNotNull(
                                item.year?.toString(),
                                item.sourceService
                            ).joinToString(" • ")

                            if (subtitle.isNotBlank()) {
                                Text(
                                    text = subtitle,
                                    color = colors.textSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (colors.isDark) Color(0xFF29304F) else Color(0xFFE0E7FF))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Switch ›",
                                color = if (colors.isDark) NeonAccent else Color(0xFF4338CA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotFoundCard(
    query: String,
    onRetryWithQuery: (String) -> Unit
) {
    val colors = LocalAppColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(text = "🔍", fontSize = 36.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "No match found for", color = colors.textSecondary, fontSize = 12.sp)
        Text(
            text = "\"$query\"",
            color = colors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Try editing the title above to match the exact show, movie, or manga name.",
            color = colors.textSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

@Composable
private fun ErrorCard(message: String) {
    val colors = LocalAppColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(text = "⚠️", fontSize = 32.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "Search error", color = Color(0xFFFF5252), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            color = colors.textSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
