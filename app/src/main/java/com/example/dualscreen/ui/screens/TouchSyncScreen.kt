package com.example.dualscreen.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dualscreen.network.DeviceRole
import com.example.dualscreen.network.NetworkManager
import com.example.dualscreen.ui.components.DualScreenCanvas

@Composable
fun TouchSyncScreen(
    networkManager: NetworkManager,
    onBack: () -> Unit
) {
    val role by networkManager.role.collectAsState()
    val config by networkManager.config.collectAsState()
    val calibration by networkManager.calibration.collectAsState()
    val latestTouch by networkManager.latestTouch.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Navigation
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Phase 7: Touch Forwarding Feasibility",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "Interactive MotionEvent Capture & Accessibility Research",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive Touch Canvas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            DualScreenCanvas(
                role = role,
                config = config,
                calibration = calibration,
                showAlignmentGrid = true,
                activeImageId = "grid",
                touchPointer = latestTouch,
                onTouchEvent = { action, xRatio, yRatio ->
                    networkManager.sendTouchEvent(action, xRatio, yRatio)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Touch Monitor
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Touch Event Telemetry",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                latestTouch?.let { touch ->
                    Text(
                        text = "Last Event: ${touch.action} | Normalized X: String.format(\"%.3f\", touch.xRatio) | Y: String.format(\"%.3f\", touch.yRatio)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    )
                } ?: run {
                    Text(
                        text = "Tap or drag anywhere on the canvas above to test touch event forwarding across devices.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Phase 7 Research Findings
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Phase 7 Research Findings & Android APIs",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Q: Can Accessibility Services inject touches?\n" +
                            "A: YES. `AccessibilityService.dispatchGesture()` allows programmatic tap/swipe gesture injection without root, requiring user consent in Accessibility settings.\n\n" +
                            "Q: Can MotionEvents be recreated?\n" +
                            "A: YES. `MotionEvent.obtain()` recreates down/move/up events with exact normalized coordinates for inside-app interactions.\n\n" +
                            "Q: Can root improve accuracy?\n" +
                            "A: YES. Root access grants direct access to `/dev/input/event*` devices, offering sub-millisecond touch injection across system UI and games.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    }
}
