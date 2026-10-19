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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.scenefinder.data.local.dao.EnrichedMediaDao
import com.scenefinder.data.local.entity.EnrichedMediaEntity
import com.scenefinder.data.prefs.AppSettings
import com.scenefinder.data.repository.MediaEnrichmentRepository
import com.scenefinder.domain.model.MediaType
import com.scenefinder.domain.parser.CandidateExtractor
import com.scenefinder.ui.components.AgeRatingBadge
import com.scenefinder.ui.components.BrutalBadge
import com.scenefinder.ui.components.BrutalButton
import com.scenefinder.ui.components.BrutalCard
import com.scenefinder.ui.components.brutalShadow
import com.scenefinder.ui.theme.BrutalBlack
import com.scenefinder.ui.theme.BrutalBorder
import com.scenefinder.ui.theme.BrutalBorderMuted
import com.scenefinder.ui.theme.BrutalCard
import com.scenefinder.ui.theme.BrutalCardElevated
import com.scenefinder.ui.theme.BrutalCyan
import com.scenefinder.ui.theme.BrutalDarkBg
import com.scenefinder.ui.theme.BrutalLime
import com.scenefinder.ui.theme.BrutalOrange
import com.scenefinder.ui.theme.BrutalPink
import com.scenefinder.ui.theme.BrutalPurple
import com.scenefinder.ui.theme.BrutalTextInverse
import com.scenefinder.ui.theme.BrutalTextPrimary
import com.scenefinder.ui.theme.BrutalTextSecondary
import com.scenefinder.ui.theme.BrutalYellow
import com.scenefinder.ui.theme.LocalAppThemeColors
import com.scenefinder.ui.theme.SceneFinderTheme
import com.scenefinder.ui.theme.ThemeSearchingIndicator
import com.scenefinder.ui.theme.ThemeAmbientOverlay
import com.scenefinder.ui.theme.CyberCornerTechNotches
import com.scenefinder.ui.theme.JarvisCornerTechBrackets
import com.scenefinder.ui.theme.CyberpunkYellow
import com.scenefinder.ui.theme.CyberpunkCyan
import com.scenefinder.ui.theme.IronManGold
import com.scenefinder.ui.theme.IronManBlueBright
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.compose.runtime.collectAsState

