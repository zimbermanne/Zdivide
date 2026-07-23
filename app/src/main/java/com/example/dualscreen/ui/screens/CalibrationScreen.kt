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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dualscreen.data.AppDatabase
import com.example.dualscreen.data.CalibrationProfile
import com.example.dualscreen.network.CalibrationPayload
import com.example.dualscreen.network.NetworkManager
import com.example.dualscreen.ui.components.DualScreenCanvas
import kotlinx.coroutines.launch

@Composable
fun CalibrationScreen(
    networkManager: NetworkManager,
    database: AppDatabase,
    onBack: () -> Unit
) {
    val role by networkManager.role.collectAsState()
    val config by networkManager.config.collectAsState()
    val calibration by networkManager.calibration.collectAsState()

    val scope = rememberCoroutineScope()
    var profileNameInput by remember { mutableStateOf("Profile 1 (Side-by-Side)") }
    var saveStatusMsg by remember { mutableStateOf("") }

    val savedProfilesFlow = remember { database.calibrationDao().getAllProfiles() }
    val savedProfiles by savedProfilesFlow.collectAsState(initial = emptyList())

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
                    text = "Phase 4: Calibration Wizard",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "Align Bezels, Offsets, Scale & Gap Compensation",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Preview Canvas
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
                activeImageId = config.activeImageId
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calibration Controls Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fine Alignment Parameters",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    IconButton(
                        onClick = {
                            val resetCalib = CalibrationPayload()
                            networkManager.sendCalibration(resetCalib)
                        }
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // X Offset
                Text(
                    text = "Horizontal X Offset: ${calibration.xOffsetDp.toInt()} dp",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Slider(
                    value = calibration.xOffsetDp,
                    onValueChange = { newVal ->
                        networkManager.sendCalibration(calibration.copy(xOffsetDp = newVal))
                    },
                    valueRange = -150f..150f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )

                // Y Offset
                Text(
                    text = "Vertical Y Offset: ${calibration.yOffsetDp.toInt()} dp",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Slider(
                    value = calibration.yOffsetDp,
                    onValueChange = { newVal ->
                        networkManager.sendCalibration(calibration.copy(yOffsetDp = newVal))
                    },
                    valueRange = -150f..150f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )

                // Scale / Zoom Factor
                Text(
                    text = "Scale / Zoom: String.format(\"%.2f\", calibration.scaleFactor)x",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Slider(
                    value = calibration.scaleFactor,
                    onValueChange = { newVal ->
                        networkManager.sendCalibration(calibration.copy(scaleFactor = newVal))
                    },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )

                // Rotation Degrees
                Text(
                    text = "Rotation Correction: ${calibration.rotationDegrees.toInt()}°",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Slider(
                    value = calibration.rotationDegrees,
                    onValueChange = { newVal ->
                        networkManager.sendCalibration(calibration.copy(rotationDegrees = newVal))
                    },
                    valueRange = -45f..45f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )

                // Bezel Gap Compensation
                Text(
                    text = "Physical Bezel Gap Compensation: ${calibration.gapCompensationDp.toInt()} dp",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                )
                Slider(
                    value = calibration.gapCompensationDp,
                    onValueChange = { newVal ->
                        networkManager.sendCalibration(calibration.copy(gapCompensationDp = newVal))
                    },
                    valueRange = 0f..80f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save & Load Profiles Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Save Calibration Profile (Room DB)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = profileNameInput,
                    onValueChange = { profileNameInput = it },
                    label = { Text("Profile Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        scope.launch {
                            val profile = CalibrationProfile(
                                id = "profile_${System.currentTimeMillis()}",
                                profileName = profileNameInput,
                                xOffsetDp = calibration.xOffsetDp,
                                yOffsetDp = calibration.yOffsetDp,
                                scaleFactor = calibration.scaleFactor,
                                rotationDegrees = calibration.rotationDegrees,
                                gapCompensationDp = calibration.gapCompensationDp,
                                splitDirection = config.splitDirection,
                                splitRatio = config.splitRatio,
                                scaleMode = config.scaleMode
                            )
                            database.calibrationDao().saveProfile(profile)
                            saveStatusMsg = "Saved profile '$profileNameInput' to database!"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Calibration Profile", color = Color.White)
                }

                if (saveStatusMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(saveStatusMsg, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary))
                }

                if (savedProfiles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Stored Profiles (${savedProfiles.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    savedProfiles.forEach { prof ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prof.profileName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                                Text(
                                    "X:${prof.xOffsetDp.toInt()} Y:${prof.yOffsetDp.toInt()} Scale:${prof.scaleFactor} Gap:${prof.gapCompensationDp.toInt()}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            Row {
                                Button(
                                    onClick = {
                                        val calib = CalibrationPayload(
                                            xOffsetDp = prof.xOffsetDp,
                                            yOffsetDp = prof.yOffsetDp,
                                            scaleFactor = prof.scaleFactor,
                                            rotationDegrees = prof.rotationDegrees,
                                            gapCompensationDp = prof.gapCompensationDp
                                        )
                                        networkManager.sendCalibration(calib)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Apply", color = Color.White)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        scope.launch { database.calibrationDao().deleteProfile(prof.id) }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
