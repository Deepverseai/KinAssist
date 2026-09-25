package com.kinassist.app.core.webrtc

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class PointerType {
    ARROW_PULSE,        // Animated arrow pointing with expanding ripple waves
    DOODLE_CIRCLE,      // Freehand drawing circle around a target button
    STEP_BADGE,         // Numbered marker ("1", "2", "3")
    RESCUE_ACTION       // Soft Accessibility action: BACK, HOME, NOTIFICATIONS
}

@Serializable
enum class RescueCommand {
    GLOBAL_BACK,
    GLOBAL_HOME,
    GLOBAL_RECENTS,
    GLOBAL_NOTIFICATIONS,
    NONE
}

@Serializable
data class PointerEvent(
    val type: PointerType,
    val xRatio: Float = 0f,         // Normalized horizontal position (0.0 to 1.0)
    val yRatio: Float = 0f,         // Normalized vertical position (0.0 to 1.0)
    val colorHex: String = "#58E7AA", // Emerald glow or Gold accent
    val stepNumber: Int = 1,
    val rescueCommand: RescueCommand = RescueCommand.NONE,
    val durationMs: Long = 3500L,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): String = jsonFormat.encodeToString(this)

    companion object {
        private val jsonFormat = Json { ignoreUnknownKeys = true }
        fun fromJson(jsonStr: String): PointerEvent? {
            return try {
                jsonFormat.decodeFromString(jsonStr)
            } catch (e: Exception) {
                null
            }
        }
    }
}

@Serializable
data class SeniorTelemetry(
    val batteryPct: Int,
    val isCharging: Boolean,
    val volumeLevelPct: Int,
    val isDndActive: Boolean,
    val wifiSsid: String,
    val signalStrength: Int
)