/**
 * Neo-Brutalist SceneFinder search & detail popup.
 *
 * Aesthetics:
 * - Thick, high-contrast borders
 * - Crisp 0-blur offset drop shadows
 * - Saturated electric primary colors (Canary Yellow, Cyber Cyan, Acid Lime, Punchy Pink)
 * - Tactile buttons that press into their shadows
 * - Raw, bold typography
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
            val currentTheme by settings.theme.collectAsState()
            SceneFinderTheme(theme = currentTheme) {
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

    // Execute multi-format search
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
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .imePadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val tc = LocalAppThemeColors.current
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
            // Main Neo-Brutalist / themed Bottom Card
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 18.dp)
                    .fillMaxWidth()
                    .brutalShadow(offset = 6.dp, color = tc.shadowColor, cornerRadius = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(tc.background)
                    .border(2.5.dp, tc.primary, RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                // Movie & Game HUD Ambient Animation Layer
                ThemeAmbientOverlay(modifier = Modifier.matchParentSize())

                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .heightIn(max = 640.dp)
                ) {
                    // Brutal Header
                    BrutalPopupHeader(onDismiss = onDismiss)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Brutal Search Bar with Pre-filled Text
                    BrutalSearchBar(
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

                    // Dynamic State Container
                    AnimatedContent(
                        targetState = state,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                        label = "search_state"
                    ) { s ->
                        when (s) {
                            is SearchState.Idle -> BrutalIdleCard()
                            is SearchState.Loading -> ThemeSearchingIndicator(s.query)
                            is SearchState.Found -> BrutalResultCard(
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
                            is SearchState.NotFound -> BrutalNotFoundCard(
                                query = s.query,
                                onRetryWithQuery = { newQ ->
                                    searchQuery = newQ
                                    activeQuery = newQ
                                }
                            )
                            is SearchState.Error -> BrutalErrorCard(s.message)
                        }
                    }
                }
            }
        }

        // Neo-Brutalist Watchlist Status Dialog
        if (showStatusDialog && currentMediaForStatus != null) {
            val media = currentMediaForStatus!!
            val tc2 = LocalAppThemeColors.current
            Dialog(onDismissRequest = { showStatusDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = tc2.surface,
                    border = androidx.compose.foundation.BorderStroke(2.5.dp, tc2.primary),
                    shadowElevation = 16.dp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BrutalBadge(
                            text = if (tc2.isCyberpunk) "[ STATUS.PICKER ]" else if (tc2.isIronMan) "JARVIS: STATUS" else "STATUS PICKER",
                            backgroundColor = tc2.primary,
                            textColor = tc2.textInverse,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = media.cleanTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = tc2.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(16.dp))

                        listOf(
                            Triple("CURRENT",   "WATCHING",      tc2.secondary),
                            Triple("PLANNING",  "PLAN TO WATCH", tc2.primary),
                            Triple("COMPLETED", "COMPLETED",     tc2.tertiary),
                            Triple("DROPPED",   "DROPPED",       tc2.accent)
                        ).forEach { (code, title, color) ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color)
                                    .border(2.dp, tc2.shadowColor, RoundedCornerShape(8.dp))
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
                                            val isAnime = toSave.mediaType.equals("Anime", ignoreCase = true) ||
                                                    toSave.mediaType.equals("Manga", ignoreCase = true)
                                            val target = if (isAnime) "AniList" else "TMDB"
                                            val msg = if (synced) "Saved as $title & synced to $target!" else "Saved as $title"
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = tc2.textInverse,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = { showStatusDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("CANCEL", color = tc2.textSecondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Neo-Brutalist Search Bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BrutalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    val tc = LocalAppThemeColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .brutalShadow(offset = 3.dp, color = tc.shadowColor, cornerRadius = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tc.surface)
            .border(2.dp, tc.border, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = tc.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "SEARCH ANY TITLE...",
                    color = tc.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    color = tc.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                ),
                cursorBrush = SolidColor(tc.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = tc.accent,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        // Accent action button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(tc.primary)
                .border(1.5.dp, tc.shadowColor, RoundedCornerShape(6.dp))
                .clickable { onSearch() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Run Search",
                tint = tc.textInverse,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Neo-Brutalist Sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BrutalPopupHeader(onDismiss: () -> Unit) {
    val tc = LocalAppThemeColors.current
    val headerLabel = when {
        tc.isCyberpunk -> "[ SCENEFINDER.HUD ]"
        tc.isIronMan   -> "FRIDAY: SEARCH"
        else           -> "SCENEFINDER"
    }
    val subLabel = when {
        tc.isCyberpunk -> "v2.077 // MEDIA-DETECT"
        tc.isIronMan   -> "STARK TECH // MEDIA-DETECT"
        else           -> "NEO-MEDIA DETECT"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrutalBadge(
                text = headerLabel,
                backgroundColor = tc.primary,
                textColor = tc.textInverse,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = subLabel,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                color = tc.secondary,
                letterSpacing = 0.6.sp
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(tc.surfaceElevated)
                .border(1.5.dp, tc.border, RoundedCornerShape(6.dp))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = tc.textPrimary,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun BrutalIdleCard() {
    val tc = LocalAppThemeColors.current
    val idleEmoji = when {
        tc.isCyberpunk -> "💻"
        tc.isIronMan   -> "🧠"
        else           -> "⚡"
    }
    BrutalCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        backgroundColor = tc.surface,
        borderColor = tc.border
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(22.dp)
        ) {
            Text(text = idleEmoji, fontSize = 36.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "TYPE OR SELECT ANY TITLE",
                color = tc.primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "AniList • TMDB • MangaDex Universal Lookup",
                color = tc.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun BrutalSearchingIndicator(query: String) {
    ThemeSearchingIndicator(query = query)
}

/**
 * Fully Neo-Brutalist Result Card with hard shadows, punchy tags, tactile buttons,
 * and related adaptations block.
 */
