package com.scenefinder.ui.overlay

import android.content.Intent
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.scenefinder.data.local.entity.EnrichedMediaEntity
import com.scenefinder.data.prefs.AppSettings
import com.scenefinder.data.repository.MediaEnrichmentRepository
import com.scenefinder.domain.model.MediaType
import com.scenefinder.domain.parser.CandidateExtractor
import com.scenefinder.ui.components.AgeRatingBadge
import com.scenefinder.ui.theme.SceneFinderTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Handles Android text-selection ACTION_PROCESS_TEXT intent.
 * When the user selects text anywhere in Android and taps "SceneFinder" in the
 * popup toolbar (alongside Copy / Paste / Select All), this activity launches
 * as a translucent floating dialog, searches for the selected text, and presents
 * a rich result card — without ever leaving the current app.
 */
@AndroidEntryPoint
class TextSearchActivity : ComponentActivity() {

    @Inject lateinit var repository: MediaEnrichmentRepository
    @Inject lateinit var settings: AppSettings
    @Inject lateinit var candidateExtractor: CandidateExtractor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedText = intent
            ?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            ?.toString()
            ?.trim()
            ?: run {
                Toast.makeText(this, "No text selected", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

        setContent {
            SceneFinderTheme {
                TextSearchPopup(
                    query = selectedText,
                    repository = repository,
                    settings = settings,
                    candidateExtractor = candidateExtractor,
                    onDismiss = { finish() },
                    onOpenOverlay = { media ->
                        OverlayActivity.show(this, media)
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
    object Loading : SearchState()
    data class Found(val result: EnrichedMediaEntity) : SearchState()
    data class NotFound(val query: String) : SearchState()
    data class Error(val message: String) : SearchState()
}

// ─────────────────────────────────────────────────────────────────────────────
// Root composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TextSearchPopup(
    query: String,
    repository: MediaEnrichmentRepository,
    settings: AppSettings,
    candidateExtractor: CandidateExtractor,
    onDismiss: () -> Unit,
    onOpenOverlay: (EnrichedMediaEntity) -> Unit
) {
    var state by remember { mutableStateOf<SearchState>(SearchState.Loading) }

    LaunchedEffect(query) {
        state = SearchState.Loading
        withContext(Dispatchers.IO) {
            try {
                val apiKey = settings.tmdbApiKey.value
                val candidates = candidateExtractor.extractCandidates(
                    listOf(query), "Text Selection Search"
                )
                val result = candidates.firstNotNullOfOrNull { c ->
                    repository.enrichCandidate(c, apiKey)
                }
                state = if (result != null) SearchState.Found(result)
                else SearchState.NotFound(query)
            } catch (e: Exception) {
                state = SearchState.Error(e.message ?: "Unknown error")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.60f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
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
                    .padding(horizontal = 16.dp, vertical = 32.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E))
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    PopupHeader(query = query, onDismiss = onDismiss)
                    Spacer(modifier = Modifier.height(20.dp))
                    AnimatedContent(
                        targetState = state,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                        label = "search_state"
                    ) { s ->
                        when (s) {
                            is SearchState.Loading -> SearchingIndicator(query)
                            is SearchState.Found -> ResultCard(s.result, onOpenOverlay)
                            is SearchState.NotFound -> NotFoundCard(s.query)
                            is SearchState.Error -> ErrorCard(s.message)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PopupHeader(query: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
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
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "SceneFinder",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = "\"$query\"",
                    fontSize = 11.sp,
                    color = Color(0xFFAAAAAA),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color(0xFF888888)
            )
        }
    }
}

@Composable
private fun SearchingIndicator(query: String) {
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
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F3460).copy(alpha = alpha)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = Color(0xFFFF6B35),
                strokeWidth = 3.dp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Searching for", color = Color(0xFF888888), fontSize = 13.sp)
        Text(
            text = "\"$query\"",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Checking AniList · TMDB · MangaDex…",
            color = Color(0xFF555555),
            fontSize = 11.sp,
            fontStyle = FontStyle.Italic
        )
    }
}

@Composable
private fun ResultCard(
    media: EnrichedMediaEntity,
    onOpenOverlay: (EnrichedMediaEntity) -> Unit
) {
    val typeColor = when (media.mediaType) {
        MediaType.ANIME.name -> Color(0xFF9C27B0)
        MediaType.MANGA.name,
        MediaType.MANHWA.name -> Color(0xFF4CAF50)
        MediaType.MOVIE.name -> Color(0xFF2196F3)
        MediaType.TV_SERIES.name,
        MediaType.TV_SHOWS.name,
        MediaType.WEB_SERIES.name -> Color(0xFFFF9800)
        else -> Color(0xFF607D8B)
    }
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!media.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = media.posterUrl,
                    contentDescription = media.cleanTitle,
                    modifier = Modifier
                        .width(90.dp)
                        .height(130.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(130.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF2A2A4A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = media.mediaType?.take(2)?.uppercase() ?: "??",
                        color = typeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(typeColor.copy(alpha = 0.18f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = media.mediaType ?: "MEDIA",
                        color = typeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = media.cleanTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (!media.ageRating.isNullOrBlank()) {
                    AgeRatingBadge(rating = media.ageRating!!)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (media.rating != null && media.rating!! > 0f) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⭐", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${"%.1f".format(media.rating!! / 10.0)} / 10",
                            color = Color(0xFFFFD700),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        if (!media.overview.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F1A2E))
                    .padding(12.dp)
            ) {
                Text(
                    text = media.overview!!,
                    color = Color(0xFFCCCCCC),
                    fontSize = 12.sp,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFF6B35), Color(0xFFDD2A7B))
                    )
                )
                .clickable { onOpenOverlay(media) }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "View Full Details",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun NotFoundCard(query: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
    ) {
        Text(text = "🔍", fontSize = 40.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "No match found for", color = Color(0xFF888888), fontSize = 13.sp)
        Text(
            text = "\"$query\"",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Try selecting the exact title of the anime, movie, manga, or manhwa.",
            color = Color(0xFF555555),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun ErrorCard(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
    ) {
        Text(text = "⚠️", fontSize = 36.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Search error", color = Color(0xFFFF5252), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            color = Color(0xFF888888),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
