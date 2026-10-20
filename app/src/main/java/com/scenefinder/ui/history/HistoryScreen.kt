package com.scenefinder.ui.history

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.scenefinder.data.local.dao.EnrichedMediaDao
import com.scenefinder.data.local.entity.EnrichedMediaEntity
import com.scenefinder.ui.components.AgeRatingBadge
import com.scenefinder.ui.components.ProgressTrackingDialog
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
import com.scenefinder.ui.theme.IGYellow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

private val storyRingBrushHoriz = Brush.horizontalGradient(
    listOf(IGGradStart, IGGradMid, IGGradEnd)
)

private val posterGradientOverlay = Brush.verticalGradient(
    listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
)

private fun getMediaTypeColor(type: String): Color = when (type) {
    "Anime" -> IGGradMid
    "Manga" -> IGGradStart
    "Movie" -> IGBlue
    "TV Shows", "TV Series", "Web Series" -> IGGreen
    "Manhwa" -> IGGradStart
    else -> IGTextSecondary
}

private fun getStatusColor(status: String?): Color = when (status) {
    "CURRENT" -> IGBlue
    "PLANNING" -> Color(0xFFA855F7)
    "COMPLETED" -> IGGreen
    "DROPPED" -> IGRed
    else -> Color(0x33FFFFFF)
}