@Composable
private fun BrutalResultCard(
    media: EnrichedMediaEntity,
    allResults: List<EnrichedMediaEntity>,
    isSaved: Boolean,
    onSelectMedia: (EnrichedMediaEntity) -> Unit,
    onToggleWatchlist: () -> Unit,
    onOpenWeb: () -> Unit,
    onDismiss: () -> Unit
) {
    val tc = LocalAppThemeColors.current
    val typeColor = when (media.mediaType.uppercase()) {
        "ANIME" -> BrutalPurple
        "MANGA", "MANHWA" -> BrutalLime
        "MOVIE" -> BrutalCyan
        MediaType.TV_SERIES.name,
        MediaType.TV_SHOWS.name,
        MediaType.WEB_SERIES.name,
        "TV SHOWS",
        "TV" -> BrutalOrange
        else -> BrutalYellow
    }

    val hudBanner = when {
        tc.isCyberpunk -> "[ RECON.LOCKED // TARGET IDENTIFIED ]"
        tc.isIronMan   -> "MARK-85 HUD: TARGET LOCK ACQUIRED"
        else           -> null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Sci-Fi / Game HUD banner if in Cyberpunk or Iron Man mode
        if (hudBanner != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = hudBanner,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = if (tc.isCyberpunk) CyberpunkYellow else IronManGold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = if (tc.isCyberpunk) "SIGNAL 100%" else "FRIDAY: VERIFIED",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (tc.isCyberpunk) CyberpunkCyan else IronManBlueBright
                )
            }
        }

        // Main Media Display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Poster with hard border & shadow
            if (!media.posterUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .brutalShadow(offset = 4.dp, color = BrutalBlack, cornerRadius = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(2.dp, BrutalBorder, RoundedCornerShape(10.dp))
                ) {
                    AsyncImage(
                        model = media.posterUrl,
                        contentDescription = media.cleanTitle,
                        modifier = Modifier
                            .width(90.dp)
                            .height(130.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(130.dp)
                        .brutalShadow(offset = 4.dp, color = BrutalBlack, cornerRadius = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrutalCardElevated)
                        .border(2.dp, BrutalBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = media.mediaType.take(2).uppercase(),
                        color = typeColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                // Badges row: Type Tag + Age Rating + Score Tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BrutalBadge(
                        text = media.mediaType,
                        backgroundColor = typeColor,
                        textColor = BrutalBlack
                    )

                    media.ageRating?.let { age ->
                        AgeRatingBadge(rating = age)
                    }

                    // Score (properly scaled to 10 max)
                    media.rating?.let { rawRating ->
                        val displayScore = if (rawRating > 10f) rawRating / 10f else rawRating
                        if (displayScore > 0f) {
                            BrutalBadge(
                                text = "⭐ ${String.format("%.1f", displayScore)}",
                                backgroundColor = BrutalYellow,
                                textColor = BrutalBlack
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = media.cleanTitle,
                    color = BrutalTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = extraDetails,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrutalCyan
                    )
                }
            }
        }

        // Synopsis Card
        if (!media.overview.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            BrutalCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = BrutalCard,
                borderColor = BrutalBorderMuted,
                borderWidth = 1.5.dp,
                shadowOffset = 3.dp
            ) {
                Text(
                    text = media.overview!!,
                    color = BrutalTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Direct Action Buttons with Tactile Brutal Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Watchlist Button
            BrutalButton(
                onClick = onToggleWatchlist,
                backgroundColor = if (isSaved) BrutalLime else BrutalYellow,
                modifier = Modifier.weight(1.1f)
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                    contentDescription = null,
                    tint = BrutalBlack,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isSaved) "SAVED ✓" else "+ WATCHLIST",
                    fontWeight = FontWeight.Black,
                    color = BrutalBlack,
                    fontSize = 11.5.sp
                )
            }

            // View Button
            BrutalButton(
                onClick = onOpenWeb,
                backgroundColor = BrutalCyan,
                modifier = Modifier.weight(0.9f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = BrutalBlack,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "VIEW ↗",
                    color = BrutalBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.5.sp
                )
            }

            // Dismiss Button
            BrutalButton(
                onClick = onDismiss,
                backgroundColor = BrutalCardElevated,
                contentColor = BrutalTextPrimary,
                borderColor = BrutalBorderMuted
            ) {
                Text(
                    text = "CLOSE",
                    color = BrutalTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                )
            }
        }

        // Related Formats & Adaptations (Manga, TV Show, Movies)
        val relatedItems = allResults.filter { it.id != media.id }
        if (relatedItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "RELATED FORMATS // ADAPTATIONS",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = BrutalYellow,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${relatedItems.size} AVAILABLE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrutalTextSecondary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                relatedItems.forEach { item ->
                    val itemTypeColor = when (item.mediaType.uppercase()) {
                        "ANIME" -> BrutalPurple
                        "MANGA", "MANHWA" -> BrutalLime
                        "MOVIE" -> BrutalCyan
                        "TV SHOWS", "TV", "TV_SERIES", "WEB SERIES" -> BrutalOrange
                        else -> BrutalYellow
                    }
                    BrutalCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = BrutalCard,
                        borderColor = BrutalBorderMuted,
                        borderWidth = 1.5.dp,
                        shadowOffset = 3.dp,
                        onClick = { onSelectMedia(item) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Poster thumbnail
                            if (!item.posterUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = item.posterUrl,
                                    contentDescription = item.cleanTitle,
                                    modifier = Modifier
                                        .size(width = 42.dp, height = 58.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(1.dp, BrutalBorder, RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(width = 42.dp, height = 58.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(BrutalCardElevated)
                                        .border(1.dp, BrutalBorder, RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.mediaType.take(2).uppercase(),
                                        color = itemTypeColor,
                                        fontWeight = FontWeight.Black,
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
                                    BrutalBadge(
                                        text = item.mediaType,
                                        backgroundColor = itemTypeColor,
                                        textColor = BrutalBlack,
                                        fontSize = 8.5.sp
                                    )

                                    item.ageRating?.let { age ->
                                        AgeRatingBadge(rating = age, fontSize = 8.sp)
                                    }

                                    item.rating?.let { rawRating ->
                                        val s = if (rawRating > 10f) rawRating / 10f else rawRating
                                        if (s > 0f) {
                                            BrutalBadge(
                                                text = "⭐ ${String.format("%.1f", s)}",
                                                backgroundColor = BrutalYellow,
                                                fontSize = 8.5.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = item.cleanTitle,
                                    color = BrutalTextPrimary,
                                    fontWeight = FontWeight.Black,
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
                                        color = BrutalCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BrutalYellow)
                                    .border(1.dp, BrutalBlack, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "SWITCH ›",
                                    color = BrutalBlack,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrutalNotFoundCard(
    query: String,
    onRetryWithQuery: (String) -> Unit
) {
    BrutalCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        backgroundColor = BrutalCard,
        borderColor = BrutalOrange
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(20.dp)
        ) {
            Text(text = "🔍", fontSize = 36.sp)
            Spacer(modifier = Modifier.height(10.dp))
            BrutalBadge(text = "NO MATCH FOUND", backgroundColor = BrutalOrange)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\"$query\"",
                color = BrutalTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Try typing the exact English or Romaji title in the search box above.",
                color = BrutalTextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun BrutalErrorCard(message: String) {
    BrutalCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        backgroundColor = BrutalCard,
        borderColor = BrutalPink
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(20.dp)
        ) {
            Text(text = "⚠️", fontSize = 32.sp)
            Spacer(modifier = Modifier.height(10.dp))
            BrutalBadge(text = "SEARCH ERROR", backgroundColor = BrutalPink)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                color = BrutalTextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
