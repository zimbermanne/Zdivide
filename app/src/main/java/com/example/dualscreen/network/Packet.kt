package com.example.dualscreen.network

import org.json.JSONObject

enum class PacketType {
    HANDSHAKE,
    PING,
    PONG,
    CONFIG_SYNC,
    CALIBRATION_SYNC,
    IMAGE_FRAME,
    VIDEO_SYNC,
    TOUCH_EVENT,
    DISCONNECT
}

data class DualScreenPacket(
    val type: PacketType,
    val senderRole: String, // "HOST" or "CLIENT"
    val timestampMs: Long = System.currentTimeMillis(),
    val payloadJson: String = ""
) {
    fun toJsonString(): String {
        val json = JSONObject()
        json.put("type", type.name)
        json.put("senderRole", senderRole)
        json.put("timestampMs", timestampMs)
        json.put("payloadJson", payloadJson)
        return json.toString()
    }

    companion object {
        fun fromJsonString(str: String): DualScreenPacket? {
            return try {
                val json = JSONObject(str)
                DualScreenPacket(
                    type = PacketType.valueOf(json.getString("type")),
                    senderRole = json.getString("senderRole"),
                    timestampMs = json.getLong("timestampMs"),
                    payloadJson = json.optString("payloadJson", "")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

// Specific payload data structures
data class ConfigPayload(
    val splitDirection: String = "HORIZONTAL",
    val splitRatio: Float = 0.5f,
    val scaleMode: String = "FILL",
    val activeImageId: String = "panoramic"
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("splitDirection", splitDirection)
        json.put("splitRatio", splitRatio)
        json.put("scaleMode", scaleMode)
        json.put("activeImageId", activeImageId)
        return json.toString()
    }

    companion object {
        fun fromJson(str: String): ConfigPayload {
            val json = JSONObject(str)
            return ConfigPayload(
                splitDirection = json.optString("splitDirection", "HORIZONTAL"),
                splitRatio = json.optDouble("splitRatio", 0.5).toFloat(),
                scaleMode = json.optString("scaleMode", "FILL"),
                activeImageId = json.optString("activeImageId", "panoramic")
            )
        }
    }
}

data class CalibrationPayload(
    val xOffsetDp: Float = 0f,
    val yOffsetDp: Float = 0f,
    val scaleFactor: Float = 1.0f,
    val rotationDegrees: Float = 0f,
    val gapCompensationDp: Float = 0f
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("xOffsetDp", xOffsetDp)
        json.put("yOffsetDp", yOffsetDp)
        json.put("scaleFactor", scaleFactor)
        json.put("rotationDegrees", rotationDegrees)
        json.put("gapCompensationDp", gapCompensationDp)
        return json.toString()
    }

    companion object {
        fun fromJson(str: String): CalibrationPayload {
            val json = JSONObject(str)
            return CalibrationPayload(
                xOffsetDp = json.optDouble("xOffsetDp", 0.0).toFloat(),
                yOffsetDp = json.optDouble("yOffsetDp", 0.0).toFloat(),
                scaleFactor = json.optDouble("scaleFactor", 1.0).toFloat(),
                rotationDegrees = json.optDouble("rotationDegrees", 0.0).toFloat(),
                gapCompensationDp = json.optDouble("gapCompensationDp", 0.0).toFloat()
            )
        }
    }
}

data class VideoSyncPayload(
    val isPlaying: Boolean,
    val positionMs: Long,
    val playbackSpeed: Float = 1.0f
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("isPlaying", isPlaying)
        json.put("positionMs", positionMs)
        json.put("playbackSpeed", playbackSpeed)
        return json.toString()
    }

    companion object {
        fun fromJson(str: String): VideoSyncPayload {
            val json = JSONObject(str)
            return VideoSyncPayload(
                isPlaying = json.optBoolean("isPlaying", false),
                positionMs = json.optLong("positionMs", 0L),
                playbackSpeed = json.optDouble("playbackSpeed", 1.0).toFloat()
            )
        }
    }
}

data class TouchPayload(
    val action: String, // "DOWN", "MOVE", "UP"
    val xRatio: Float,   // 0.0 to 1.0
    val yRatio: Float,   // 0.0 to 1.0
    val pointerId: Int = 0
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("action", action)
        json.put("xRatio", xRatio)
        json.put("yRatio", yRatio)
        json.put("pointerId", pointerId)
        return json.toString()
    }

    companion object {
        fun fromJson(str: String): TouchPayload {
            val json = JSONObject(str)
            return TouchPayload(
                action = json.optString("action", "MOVE"),
                xRatio = json.optDouble("xRatio", 0.0).toFloat(),
                yRatio = json.optDouble("yRatio", 0.0).toFloat(),
                pointerId = json.optInt("pointerId", 0)
            )
        }
    }
}
