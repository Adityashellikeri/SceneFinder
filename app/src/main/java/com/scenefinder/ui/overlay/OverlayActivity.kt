package com.scenefinder.ui.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.scenefinder.ui.theme.DarkSurface
import com.scenefinder.ui.theme.DarkSurfaceVariant
import com.scenefinder.ui.theme.NeonAccent
import com.scenefinder.ui.theme.NeonGreen
import com.scenefinder.ui.theme.NeonOrange
import com.scenefinder.ui.theme.SceneFinderTheme
import com.scenefinder.ui.components.AgeRatingBadge
import com.scenefinder.ui.theme.TextSecondary

import android.widget.Toast
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.scenefinder.data.local.dao.EnrichedMediaDao
import com.scenefinder.data.local.entity.EnrichedMediaEntity
import com.scenefinder.data.prefs.AppSettings
import com.scenefinder.data.repository.MediaEnrichmentRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class OverlayActivity : ComponentActivity() {

    @Inject
    lateinit var repository: MediaEnrichmentRepository

    @Inject
    lateinit var settings: AppSettings

    @Inject
    lateinit var dao: EnrichedMediaDao

    companion object {
        private var pendingMedia: EnrichedMediaEntity? = null

        fun show(context: Context, media: EnrichedMediaEntity) {
            pendingMedia = media
            val intent = Intent(context, OverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val media = pendingMedia

        if (media == null) {
            finish()
            return
        }

        setContent {
            SceneFinderTheme {
                val scope = rememberCoroutineScope()
                val anilistToken by settings.anilistToken.collectAsState()
                val apiKey by settings.tmdbApiKey.collectAsState()
                val tmdbSessionId by settings.tmdbSessionId.collectAsState()
                val tmdbAccountId by settings.tmdbAccountId.collectAsState()

                var isSaved by remember { mutableStateOf(false) }
                var showStatusDialog by remember { mutableStateOf(false) }

                LaunchedEffect(media.id) {
                    withContext(Dispatchers.IO) {
                        val existing = dao.getById(media.id)
                        isSaved = existing != null
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            finish()
                        },
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { /* Do not propagate click outside */ }
                    ) {
                        OverlayMediaCard(
                            media = media,
                            isSaved = isSaved,
                            onToggleWatchlist = {
                                if (isSaved) {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            repository.removeFromLibrary(
                                                media = media,
                                                anilistToken = anilistToken,
                                                tmdbApiKey = apiKey,
                                                tmdbSessionId = tmdbSessionId,
                                                tmdbAccountId = tmdbAccountId
                                            )
                                        }
                                        isSaved = false
                                        Toast.makeText(this@OverlayActivity, "Removed from Watchlist", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    showStatusDialog = true
                                }
                            },
                            onOpenWeb = {
                                media.directWebUrl?.let { url ->
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    startActivity(browserIntent)
                                }
                                finish()
                            },
                            onDismiss = { finish() }
                        )

                        if (showStatusDialog) {
                            Dialog(onDismissRequest = { showStatusDialog = false }) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = DarkSurface,
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
                                            color = Color.White
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = media.cleanTitle,
                                            fontSize = 13.sp,
                                            color = TextSecondary,
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
                                                    .background(Color.Black.copy(alpha = 0.5f))
                                                    .clickable {
                                                        showStatusDialog = false
                                                        scope.launch {
                                                            val toSave = media.copy(userStatus = code, cachedAt = System.currentTimeMillis())
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
                                                            val isAnime = toSave.mediaType.equals("Anime", ignoreCase = true) || toSave.mediaType.equals("Manga", ignoreCase = true)
                                                            val target = if (isAnime) "AniList" else "TMDB"
                                                            val msg = if (synced) "Saved as $statusLabel & synced to $target!" else "Saved as $statusLabel"
                                                            Toast.makeText(this@OverlayActivity, msg, Toast.LENGTH_SHORT).show()
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
                                                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                                    Text(desc, fontSize = 11.sp, color = TextSecondary)
                                                }
                                            }
                                            Spacer(Modifier.height(8.dp))
                                        }

                                        Spacer(Modifier.height(4.dp))
                                        TextButton(onClick = { showStatusDialog = false }, modifier = Modifier.fillMaxWidth()) {
                                            Text("Cancel", color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OverlayMediaCard(
    media: EnrichedMediaEntity,
    isSaved: Boolean,
    onToggleWatchlist: () -> Unit,
    onOpenWeb: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, NeonAccent, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!media.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = media.posterUrl,
                        contentDescription = media.cleanTitle,
                        modifier = Modifier
                            .size(width = 65.dp, height = 90.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(NeonAccent.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = media.mediaType,
                                color = NeonAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        media.ageRating?.let { age ->
                            AgeRatingBadge(rating = age, fontSize = 9.sp)
                        }

                        media.rating?.let { score ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating",
                                    tint = NeonOrange,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = String.format("%.1f", score),
                                    fontSize = 11.sp,
                                    color = NeonOrange,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = media.cleanTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val extraDetails = listOfNotNull(
                        media.year?.toString(),
                        media.season?.let { "S$it" },
                        media.episode?.let { "EP$it" },
                        media.chapter?.let { "Ch.$it" },
                        media.sourceService
                    ).joinToString(" • ")

                    if (extraDetails.isNotBlank()) {
                        Text(
                            text = extraDetails,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            if (!media.overview.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = media.overview,
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add to Watchlist button
                Button(
                    onClick = onToggleWatchlist,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSaved) NeonGreen.copy(alpha = 0.2f) else NeonAccent,
                        contentColor = if (isSaved) NeonGreen else Color.Black
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
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
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = onOpenWeb,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "View",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Dismiss", color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}
