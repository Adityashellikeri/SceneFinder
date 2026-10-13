package com.scenefinder.ui.history

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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
import androidx.compose.material3.CircularProgressIndicator
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
    "COMPLETED" -> IGGreen
    "DROPPED" -> IGRed
    else -> IGTextTertiary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    dao: EnrichedMediaDao,
    repository: MediaEnrichmentRepository,
    settings: AppSettings,
    modifier: Modifier = Modifier,
    startInSearch: Boolean = false,
    onBackToHome: () -> Unit = {}
) {
    val historyList by dao.getAllHistory().collectAsState(initial = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    var itemToSaveWithStatus by remember { mutableStateOf<EnrichedMediaEntity?>(null) }
    var selectedMediaForDetail by remember { mutableStateOf<EnrichedMediaEntity?>(null) }
    val onCardClick = remember { { item: EnrichedMediaEntity -> selectedMediaForDetail = item } }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchingOnline by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<EnrichedMediaEntity>>(emptyList()) }
    var predictedSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }
    var isSearchBarActive by remember { mutableStateOf(startInSearch) }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var selectedMediaTypeFilter by remember { mutableStateOf("ALL") }

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

    // Handle back button on navigation: return to previous place or home page
    BackHandler(enabled = isSearchBarActive || searchQuery.isNotEmpty()) {
        if (searchQuery.isNotEmpty()) {
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

    val pageSize = 24
    var displayCount by remember(selectedStatusFilter, selectedMediaTypeFilter) { mutableIntStateOf(pageSize) }
    val displayedLibrary = remember(filteredLibrary, displayCount) {
        filteredLibrary.take(displayCount)
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

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = listState,
        modifier = modifier.fillMaxSize().background(IGBlack),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // Top bar
        item(span = { GridItemSpan(maxLineSpan) }) {
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
                onSync = ::triggerCloudSync
            )
        }

        // Categorical options bar for Search: Anime, Manga, Movie, Manhwa, Web Series
        if (isSearchBarActive) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    items(searchCategories) { (code, label) ->
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
                            items(displayedPredictions) { suggestion ->
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
        } else {
            // Status filter chips
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                ) {
                    items(statusFilters) { (code, label) ->
                        val selected = selectedStatusFilter == code
                        val bgColor by animateColorAsState(if (selected) IGTextPrimary else IGCard, tween(200), label = "c")
                        val textColor by animateColorAsState(if (selected) IGBlack else IGTextSecondary, tween(200), label = "t")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(bgColor)
                                .clickable { selectedStatusFilter = code }
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Text(label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = textColor)
                        }
                    }
                }
            }

            // Media type filter chips
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    items(mediaTypeFilters) { (code, label) ->
                        val selected = selectedMediaTypeFilter == code
                        val chipColor = if (selected) { if (code == "ALL") IGTextPrimary else getMediaTypeColor(code) } else IGCard
                        val chipBorder = if (selected) Color.Transparent else IGDivider
                        val chipTextColor by animateColorAsState(if (selected) { if (code == "ALL") IGBlack else Color.White } else IGTextSecondary, tween(200), label = "mt")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(chipColor)
                                .border(1.dp, chipBorder, RoundedCornerShape(16.dp))
                                .clickable { selectedMediaTypeFilter = code }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = chipTextColor
                            )
                        }
                    }
                }
            }

            if (filteredLibrary.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { IGEmptyState(selectedStatusFilter) }
            } else {
                items(
                    items = displayedLibrary,
                    key = { it.id }
                ) { media ->
                    IGPosterCard(
                        media = media,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onCardClick
                    )
                }

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

    // Modal dialog to view and update details/status for a library item
    selectedMediaForDetail?.let { detailItem ->
        val latestMedia = historyList.find { it.id == detailItem.id } ?: detailItem
        MediaDetailDialog(
            media = latestMedia,
            onDismiss = { selectedMediaForDetail = null },
            onStatusChange = { newStatus ->
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
                            "CURRENT" -> Color(0xFF4CAF50)
                            "PLANNING" -> Color(0xFF2196F3)
                            "COMPLETED" -> Color(0xFF9C27B0)
                            else -> Color(0xFFE91E63)
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
    onSync: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth().background(IGBlack).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSearchActive) {
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
            } else {
                Text(
                    text = "SceneFinder",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium.copy(brush = storyRingBrushHoriz),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onSync) {
                    if (isCloudSyncing) {
                        CircularProgressIndicator(color = IGGradMid, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.Refresh, "Sync Library", tint = IGTextPrimary, modifier = Modifier.size(22.dp))
                    }
                }
                IconButton(onClick = onSearchToggle) {
                    Icon(Icons.Default.Search, "Search", tint = IGTextPrimary, modifier = Modifier.size(24.dp))
                }
                if (showClearAll) {
                    IconButton(onClick = onClearAll) {
                        Icon(Icons.Default.Delete, "Clear", tint = IGTextSecondary, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(IGDivider))
    }
}

@Composable
private fun IGPosterCard(
    media: EnrichedMediaEntity,
    onClick: (EnrichedMediaEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(media.posterUrl) {
        ImageRequest.Builder(context)
            .data(media.posterUrl)
            .size(360, 540)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Box(
        modifier = modifier
            .aspectRatio(0.67f)
            .clip(RoundedCornerShape(3.dp))
            .background(IGSurface)
            .clickable { onClick(media) }
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

        // Bottom gradient overlay with title and rating
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(posterGradientOverlay)
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Column {
                val typeColor = remember(media.mediaType) { getMediaTypeColor(media.mediaType) }
                val ratingStr = remember(media.rating) { media.rating?.let { "%.1f".format(it) } }
                Text(
                    media.mediaType.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = typeColor,
                    letterSpacing = 0.5.sp
                )
                Text(
                    media.cleanTitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IGTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (ratingStr != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = IGYellow, modifier = Modifier.size(9.dp))
                        Text(" $ratingStr", fontSize = 9.sp, color = IGYellow)
                    }
                }
            }
        }

        // Status indicator dot at top-right
        val statusColor = remember(media.userStatus) { getStatusColor(media.userStatus) }
        Box(
            modifier = Modifier
                .padding(6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(statusColor)
                .align(Alignment.TopEnd)
        )
    }
}

@Composable
private fun MediaDetailDialog(
    media: EnrichedMediaEntity,
    onDismiss: () -> Unit,
    onStatusChange: (String) -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = IGCard,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
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
                    Triple("CURRENT", "Watching", Color(0xFF4CAF50)),
                    Triple("PLANNING", "Plan to Watch", Color(0xFF2196F3)),
                    Triple("COMPLETED", "Completed", Color(0xFF9C27B0)),
                    Triple("DROPPED", "Dropped", Color(0xFFE91E63))
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    statusOptions.forEach { (code, label, color) ->
                        val isSelected = media.userStatus == code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) color.copy(alpha = 0.18f) else IGBlack.copy(alpha = 0.45f))
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) color else IGDivider,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onStatusChange(code) }
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
                                    color = if (isSelected) IGTextPrimary else IGTextSecondary
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = color, modifier = Modifier.size(16.dp))
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
                                Icon(Icons.Default.OpenInNew, null, tint = IGBlue, modifier = Modifier.size(15.dp))
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
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
