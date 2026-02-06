package com.example.callfeedback.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserFeedback(
    val voiceQuality: Int? = null,
    val audioIssues: List<AudioIssue>? = null,
    val environment: Environment? = null,
    val comment: String? = null,
    // Network & Device metadata
    val networkGeneration: String? = null,  // WiFi, 2G, 3G, 4G, 5G, etc.
    val signalStrength: Int? = null,        // Signal strength in dBm
    val latitude: Double? = null,           // Latitude of the device
    val longitude: Double? = null,          // Longitude of the device
    val timestamp: Long? = null             // Unix timestamp in milliseconds
) {
    init {
        require(voiceQuality == null || (voiceQuality in 1..5)) {
            "Voice quality must be between 1 and 5"
        }
    }
}

@Serializable
enum class AudioIssue {
    CALL_DROPPED,
    COULD_NOT_HEAR_OTHER,
    OTHER_COULD_NOT_HEAR_ME,
    BACKGROUND_NOISE,
    ECHO
}

@Serializable
enum class Environment {
    INDOOR,
    OUTDOOR,
    IN_VEHICLE,
    NOISY_AREA
}
