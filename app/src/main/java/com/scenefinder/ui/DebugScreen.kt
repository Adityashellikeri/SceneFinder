package com.scenefinder.ui

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.scenefinder.domain.model.MediaMatchCandidate
import com.scenefinder.domain.session.SessionManager
import com.scenefinder.domain.session.SessionState
import com.scenefinder.service.CircleToSearchLauncher
import com.scenefinder.service.SceneAccessibilityService
import com.scenefinder.ui.onboarding.PermissionHelper
import com.scenefinder.ui.theme.DarkBackground
import com.scenefinder.ui.theme.DarkSurface
import com.scenefinder.ui.theme.DarkSurfaceVariant
import com.scenefinder.ui.theme.NeonAccent
import com.scenefinder.ui.theme.NeonGreen
import com.scenefinder.ui.theme.NeonOrange
import com.scenefinder.ui.theme.NeonRed
import com.scenefinder.ui.theme.TextSecondary
import java.io.File

@Composable
fun DebugScreen(
    sessionManager: SessionManager,
    launcher: CircleToSearchLauncher,
    modifier: Modifier = Modifier
) {
    val state by sessionManager.state.collectAsState()
    val logs by sessionManager.logs.collectAsState()
    val context = LocalContext.current

    val isArmed = state is SessionState.Armed
    val stateBadgeColor = when (state) {
        is SessionState.Armed -> NeonGreen
        is SessionState.Captured -> NeonAccent
        is SessionState.Cancelled -> NeonRed
        is SessionState.TimedOut -> NeonOrange
        else -> TextSecondary
    }
    val stateLabel = when (val s = state) {
        is SessionState.Armed -> "ARMED (Remaining: ${s.remainingSeconds}s)"
        is SessionState.Captured -> "CAPTURED (${s.nodeCount} text nodes, ${s.candidates.size} candidates)"
        is SessionState.Cancelled -> "CANCELLED: ${s.reason}"
        is SessionState.TimedOut -> "TIMED OUT: ${s.message}"
        is SessionState.Disarmed -> "DISARMED"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Column {
                Text(
                    text = "SceneFinder Testing",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonAccent
                )
                Text(
                    text = "Screen Reader & Candidate Extraction Feasibility",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Live Session Control & Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Session State Machine", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                            .border(1.dp, stateBadgeColor, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = stateLabel,
                            color = stateBadgeColor,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isArmed) {
                            Button(
                                onClick = { sessionManager.cancel("User cancelled via UI") },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel Session")
                            }
                        } else {
                            Button(
                                onClick = { sessionManager.arm(45) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Arm Session (45s)", color = Color.Black)
                            }
                        }

                        OutlinedButton(
                            onClick = { sessionManager.disarm() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Force Disarm")
                        }
                    }
                }
            }
        }

        // Live System Permissions Card
        item {
            val hasAccessibility = remember { PermissionHelper.isAccessibilityServiceEnabled(context) }
            val hasOverlay = remember { PermissionHelper.canDrawOverlays(context) }
            val hasNotification = remember { PermissionHelper.areNotificationsEnabled(context) }

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("System Integration & Permissions", fontWeight = FontWeight.SemiBold, color = NeonAccent)
                    Spacer(Modifier.height(8.dp))

                    listOf<Pair<String, Boolean>>(
                        Pair("Accessibility Service", hasAccessibility),
                        Pair("Display Over Other Apps", hasOverlay),
                        Pair("Push Notifications", hasNotification)
                    ).forEach { (name, isGranted) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, fontSize = 13.sp, color = Color.White)
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isGranted) NeonGreen.copy(alpha = 0.2f) else NeonRed.copy(alpha = 0.2f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(1.dp, if (isGranted) NeonGreen else NeonRed, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isGranted) "GRANTED" else "MISSING",
                                    color = if (isGranted) NeonGreen else NeonRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Extracted Candidates (Stage 2 Feature)
        if (state is SessionState.Captured) {
            val captured = state as SessionState.Captured
            if (captured.candidates.isNotEmpty()) {
                item {
                    Text(
                        text = "Extracted Candidates (${captured.candidates.size})",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                items(captured.candidates) { candidate ->
                    CandidateCard(candidate)
                }
            }

            // Raw Dump Share
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Captured Hierarchy Dump", fontWeight = FontWeight.SemiBold, color = NeonAccent)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = captured.sampleText.ifBlank { "No sample preview" },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(10.dp))
                        captured.dumpFilePath?.let { path ->
                            Button(
                                onClick = { shareDumpFile(context, path) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Share Dump File (${File(path).name})")
                            }
                        }
                    }
                }
            }
        }

        // CTS Trigger Feasibility Tools
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Trigger Assist Tests", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val res = launcher.tryActionAssist(context)
                                sessionManager.log("Test ActionAssist: $res")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ACTION_ASSIST")
                        }

                        Button(
                            onClick = {
                                val res = launcher.tryVoiceAssist(context)
                                sessionManager.log("Test VoiceAssist: $res")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("VOICE_ASSIST")
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            SceneAccessibilityService.instance?.captureActiveWindow()
                                ?: sessionManager.log("Service instance null, cannot capture")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Capture Active Window Now (Manual)")
                    }
                }
            }
        }

        // Live Event Logs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Live Logs (${logs.size})", fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { sessionManager.clearLogs() }) {
                    Text("Clear")
                }
            }
        }

        items(logs.reversed()) { logEntry ->
            Surface(
                color = DarkSurfaceVariant,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = logEntry,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun CandidateCard(candidate: MediaMatchCandidate) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = candidate.cleanTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .background(NeonAccent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .border(1.dp, NeonAccent, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = candidate.mediaType.displayName,
                        color = NeonAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                candidate.year?.let {
                    Text("Year: $it", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                candidate.season?.let {
                    Text("Season $it", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                candidate.episode?.let {
                    Text("Episode $it", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                candidate.chapter?.let {
                    Text("Ch. $it", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Text(
                    text = "Conf: ${(candidate.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "Raw: \"${candidate.rawText}\" (${candidate.source})",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}

private fun shareDumpFile(context: Context, filePath: String) {
    val file = File(filePath)
    if (!file.exists()) return
    val uri: Uri = FileProvider.getUriForFile(
        context,
        "com.scenefinder.fileprovider",
        file
    )
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Dump File"))
}