@Composable
private fun ActionStatusChip(
    label: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.16f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun IGBatchActionBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAllToggle: () -> Unit,
    onCancel: () -> Unit,
    onMarkCompleted: () -> Unit,
    onMarkWatching: () -> Unit,
    onMarkPlanToWatch: () -> Unit,
    onMarkDropped: () -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(IGCard)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Outlined.Close, contentDescription = "Cancel", tint = IGTextPrimary)
            }
            Text(
                text = "$selectedCount Selected",
                color = IGTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSelectAllToggle) {
                Icon(
                    imageVector = if (selectedCount == totalCount) Icons.Default.Deselect else Icons.Default.SelectAll,
                    contentDescription = if (selectedCount == totalCount) "Deselect All" else "Select All",
                    tint = IGTextPrimary
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = IGRed)
            }
        }

        // Quick status actions row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ActionStatusChip(
                    label = "Watching",
                    color = IGBlue,
                    icon = Icons.Default.PlayArrow,
                    onClick = onMarkWatching
                )
            }
            item {
                ActionStatusChip(
                    label = "Plan to Watch",
                    color = Color(0xFFA855F7),
                    icon = Icons.Default.BookmarkAdd,
                    onClick = onMarkPlanToWatch
                )
            }
            item {
                ActionStatusChip(
                    label = "Completed",
                    color = IGGreen,
                    icon = Icons.Default.CheckCircle,
                    onClick = onMarkCompleted
                )
            }
            item {
                ActionStatusChip(
                    label = "Dropped",
                    color = IGRed,
                    icon = Icons.Default.Cancel,
                    onClick = onMarkDropped
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(IGDivider))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    dao: EnrichedMediaDao,
    repository: MediaEnrichmentRepository,
    settings: AppSettings,
    modifier: Modifier = Modifier,
    startInSearch: Boolean = false,
    onBackToHome: () -> Unit = {},
    onNavigateToDiscover: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val historyList by dao.getAllHistory().collectAsState(initial = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current

    var itemToSaveWithStatus by remember { mutableStateOf<EnrichedMediaEntity?>(null) }
    var selectedMediaForDetail by remember { mutableStateOf<EnrichedMediaEntity?>(null) }
    var progressDialogMedia by remember { mutableStateOf<EnrichedMediaEntity?>(null) }
    var selectedMediaIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val isSelectionMode = selectedMediaIds.isNotEmpty()
    val onCardClick = remember(isSelectionMode) {
        { item: EnrichedMediaEntity ->
            if (isSelectionMode) {
                selectedMediaIds = if (selectedMediaIds.contains(item.id)) {
                    selectedMediaIds - item.id
                } else {
                    selectedMediaIds + item.id
                }
            } else {
                selectedMediaForDetail = item
            }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchingOnline by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<EnrichedMediaEntity>>(emptyList()) }
    var predictedSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }
    var isSearchBarActive by remember { mutableStateOf(startInSearch) }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var selectedMediaTypeFilter by remember { mutableStateOf("ALL") }
    var showFilterModal by remember { mutableStateOf(false) }

    val mediaTypeFilters = listOf(
        "ALL" to "All",
        "Anime" to "Anime",
        "Manga" to "Manga",
        "Movie" to "Movie",
        "Manhwa" to "Manhwa",
        "TV Shows" to "TV Shows",
        "Web Series" to "Web Series"
    )

    // Categories for Search: Anime, Manga, Movie, Manhwa, Web Series, TV Shows
    val searchCategories = listOf(
        "ALL" to "All",
        "Anime" to "Anime",
        "Manga" to "Manga",
        "Movie" to "Movie",
        "Manhwa" to "Manhwa",
        "TV Shows" to "TV Shows",
        "Web Series" to "Web Series"
    )
    var selectedSearchCategory by remember { mutableStateOf("ALL") }

    val statusFilters = listOf(
        "ALL" to "All",
        "CURRENT" to "Watching",
        "PLANNING" to "Plan to Watch",
        "COMPLETED" to "Completed",
        "DROPPED" to "Dropped"
    )

    val apiKey by settings.tmdbApiKey.collectAsState()
    val tmdbSessionId by settings.tmdbSessionId.collectAsState()
    val tmdbAccountId by settings.tmdbAccountId.collectAsState()
    val anilistToken by settings.anilistToken.collectAsState()
    var isCloudSyncing by remember { mutableStateOf(false) }
    val savedIds = remember(historyList) { historyList.map { it.id }.toHashSet() }

    fun triggerCloudSync() {
        if (anilistToken.isBlank() && (tmdbSessionId.isBlank() || tmdbSessionId.startsWith("__pending__:"))) {
            Toast.makeText(context, "Connect AniList or TMDB in Settings first", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            isCloudSyncing = true
            var aniCount = 0
            var tmdbCount = 0
            if (anilistToken.isNotBlank()) {
                aniCount = withContext(Dispatchers.IO) { repository.syncAniListLibrary(anilistToken) }
            }
            if (tmdbSessionId.isNotBlank() && !tmdbSessionId.startsWith("__pending__:") && tmdbAccountId > 0) {
                val effectiveKey = apiKey.ifBlank { MediaEnrichmentRepository.DEFAULT_TMDB_API_KEY }
                tmdbCount = withContext(Dispatchers.IO) { repository.syncTmdbLibrary(effectiveKey, tmdbSessionId, tmdbAccountId) }
            }
            isCloudSyncing = false
            Toast.makeText(context, "Synced: AniList ($aniCount) | TMDB ($tmdbCount)", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle back button on navigation: return to previous place or exit selection
    BackHandler(enabled = isSelectionMode || isSearchBarActive || searchQuery.isNotEmpty()) {
        if (isSelectionMode) {
            selectedMediaIds = emptySet()
        } else if (searchQuery.isNotEmpty()) {
            searchQuery = ""
            hasSearched = false
            searchResults = emptyList()
            predictedSuggestions = emptyList()
        } else if (!startInSearch && isSearchBarActive) {
            isSearchBarActive = false
        } else {
            onBackToHome()
        }
    }

    LaunchedEffect(startInSearch) {
        if (startInSearch) isSearchBarActive = true
    }

    // Auto-refresh and backfill age ratings directly from APIs for any existing library items
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val effectiveKey = apiKey.ifBlank { MediaEnrichmentRepository.DEFAULT_TMDB_API_KEY }
            repository.refreshAllAgeRatings(effectiveKey)
        }
    }

    // Predictive live search as the user types (with 280ms debounce)
    LaunchedEffect(searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            hasSearched = false
            searchResults = emptyList()
            predictedSuggestions = emptyList()
            isSearchingOnline = false
            return@LaunchedEffect
        }

        hasSearched = true
        isSearchingOnline = true

        // 1. Immediately match against user's local library (0ms latency)
        val localMatches = historyList.filter {
            it.cleanTitle.contains(query, ignoreCase = true) ||
            (it.englishTitle?.contains(query, ignoreCase = true) == true) ||
            (it.originalTitle?.contains(query, ignoreCase = true) == true)
        }
        if (localMatches.isNotEmpty()) {
            searchResults = localMatches
            predictedSuggestions = localMatches.map { it.cleanTitle }.distinct().take(6)
        }

        // 2. Debounce by 280ms before hitting external APIs to avoid lag while typing fast
        delay(280)

        // 3. Query AniList, TMDB, and MangaDex concurrently
        val remoteResults = withContext(Dispatchers.IO) {
            repository.searchAllMedia(query, apiKey)
        }

        val combined = (localMatches + remoteResults).distinctBy { it.cleanTitle.lowercase() }
        searchResults = combined

        // 4. Derive smart predictive autocomplete suggestions
        val suggestions = combined.map { it.cleanTitle }
            .distinct()
            .sortedWith(
                compareByDescending<String> { it.startsWith(query, ignoreCase = true) }
                    .thenBy { it.length }
            )
            .take(8)
        predictedSuggestions = suggestions
        isSearchingOnline = false
    }

    fun performOnlineSearch() {
        val query = searchQuery.trim()
        if (query.isEmpty()) return
        keyboardController?.hide()
        isSearchingOnline = true
        hasSearched = true
        scope.launch(Dispatchers.IO) {
            val results = repository.searchAllMedia(query, apiKey)
            val localMatches = historyList.filter {
                it.cleanTitle.contains(query, ignoreCase = true) ||
                (it.englishTitle?.contains(query, ignoreCase = true) == true)
            }
            val combined = (localMatches + results).distinctBy { it.cleanTitle.lowercase() }
            val suggestions = combined.map { it.cleanTitle }
                .distinct()
                .sortedWith(
                    compareByDescending<String> { it.startsWith(query, ignoreCase = true) }
                        .thenBy { it.length }
                )
                .take(8)
            launch(Dispatchers.Main) {
                searchResults = combined
                predictedSuggestions = suggestions
                isSearchingOnline = false
            }
        }
    }

    val filteredLibrary = remember(historyList, selectedStatusFilter, selectedMediaTypeFilter) {
        var list = historyList
        if (selectedStatusFilter != "ALL") {
            list = list.filter { it.userStatus == selectedStatusFilter }
        }
        if (selectedMediaTypeFilter != "ALL") {
            list = when (selectedMediaTypeFilter) {
                "TV Shows" -> list.filter {
                    it.mediaType.equals("TV Shows", ignoreCase = true) ||
                    it.mediaType.equals("TV Series", ignoreCase = true)
                }
                "Web Series" -> list.filter {
                    it.mediaType.equals("Web Series", ignoreCase = true)
                }
                else -> list.filter {
                    it.mediaType.equals(selectedMediaTypeFilter, ignoreCase = true)
                }
            }
        }
        list
    }

    val isPageWise by settings.isPageWiseLayout.collectAsState()
    val itemsPerPage = 24
    var currentPage by remember(selectedStatusFilter, selectedMediaTypeFilter, isPageWise) { mutableIntStateOf(1) }
    val totalPages = remember(filteredLibrary.size, itemsPerPage) {
        maxOf(1, (filteredLibrary.size + itemsPerPage - 1) / itemsPerPage)
    }
    val safeCurrentPage = currentPage.coerceIn(1, totalPages)

    val pageSize = 60
    var displayCount by remember(selectedStatusFilter, selectedMediaTypeFilter) { mutableIntStateOf(pageSize) }
    val displayedLibrary = remember(filteredLibrary, isPageWise, safeCurrentPage, displayCount) {
        if (isPageWise) {
            filteredLibrary.drop((safeCurrentPage - 1) * itemsPerPage).take(itemsPerPage)
        } else {
            filteredLibrary.take(displayCount)
        }
    }

    val listState = rememberLazyGridState()

    // Filter search results by categorical options
    val displayedSearchResults = remember(searchResults, selectedSearchCategory) {
        when (selectedSearchCategory) {
            "ALL" -> searchResults
            "Web Series" -> searchResults.filter {
                it.mediaType.equals("Web Series", ignoreCase = true)
            }
            "TV Shows" -> searchResults.filter {
                it.mediaType.equals("TV Shows", ignoreCase = true) ||
                it.mediaType.equals("TV Series", ignoreCase = true)
            }
            else -> searchResults.filter {
                it.mediaType.equals(selectedSearchCategory, ignoreCase = true)
            }
        }
    }

    val displayedPredictions = remember(searchResults, predictedSuggestions, selectedSearchCategory) {
        if (selectedSearchCategory == "ALL") {
            predictedSuggestions
        } else {
            searchResults
                .filter {
                    when (selectedSearchCategory) {
                        "Web Series" -> it.mediaType.equals("Web Series", ignoreCase = true)
                        "TV Shows" -> it.mediaType.equals("TV Shows", ignoreCase = true) ||
                            it.mediaType.equals("TV Series", ignoreCase = true)
                        else -> it.mediaType.equals(selectedSearchCategory, ignoreCase = true)
                    }
                }
                .map { it.cleanTitle }
                .distinct()
                .take(8)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(IGBlack)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = listState,
            modifier = Modifier.fillMaxSize().background(IGBlack),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
        // Top bar: either Batch Action Bar (in selection mode) or IGTopBar
        item(span = { GridItemSpan(maxLineSpan) }) {
            if (isSelectionMode) {
                IGBatchActionBar(
                    selectedCount = selectedMediaIds.size,
                    totalCount = displayedLibrary.size,
                    onSelectAllToggle = {
                        selectedMediaIds = if (selectedMediaIds.size == displayedLibrary.size) {
                            emptySet()
                        } else {
                            displayedLibrary.map { it.id }.toSet()
                        }
                    },
                    onCancel = { selectedMediaIds = emptySet() },
                    onMarkCompleted = {
                        val targets = selectedMediaIds.toList()
                        val targetEntities = displayedLibrary.filter { targets.contains(it.id) }
                        scope.launch(Dispatchers.IO) {
                            dao.updateStatusForIds(targets, "COMPLETED")
                            for (entity in targetEntities) {
                                try {
                                    repository.updateMediaStatus(
                                        media = entity,
                                        newStatus = "COMPLETED",
                                        anilistToken = anilistToken,
                                        tmdbApiKey = apiKey,
                                        tmdbSessionId = tmdbSessionId,
                                        tmdbAccountId = tmdbAccountId
                                    )
                                } catch (e: Exception) { }
                            }
                            launch(Dispatchers.Main) {
                                Toast.makeText(context, "Marked ${targets.size} as Completed", Toast.LENGTH_SHORT).show()
                            }
                        }
                        selectedMediaIds = emptySet()
                    },
                    onMarkWatching = {
                        val targets = selectedMediaIds.toList()
                        val targetEntities = displayedLibrary.filter { targets.contains(it.id) }
                        scope.launch(Dispatchers.IO) {
                            dao.updateStatusForIds(targets, "CURRENT")
                            for (entity in targetEntities) {
                                try {
                                    repository.updateMediaStatus(
                                        media = entity,
                                        newStatus = "CURRENT",
                                        anilistToken = anilistToken,
                                        tmdbApiKey = apiKey,
                                        tmdbSessionId = tmdbSessionId,
                                        tmdbAccountId = tmdbAccountId
                                    )
                                } catch (e: Exception) { }
                            }
                            launch(Dispatchers.Main) {
                                Toast.makeText(context, "Marked ${targets.size} as Watching", Toast.LENGTH_SHORT).show()
                            }
                        }
                        selectedMediaIds = emptySet()
                    },
                    onMarkPlanToWatch = {
                        val targets = selectedMediaIds.toList()
                        val targetEntities = displayedLibrary.filter { targets.contains(it.id) }
                        scope.launch(Dispatchers.IO) {
                            dao.updateStatusForIds(targets, "PLANNING")
                            for (entity in targetEntities) {
                                try {
                                    repository.updateMediaStatus(
                                        media = entity,
                                        newStatus = "PLANNING",
                                        anilistToken = anilistToken,
                                        tmdbApiKey = apiKey,
                                        tmdbSessionId = tmdbSessionId,
                                        tmdbAccountId = tmdbAccountId
                                    )
                                } catch (e: Exception) { }
                            }
                            launch(Dispatchers.Main) {
                                Toast.makeText(context, "Marked ${targets.size} as Plan to Watch", Toast.LENGTH_SHORT).show()
                            }
                        }
                        selectedMediaIds = emptySet()
                    },
                    onMarkDropped = {
                        val targets = selectedMediaIds.toList()
                        val targetEntities = displayedLibrary.filter { targets.contains(it.id) }
                        scope.launch(Dispatchers.IO) {
                            dao.updateStatusForIds(targets, "DROPPED")
                            for (entity in targetEntities) {
                                try {
                                    repository.updateMediaStatus(
                                        media = entity,
                                        newStatus = "DROPPED",
                                        anilistToken = anilistToken,
                                        tmdbApiKey = apiKey,
                                        tmdbSessionId = tmdbSessionId,
                                        tmdbAccountId = tmdbAccountId
                                    )
                                } catch (e: Exception) { }
                            }
                            launch(Dispatchers.Main) {
                                Toast.makeText(context, "Marked ${targets.size} as Dropped", Toast.LENGTH_SHORT).show()
                            }
                        }
                        selectedMediaIds = emptySet()
                    },
                    onRemove = {
                        val targets = selectedMediaIds.toList()
                        val targetEntities = displayedLibrary.filter { targets.contains(it.id) }
                        scope.launch(Dispatchers.IO) {
                            dao.deleteByIds(targets)
                            for (entity in targetEntities) {
                                try {
                                    repository.removeFromLibrary(
                                        media = entity,
                                        anilistToken = anilistToken,
                                        tmdbApiKey = apiKey,
                                        tmdbSessionId = tmdbSessionId,
                                        tmdbAccountId = tmdbAccountId
                                    )
                                } catch (e: Exception) { }
                            }
                            launch(Dispatchers.Main) {
                                Toast.makeText(context, "Removed ${targets.size} items", Toast.LENGTH_SHORT).show()
                            }
                        }
                        selectedMediaIds = emptySet()
                    }
                )
            } else {
                IGTopBar(
                    isSearchActive = isSearchBarActive,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { q ->
                        searchQuery = q
                        if (q.isBlank()) {
                            hasSearched = false
                            searchResults = emptyList()
                            predictedSuggestions = emptyList()
                        }
                    },
                    onSearchToggle = {
                        isSearchBarActive = !isSearchBarActive
                        if (!isSearchBarActive) {
                            searchQuery = ""
                            hasSearched = false
                            searchResults = emptyList()
                            predictedSuggestions = emptyList()
                        }
                    },
                    onSearch = ::performOnlineSearch,
                    onClearAll = { scope.launch(Dispatchers.IO) { dao.clearAll() } },
                    showClearAll = historyList.isNotEmpty() && !hasSearched,
                    focusRequester = searchFocusRequester,
                    isSearchingOnline = isSearchingOnline,
                    isCloudSyncing = isCloudSyncing,
                    isPageWise = isPageWise,
                    onTogglePageWise = {
                        val next = !isPageWise
                        settings.setPageWiseLayout(next)
                        Toast.makeText(context, if (next) "Page-by-page mode enabled" else "Continuous scroll mode enabled", Toast.LENGTH_SHORT).show()
                    },
                    onNavigateToDiscover = onNavigateToDiscover,
                    onNavigateToSettings = onNavigateToSettings,
                    onSync = ::triggerCloudSync
                )
            }
        }

        // Categorical options bar for Search: Anime, Manga, Movie, Manhwa, Web Series (Pink Row)
        if (isSearchBarActive) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    items(
                        items = searchCategories,
                        key = { it.first }
                    ) { (code, label) ->
                        val selected = selectedSearchCategory == code
                        val bgColor by animateColorAsState(if (selected) IGGradMid else IGCard, tween(200), label = "cat_bg")
                        val textColor by animateColorAsState(if (selected) Color.White else IGTextSecondary, tween(200), label = "cat_text")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(bgColor)
                                .border(1.dp, if (selected) Color.Transparent else IGDivider, RoundedCornerShape(16.dp))
                                .clickable { selectedSearchCategory = code }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = textColor
                            )
                        }
                    }
                }
            }

            // User request: "keep which is in pink and make the other 2 below it in a button"
            item(span = { GridItemSpan(maxLineSpan) }) {
                val isFiltered = selectedStatusFilter != "ALL" || selectedMediaTypeFilter != "ALL"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { showFilterModal = true },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isFiltered) Color(0xFF00F2FE).copy(alpha = 0.20f) else IGCard,
                        border = BorderStroke(0.75.dp, if (isFiltered) Color(0xFF00F2FE).copy(alpha = 0.7f) else IGDivider),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Tune,
                                contentDescription = "Filters",
                                tint = if (isFiltered) Color(0xFF00F2FE) else IGTextPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            val labelText = if (isFiltered) {
                                val active = mutableListOf<String>()
                                if (selectedStatusFilter != "ALL") active.add(statusFilters.find { it.first == selectedStatusFilter }?.second ?: selectedStatusFilter)
                                if (selectedMediaTypeFilter != "ALL") active.add(selectedMediaTypeFilter)
                                "Filtered: " + active.joinToString(" • ")
                            } else {
                                "Filters (Status & Library) ▾"
                            }
                            Text(
                                text = labelText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isFiltered) Color(0xFF00F2FE) else IGTextPrimary
                            )
                        }
                    }

                    if (isFiltered) {
                        Text(
                            text = "Reset",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF453A),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedStatusFilter = "ALL"
                                    selectedMediaTypeFilter = "ALL"
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (isSearchBarActive && hasSearched) {
            // Predictive suggestion pills
            if (displayedPredictions.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "PREDICTIONS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = IGGradMid,
                                letterSpacing = 0.8.sp
                            )
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            items(
                                items = displayedPredictions,
                                key = { it }
                            ) { suggestion ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(IGCard)
                                        .border(1.dp, IGGradMid.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                        .clickable {
                                            searchQuery = suggestion
                                            keyboardController?.hide()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null,
                                            tint = IGGradMid,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = suggestion,
                                            color = IGTextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isSearchingOnline && displayedSearchResults.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = IGGradMid, strokeWidth = 2.5.dp, modifier = Modifier.size(36.dp))
                    }
                }
            } else if (displayedSearchResults.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val catLabel = if (selectedSearchCategory != "ALL") " $selectedSearchCategory" else ""
                            Text("No$catLabel results for \"$searchQuery\"", color = IGTextSecondary, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                            Text("Try another category or search in English/Romaji", color = IGTextTertiary, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val categorySuffix = if (selectedSearchCategory != "ALL") " in $selectedSearchCategory" else ""
                        Text(
                            "${displayedSearchResults.size} results for \"$searchQuery\"$categorySuffix",
                            color = IGTextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (isSearchingOnline) {
                            CircularProgressIndicator(color = IGGradMid, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                items(
                    items = displayedSearchResults,
                    key = { it.id },
                    span = { GridItemSpan(maxLineSpan) }
                ) { result ->
                    val isAlreadySaved = savedIds.contains(result.id)
                    IGSearchResultRow(
                        media = result,
                        isSaved = isAlreadySaved,
                        onAdd = {
                            if (isAlreadySaved) {
                                scope.launch(Dispatchers.IO) {
                                    repository.removeFromLibrary(
                                        media = result,
                                        anilistToken = anilistToken,
                                        tmdbApiKey = apiKey,
                                        tmdbSessionId = tmdbSessionId,
                                        tmdbAccountId = tmdbAccountId
                                    )
                                    launch(Dispatchers.Main) {
                                        Toast.makeText(context, "Removed from Watchlist", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                itemToSaveWithStatus = result
                            }
                        },
                        onOpen = {
                            result.directWebUrl?.let { url ->
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                            }
                        }
                    )
                    IGDividerLine()
                }
            }
        } else if (isSearchBarActive && !hasSearched) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF00F2FE).copy(alpha = 0.45f),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Search across anime, movies, manga & manhwa",
                            fontSize = 13.sp,
                            color = IGTextSecondary
                        )
                    }
                }
            }
        } else {
            // Unified Filters Section (Single Clean Bar for Library)
            item(span = { GridItemSpan(maxLineSpan) }) {
                val isFiltered = selectedStatusFilter != "ALL" || selectedMediaTypeFilter != "ALL"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Primary Unified Filters Button
                            Surface(
                                onClick = { showFilterModal = true },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isFiltered) Color(0xFF00F2FE).copy(alpha = 0.22f) else IGCard,
                                border = BorderStroke(1.dp, if (isFiltered) Color(0xFF00F2FE).copy(alpha = 0.75f) else IGDivider),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Tune,
                                        contentDescription = "Filters",
                                        tint = if (isFiltered) Color(0xFF00F2FE) else IGTextPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (isFiltered) "Filters (Active)" else "Filters ▾",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFiltered) Color(0xFF00F2FE) else IGTextPrimary
                                    )
                                }
                            }

                            // Quick horizontal status pills
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(
                                    listOf("ALL" to "All", "CURRENT" to "Watching", "PLANNING" to "Plan to Watch", "COMPLETED" to "Completed")
                                ) { (code, label) ->
                                    val selected = selectedStatusFilter == code
                                    val chipAccent = when (code) {
                                        "CURRENT" -> Color(0xFF0095F6)
                                        "PLANNING" -> Color(0xFFA855F7)
                                        "COMPLETED" -> Color(0xFF10B981)
                                        else -> Color(0xFF00F2FE)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (selected) chipAccent.copy(alpha = 0.22f) else IGCard)
                                            .border(0.8.dp, if (selected) chipAccent else IGDivider, RoundedCornerShape(16.dp))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedStatusFilter = code
                                            }
                                            .padding(horizontal = 11.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) chipAccent else IGTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        if (isFiltered) {
                            Text(
                                text = "Clear",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF453A),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedStatusFilter = "ALL"
                                        selectedMediaTypeFilter = "ALL"
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Active Media Type indicator pill if filtered
                    if (selectedMediaTypeFilter != "ALL") {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Media Type: ", fontSize = 11.sp, color = IGTextTertiary)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(getMediaTypeColor(selectedMediaTypeFilter).copy(alpha = 0.22f))
                                    .border(0.7.dp, getMediaTypeColor(selectedMediaTypeFilter), RoundedCornerShape(10.dp))
                                    .clickable { selectedMediaTypeFilter = "ALL" }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("${selectedMediaTypeFilter} ✕", fontSize = 10.5.sp, color = getMediaTypeColor(selectedMediaTypeFilter), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (filteredLibrary.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { IGEmptyState(selectedStatusFilter) }
            } else {
                items(
                    items = displayedLibrary,
                    key = { it.id },
                    contentType = { "poster_card" }
                ) { media ->
                    val isSelected = selectedMediaIds.contains(media.id)
                    IGPosterCard(
                        media = media,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onCardClick,
                        onLongClick = { item ->
                            selectedMediaIds = if (selectedMediaIds.contains(item.id)) {
                                selectedMediaIds - item.id
                            } else {
                                selectedMediaIds + item.id
                            }
                        }
                    )
                }

                if (isPageWise) {
                    if (totalPages > 1) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 18.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous Page Button
                                FilledTonalIconButton(
                                    onClick = {
                                        if (safeCurrentPage > 1) {
                                            currentPage = safeCurrentPage - 1
                                            scope.launch { listState.scrollToItem(0) }
                                        }
                                    },
                                    enabled = safeCurrentPage > 1,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = IGCard,
                                        contentColor = IGTextPrimary,
                                        disabledContainerColor = IGCard.copy(alpha = 0.35f),
                                        disabledContentColor = IGTextTertiary
                                    ),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.NavigateBefore, "Previous Page")
                                }

                                Spacer(Modifier.width(14.dp))

                                // Current Page Pill Badge
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = IGCard,
                                    border = BorderStroke(1.dp, IGDivider)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Page ", fontSize = 13.sp, color = IGTextSecondary)
                                        Text("$safeCurrentPage", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = IGGradMid)
                                        Text(" of $totalPages", fontSize = 13.sp, color = IGTextSecondary)
                                        Text(" (${filteredLibrary.size})", fontSize = 11.sp, color = IGTextTertiary)
                                    }
                                }

                                Spacer(Modifier.width(14.dp))

                                // Next Page Button
                                FilledTonalIconButton(
                                    onClick = {
                                        if (safeCurrentPage < totalPages) {
                                            currentPage = safeCurrentPage + 1
                                            scope.launch { listState.scrollToItem(0) }
                                        }
                                    },
                                    enabled = safeCurrentPage < totalPages,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = IGCard,
                                        contentColor = IGTextPrimary,
                                        disabledContainerColor = IGCard.copy(alpha = 0.35f),
                                        disabledContentColor = IGTextTertiary
                                    ),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.NavigateNext, "Next Page")
                                }
                            }
                        }
                    }
                } else {
                    if (displayedLibrary.size < filteredLibrary.size) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            LaunchedEffect(Unit) {
                                displayCount = (displayCount + pageSize).coerceAtMost(filteredLibrary.size)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = IGGradMid,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Scroll-to-Top Floating Button
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 4 }
    }
    AnimatedVisibility(
        visible = showScrollToTop,
        enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 2 } + scaleIn(tween(200)),
        exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 2 } + scaleOut(tween(200)),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 80.dp)
    ) {
        Surface(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            },
            shape = CircleShape,
            color = Color(0xF018181C),
            border = BorderStroke(1.2.dp, Brush.linearGradient(listOf(Color(0xFF00F2FE), Color(0xFF2563EB)))),
            shadowElevation = 12.dp,
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Scroll to top",
                    tint = Color(0xFF00F2FE),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

    // Modal dialog to view and update details/status for a library item
    selectedMediaForDetail?.let { detailItem ->
        val latestMedia = historyList.find { it.id == detailItem.id } ?: detailItem
        MediaDetailDialog(
            media = latestMedia,
            onDismiss = { selectedMediaForDetail = null },
            onStatusChange = { newStatus ->
                if (newStatus == "CURRENT") {
                    progressDialogMedia = latestMedia.copy(userStatus = "CURRENT")
                }
                scope.launch(Dispatchers.IO) {
                    val synced = repository.updateMediaStatus(
                        media = latestMedia,
                        newStatus = newStatus,
                        anilistToken = anilistToken,
                        tmdbApiKey = apiKey,
                        tmdbSessionId = tmdbSessionId,
                        tmdbAccountId = tmdbAccountId
                    )
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, if (synced) "Status updated & synced" else "Status updated", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onTrackProgress = {
                progressDialogMedia = latestMedia
            },
            onOpen = {
                latestMedia.directWebUrl?.let { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                }
            },
            onDelete = {
                selectedMediaForDetail = null
                scope.launch(Dispatchers.IO) {
                    repository.removeFromLibrary(
                        media = latestMedia,
                        anilistToken = anilistToken,
                        tmdbApiKey = apiKey,
                        tmdbSessionId = tmdbSessionId,
                        tmdbAccountId = tmdbAccountId
                    )
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Removed from Library", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Dialog asking for status when saving an item from search
    itemToSaveWithStatus?.let { item ->
        StatusSelectionDialog(
            media = item,
            onDismiss = { itemToSaveWithStatus = null },
            onStatusSelected = { chosenStatus ->
                if (chosenStatus == "CURRENT") {
                    progressDialogMedia = item.copy(userStatus = "CURRENT")
                }
                scope.launch(Dispatchers.IO) {
                    val toSave = item.copy(userStatus = chosenStatus, cachedAt = System.currentTimeMillis())
                    val synced = repository.saveToLibrary(
                        media = toSave,
                        anilistToken = anilistToken,
                        tmdbApiKey = apiKey,
                        tmdbSessionId = tmdbSessionId,
                        tmdbAccountId = tmdbAccountId
                    )
                    launch(Dispatchers.Main) {
                        val statusLabel = when (chosenStatus) {
                            "CURRENT" -> "Watching"
                            "COMPLETED" -> "Completed"
                            "DROPPED" -> "Dropped"
                            else -> "Plan to Watch"
                        }
                        val isAnime = toSave.mediaType.equals("Anime", ignoreCase = true) || toSave.mediaType.equals("Manga", ignoreCase = true)
                        val target = if (isAnime) "AniList" else "TMDB"
                        val msg = if (synced) "Added as $statusLabel & synced to $target!" else "Added \"${toSave.cleanTitle}\" as $statusLabel"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
                itemToSaveWithStatus = null
            }
        )
    }

    // Interactive progress popup for episode or chapter tracking
    progressDialogMedia?.let { media ->
        ProgressTrackingDialog(
            media = media,
            onDismiss = { progressDialogMedia = null },
            onSaveProgress = { m, ep, ch ->
                progressDialogMedia = null
                scope.launch(Dispatchers.IO) {
                    repository.saveToLibrary(
                        media = m.copy(userStatus = "CURRENT", episode = ep, chapter = ch),
                        anilistToken = anilistToken,
                        tmdbApiKey = apiKey,
                        tmdbSessionId = tmdbSessionId,
                        tmdbAccountId = tmdbAccountId
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
                        val isManga = m.mediaType.equals("Manga", true) || m.mediaType.equals("Manhwa", true)
                        val unit = if (isManga) "Ch. ${ch ?: 1}" else "Ep. ${ep ?: 1}"
                        Toast.makeText(context, "Progress updated ($unit)", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Unified Filter Dialog (for both Search and Library)
    if (showFilterModal) {
        Dialog(onDismissRequest = { showFilterModal = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xF2121524),
                border = BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.08f), Color.Transparent))),
                shadowElevation = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00F2FE))
                            )
                            Text(
                                text = "FILTER LIBRARY",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00F2FE),
                                letterSpacing = 0.8.sp
                            )
                        }
                        IconButton(
                            onClick = { showFilterModal = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = IGTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "WATCH STATUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = IGTextSecondary,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(statusFilters, key = { it.first }) { (code, label) ->
                            val selected = selectedStatusFilter == code
                            val chipAccent = when (code) {
                                "CURRENT" -> Color(0xFF0095F6)
                                "PLANNING" -> Color(0xFFA855F7)
                                "COMPLETED" -> Color(0xFF10B981)
                                "DROPPED" -> Color(0xFFEF4444)
                                else -> Color(0xFF00F2FE)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) chipAccent.copy(alpha = 0.24f) else IGCard)
                                    .border(1.dp, if (selected) chipAccent else IGDivider, RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedStatusFilter = code
                                    }
                                    .padding(horizontal = 13.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) chipAccent else IGTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "MEDIA TYPE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = IGTextSecondary,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(mediaTypeFilters, key = { it.first }) { (code, label) ->
                            val selected = selectedMediaTypeFilter == code
                            val typeAccent = if (code == "ALL") Color.White else getMediaTypeColor(code)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) typeAccent.copy(alpha = 0.24f) else IGCard)
                                    .border(1.dp, if (selected) typeAccent else IGDivider, RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedMediaTypeFilter = code
                                    }
                                    .padding(horizontal = 13.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) typeAccent else IGTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = {
                                selectedStatusFilter = "ALL"
                                selectedMediaTypeFilter = "ALL"
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset All", color = Color(0xFFFF453A), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { showFilterModal = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF00F2FE), Color(0xFF0095F6)))),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Apply Filters", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusSelectionDialog(
    media: EnrichedMediaEntity,
    onDismiss: () -> Unit,
    onStatusSelected: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = IGCard,
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
                    color = IGTextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = media.cleanTitle,
                    fontSize = 13.sp,
                    color = IGTextSecondary,
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
                            .background(IGBlack.copy(alpha = 0.5f))
                            .clickable { onStatusSelected(code) }
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
                            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = IGTextPrimary)
                            Text(desc, fontSize = 11.sp, color = IGTextSecondary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = IGTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun FlashyAppLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF00F2FE), // Cyber Cyan
                        Color(0xFF2563EB), // Electric Blue
                        Color(0xFF38EF7D)  // Neon Emerald
                    )
                )
            )
            .padding(1.5.dp)
            .clip(RoundedCornerShape(6.5.dp))
            .background(Color(0xFF080D1A)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(17.dp)) {
            val w = size.width
            val h = size.height

            // Glowing cinematic play prism
            val playPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.28f, h * 0.18f)
                lineTo(w * 0.82f, h * 0.50f)
                lineTo(w * 0.28f, h * 0.82f)
                close()
            }
            drawPath(
                path = playPath,
                brush = Brush.linearGradient(
                    listOf(Color(0xFF00F2FE), Color(0xFF38EF7D))
                )
            )

            // High-tech horizontal scanner laser line
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.White, Color(0xFF00F2FE), Color.Transparent)
                ),
                start = androidx.compose.ui.geometry.Offset(0f, h * 0.5f),
                end = androidx.compose.ui.geometry.Offset(w, h * 0.5f),
                strokeWidth = 1.5.dp.toPx()
            )

            // Optical focal point
            drawCircle(
                color = Color.White,
                radius = 1.5.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(w * 0.48f, h * 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IGTopBar(
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onSearch: () -> Unit,
    onClearAll: () -> Unit,
    showClearAll: Boolean,
    focusRequester: FocusRequester,
    isSearchingOnline: Boolean,
    isCloudSyncing: Boolean = false,
    isPageWise: Boolean = false,
    onTogglePageWise: () -> Unit = {},
    onNavigateToDiscover: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onSync: () -> Unit = {}
) {
    var showOptionsMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().background(IGBlack).statusBarsPadding()) {
        AnimatedContent(
            targetState = isSearchActive,
            transitionSpec = {
                if (targetState) {
                    (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 5 })
                        .togetherWith(fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { -it / 5 })
                } else {
                    (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { -it / 5 })
                        .togetherWith(fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { it / 5 })
                }
            },
            label = "topbar_search_transition",
            modifier = Modifier.fillMaxWidth()
        ) { searchActive ->
            if (searchActive) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search anime, movies, manga, manhwa...", color = IGTextTertiary, fontSize = 14.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = IGCard,
                            unfocusedContainerColor = IGCard,
                            focusedTextColor = IGTextPrimary,
                            unfocusedTextColor = IGTextPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = IGBlue
                        ),
                        leadingIcon = {
                            if (isSearchingOnline) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = IGGradMid)
                            } else {
                                Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp), tint = IGTextSecondary)
                            }
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Outlined.Close, null, tint = IGTextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch() })
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Cancel",
                        color = IGBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onSearchToggle() }
                    )
                    LaunchedEffect(Unit) { focusRequester.requestFocus() }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title and Logo (Guaranteed single line, never wraps vertically)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        FlashyAppLogo()
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "SceneFinder",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF00F2FE), // Cyber Cyan
                                        Color(0xFF38EF7D), // Emerald Neon
                                        Color(0xFFFFFFFF)  // Pure White
                                    )
                                )
                            ),
                            letterSpacing = (-0.3).sp
                        )
                    }

                    // Merged Discover & Settings Action Button with Quick Dropdown
                    Box {
                        IconButton(
                            onClick = { showOptionsMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "Options & Settings",
                                tint = IGTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false },
                            modifier = Modifier
                                .background(IGCard)
                                .border(0.5.dp, IGDivider, RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Explore, contentDescription = null, tint = Color(0xFF00F2FE), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Text("Discover SceneFinder", color = IGTextPrimary, fontSize = 13.5.sp)
                                    }
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    onNavigateToDiscover()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Settings, contentDescription = null, tint = IGTextSecondary, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Text("Settings & Sync", color = IGTextPrimary, fontSize = 13.5.sp)
                                    }
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    onNavigateToSettings()
                                }
                            )
                        }
                    }

                    // Page-Wise / Scroll Toggle
                    IconButton(
                        onClick = onTogglePageWise,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = if (isPageWise) "Switch to Infinite Scroll" else "Switch to Page-Wise Mode",
                            tint = if (isPageWise) Color(0xFF00F2FE) else IGTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Library Sync Button
                    IconButton(
                        onClick = onSync,
                        modifier = Modifier.size(36.dp)
                    ) {
                        if (isCloudSyncing) {
                            CircularProgressIndicator(color = Color(0xFF00F2FE), strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Default.Refresh, "Sync Library", tint = IGTextPrimary, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Search Button
                    IconButton(
                        onClick = onSearchToggle,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Search, "Search", tint = IGTextPrimary, modifier = Modifier.size(21.dp))
                    }
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(0.5.dp).background(IGDivider))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IGPosterCard(
    media: EnrichedMediaEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: (EnrichedMediaEntity) -> Unit,
    onLongClick: (EnrichedMediaEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Fast, lightweight tactile press & selection scale animation
    val targetScale = if (isPressed) 0.94f else if (isSelected) 0.95f else 1.0f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(120),
        label = "poster_scale"
    )

    // Border: static when unselected to avoid allocating animation state per card
    val borderWidth = if (isSelected) 2.5.dp else 0.5.dp
    val borderColor = if (isSelected) IGGradMid else Color(0x22FFFFFF)

    val imageRequest = remember(media.posterUrl) {
        ImageRequest.Builder(context)
            .data(media.posterUrl)
            .size(360, 540)
            .crossfade(140)
            .allowHardware(true)
            .allowRgb565(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .aspectRatio(0.67f)
            .clip(RoundedCornerShape(6.dp))
            .background(IGSurface)
            .border(borderWidth, borderColor, RoundedCornerShape(6.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (isSelectionMode) {
                        onLongClick(media)
                    } else {
                        onClick(media)
                    }
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick(media)
                }
            )
    ) {
        if (!media.posterUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageRequest,
                contentDescription = media.cleanTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize().background(IGCard), contentAlignment = Alignment.Center) {
                Text(media.mediaType.take(1), fontSize = 36.sp, color = IGTextTertiary)
            }
        }

        // Bottom gradient overlay with title, type, and rating
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black.copy(alpha = 0.96f)
                        )
                    )
                )
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                val typeColor = remember(media.mediaType) { getMediaTypeColor(media.mediaType) }
                val ratingStr = remember(media.rating) { media.rating?.let { "%.1f".format(it) } }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Category pill badge with clear high-contrast backing
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(typeColor.copy(alpha = 0.22f))
                                .border(0.5.dp, typeColor.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = media.mediaType.uppercase(),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = typeColor,
                                letterSpacing = 0.4.sp
                            )
                        }

                        // Age Rating badge
                        media.ageRating?.let { age ->
                            AgeRatingBadge(rating = age, fontSize = 8.sp)
                        }
                    }

                    if (ratingStr != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Icon(Icons.Default.Star, null, tint = IGYellow, modifier = Modifier.size(9.dp))
                            Text(" $ratingStr", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = IGYellow)
                        }
                    }
                }

                // Show title with dark frosted background for 100% legibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 3.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = media.cleanTitle,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Selection badge at top-left
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(tween(150)) + scaleIn(tween(150)),
            exit = androidx.compose.animation.fadeOut(tween(150)) + scaleOut(tween(150)),
            modifier = Modifier
                .padding(6.dp)
                .align(Alignment.TopStart)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(IGGradMid),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Status differentiation glassmorphic pill badge at top-right
        if (!isSelected) {
            val statusInfo = remember(media.userStatus) {
                when (media.userStatus) {
                    "CURRENT" -> "Watching" to Color(0xFF0095F6)
                    "PLANNING" -> "Plan" to Color(0xFFA855F7)
                    "COMPLETED" -> "Completed" to Color(0xFF10B981)
                    "DROPPED" -> "Dropped" to Color(0xFFEF4444)
                    else -> null
                }
            }
            if (statusInfo != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.78f),
                    border = BorderStroke(0.5.dp, statusInfo.second.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .padding(5.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusInfo.second)
                        )
                        Text(
                            text = statusInfo.first,
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaDetailDialog(
    media: EnrichedMediaEntity,
    onDismiss: () -> Unit,
    onStatusChange: (String) -> Unit,
    onTrackProgress: () -> Unit = {},
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = IGCard,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header with Poster thumbnail, details, and dedicated Dismiss button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 62.dp, height = 90.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(IGSurface)
                    ) {
                        if (!media.posterUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = media.posterUrl,
                                contentDescription = media.cleanTitle,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(Modifier.fillMaxSize().background(IGCard), contentAlignment = Alignment.Center) {
                                Text(media.mediaType.take(1), fontSize = 28.sp, color = IGTextTertiary)
                            }
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        val typeColor = getMediaTypeColor(media.mediaType)
                        Text(
                            media.mediaType.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeColor,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            media.cleanTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = IGTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            media.year?.let {
                                Text("$it", fontSize = 12.sp, color = IGTextSecondary)
                            }
                            media.rating?.let {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, null, tint = IGYellow, modifier = Modifier.size(11.dp))
                                    Text(" ${String.format("%.1f", it)}", fontSize = 12.sp, color = IGYellow, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            media.ageRating?.let { age ->
                                AgeRatingBadge(rating = age, fontSize = 9.sp)
                            }
                            Text("• ${media.sourceService}", fontSize = 11.sp, color = IGTextTertiary)
                        }
                    }

                    // Prominent (X) button to close the menu conveniently without mis-pressing
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = IGTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                media.overview?.takeIf { it.isNotBlank() }?.let { overview ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        overview,
                        fontSize = 12.sp,
                        color = IGTextSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    "STATUS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = IGTextTertiary,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(8.dp))

                // Convenient status selector rows
                val statusOptions = listOf(
                    Triple("CURRENT", "Watching", Color(0xFF0095F6)),
                    Triple("PLANNING", "Plan to Watch", Color(0xFFA855F7)),
                    Triple("COMPLETED", "Completed", Color(0xFF10B981)),
                    Triple("DROPPED", "Dropped", Color(0xFFEF4444))
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    statusOptions.forEach { (code, label, color) ->
                        val isSelected = media.userStatus == code
                        val rowBg by animateColorAsState(
                            targetValue = if (isSelected) color.copy(alpha = 0.18f) else IGBlack.copy(alpha = 0.45f),
                            animationSpec = tween(200),
                            label = "status_row_bg"
                        )
                        val rowBorder by animateColorAsState(
                            targetValue = if (isSelected) color else IGDivider,
                            animationSpec = tween(200),
                            label = "status_row_border"
                        )
                        val rowTextColor by animateColorAsState(
                            targetValue = if (isSelected) IGTextPrimary else IGTextSecondary,
                            animationSpec = tween(200),
                            label = "status_row_text"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(rowBg)
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = rowBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onStatusChange(code)
                                    if (code == "CURRENT") onTrackProgress()
                                }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
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
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = rowTextColor
                                )
                            }
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn(tween(160)) + scaleIn(tween(160)),
                                exit = fadeOut(tween(120)) + scaleOut(tween(120))
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = color, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Episode / Chapter Progress Tracker Card
                val isEpisodic = media.mediaType.equals("Anime", true) ||
                        media.mediaType.equals("Manga", true) ||
                        media.mediaType.equals("Manhwa", true) ||
                        media.mediaType.contains("TV", true) ||
                        media.mediaType.contains("Series", true)

                if (isEpisodic) {
                    val isMangaType = media.mediaType.equals("Manga", true) || media.mediaType.equals("Manhwa", true)
                    val currentVal = if (isMangaType) media.chapter ?: 0 else media.episode ?: 0
                    val unitName = if (isMangaType) "Chapter" else "Episode"

                    Spacer(Modifier.height(10.dp))
                    Surface(
                        onClick = onTrackProgress,
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0095F6).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF0095F6).copy(alpha = 0.4f)),
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
                                    "CURRENT PROGRESS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0095F6),
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (currentVal > 0) "$unitName $currentVal" else "Not started yet",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IGTextPrimary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0095F6),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    Text("Track", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Action buttons: Open in Web / Remove from Library
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (media.directWebUrl != null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(IGBlue.copy(alpha = 0.15f))
                                .border(1.dp, IGBlue.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .clickable { onOpen() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = IGBlue, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Open Web", color = IGBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(IGRed.copy(alpha = 0.12f))
                            .border(1.dp, IGRed.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .clickable { onDelete() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, null, tint = IGRed, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Remove", color = IGRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = IGTextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun IGSearchResultRow(
    media: EnrichedMediaEntity,
    isSaved: Boolean,
    onAdd: () -> Unit,
    onOpen: () -> Unit
) {
    val rowInteraction = remember { MutableInteractionSource() }
    val rowPressed by rowInteraction.collectIsPressedAsState()
    val rowScale by animateFloatAsState(
        targetValue = if (rowPressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "row_scale"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = rowScale; scaleY = rowScale }
            .clickable(
                interactionSource = rowInteraction,
                indication = null
            ) { onOpen() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 76.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(IGCard)
        ) {
            if (!media.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = media.posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            val typeColor = getMediaTypeColor(media.mediaType)
            Text(media.mediaType.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = typeColor, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(2.dp))
            Text(media.cleanTitle, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = IGTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                media.year?.let { Text("$it", fontSize = 12.sp, color = IGTextSecondary) }
                media.rating?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = IGYellow, modifier = Modifier.size(11.dp))
                        Text(" ${String.format("%.1f", it)}", fontSize = 12.sp, color = IGYellow)
                    }
                }
                media.ageRating?.let { age ->
                    AgeRatingBadge(rating = age, fontSize = 8.5.sp)
                }
                Text("• ${media.sourceService}", fontSize = 12.sp, color = IGTextSecondary)
            }
            media.overview?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 12.sp, color = IGTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        FilledTonalIconButton(
            onClick = onAdd,
            modifier = Modifier.size(36.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (isSaved) IGGreen.copy(alpha = 0.15f) else IGCard,
                contentColor = if (isSaved) IGGreen else IGTextPrimary
            )
        ) {
            Icon(
                if (isSaved) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun IGDividerLine() {
    Box(Modifier.fillMaxWidth().padding(start = 76.dp).height(0.5.dp).background(IGDivider))
}

@Composable
private fun IGEmptyState(filter: String) {
    val (iconText, title, subtitle) = when (filter) {
        "CURRENT"   -> Triple("▶", "Not watching anything", "Find something and set it as 'Watching'")
        "PLANNING"  -> Triple("🔖", "No watchlist yet", "Explore and add titles to your list")
        "COMPLETED" -> Triple("✓", "Nothing completed", "Finish something to see it here")
        "DROPPED"   -> Triple("✕", "Nothing dropped", "Keep exploring new shows and manga!")
        else        -> Triple("📷", "Your library is empty", "Tap the sidebar handle to scan a scene or search above!")
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(iconText, fontSize = 48.sp, color = IGTextSecondary)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = IGTextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = IGTextSecondary, textAlign = TextAlign.Center)
    }
}
