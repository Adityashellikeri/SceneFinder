package com.scenefinder.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.scenefinder.R
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

private val headerGradient = Brush.horizontalGradient(listOf(IGGradStart, IGGradMid, IGGradEnd))

@Composable
fun OnboardingScreen(
    onContinueToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasAccessibility by remember { mutableStateOf(PermissionHelper.isAccessibilityServiceEnabled(context)) }
    var hasOverlay by remember { mutableStateOf(PermissionHelper.canDrawOverlays(context)) }
    var hasNotification by remember { mutableStateOf(PermissionHelper.areNotificationsEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(context)
                hasOverlay = PermissionHelper.canDrawOverlays(context)
                hasNotification = PermissionHelper.areNotificationsEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val requestNotifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotification = isGranted
    }

    val allGranted = hasAccessibility && hasOverlay && hasNotification

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(IGBlack)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 64.dp)
    ) {
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onContinueToDashboard,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = IGTextPrimary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Discover SceneFinder",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineLarge.copy(brush = headerGradient)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Automated visual identification and enrichment setup",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IGTextSecondary
                )
            }
        }

        // Privacy Notice Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IGSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, IGDivider, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Privacy Guarantee",
                        fontWeight = FontWeight.Bold,
                        color = IGTextPrimary,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.onboarding_privacy),
                        style = MaterialTheme.typography.bodySmall,
                        color = IGTextSecondary
                    )
                }
            }
        }

        // Checklist Header
        item {
            Text(
                text = "SYSTEM PERMISSIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = IGGradMid,
                letterSpacing = 0.8.sp
            )
        }

        // 1. Accessibility Service
        item {
            PermissionCard(
                title = "Accessibility Screen Reader",
                description = "Reads Google search results hierarchy ONLY when you arm a session.",
                isGranted = hasAccessibility,
                buttonLabel = if (hasAccessibility) "Enabled" else "Open Accessibility Settings",
                onAction = { PermissionHelper.openAccessibilitySettings(context) }
            )
        }

        // 2. Overlay Permission
        item {
            PermissionCard(
                title = "Display Over Other Apps",
                description = "Displays the floating match card over your screen when a scene is identified.",
                isGranted = hasOverlay,
                buttonLabel = if (hasOverlay) "Granted" else "Grant Overlay Permission",
                onAction = { PermissionHelper.openOverlaySettings(context) }
            )
        }

        // 3. Notification Permission
        item {
            PermissionCard(
                title = "Listening Indicator Notification",
                description = "Shows the active session countdown banner with quick Cancel button.",
                isGranted = hasNotification,
                buttonLabel = if (hasNotification) "Allowed" else "Allow Notifications",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestNotifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        PermissionHelper.openAppNotificationSettings(context)
                    }
                }
            )
        }

        // Shortcuts & Arming Guidance
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IGSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, IGDivider, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "How to Arm SceneFinder",
                        fontWeight = FontWeight.Bold,
                        color = IGBlue,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "1. Quick Settings Tile: Pull down status bar, tap edit, and add the 'SceneFinder' tile for 1-tap arming.\n2. App Shortcut: Long-press the SceneFinder launcher icon and tap 'Arm SceneFinder'.\n3. Inside App: Use the floating dock on screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = IGTextSecondary
                    )
                }
            }
        }

        // Continue Button
        item {
            val buttonColor by animateColorAsState(
                if (allGranted) IGGradMid else IGCard,
                tween(200),
                label = "btn"
            )
            Button(
                onClick = onContinueToDashboard,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (allGranted) "Continue to Library" else "Continue to Library",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    buttonLabel: String,
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = IGSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, IGDivider, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    color = IGTextPrimary,
                    modifier = Modifier.weight(1f),
                    fontSize = 15.sp
                )

                Box(
                    modifier = Modifier
                        .background(
                            if (isGranted) IGGreen.copy(alpha = 0.15f) else IGRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .border(
                            1.dp,
                            if (isGranted) IGGreen else IGRed,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isGranted) "READY" else "MISSING",
                        color = if (isGranted) IGGreen else IGRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = IGTextSecondary
            )

            Spacer(Modifier.height(12.dp))
            if (isGranted) {
                OutlinedButton(
                    onClick = onAction,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = IGTextSecondary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("✓ $buttonLabel", fontSize = 13.sp)
                }
            } else {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = IGBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(buttonLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
