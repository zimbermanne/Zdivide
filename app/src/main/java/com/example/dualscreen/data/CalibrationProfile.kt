package com.example.dualscreen.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calibration_profiles")
data class CalibrationProfile(
    @PrimaryKey val id: String = "default_profile",
    val profileName: String = "Default Alignment",
    val xOffsetDp: Float = 0f,
    val yOffsetDp: Float = 0f,
    val scaleFactor: Float = 1.0f,
    val rotationDegrees: Float = 0f,
    val gapCompensationDp: Float = 0f,
    val splitDirection: String = "HORIZONTAL", // "HORIZONTAL" or "VERTICAL"
    val splitRatio: Float = 0.5f,              // 0.1 to 0.9
    val isClientRightSide: Boolean = true,     // Client renders Right or Bottom half
    val scaleMode: String = "FILL"              // "FIT", "FILL", "STRETCH"
)
