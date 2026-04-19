package com.example.callfeedback.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserFeedback(
    val voiceQuality: Int? = null,
    val audioIssues: List<AudioIssue>? = null,
    val environment: Environment? = null,
    val comment: String? = null,
    val carrier: String? = null,
    val networkGeneration: String? = null,
    val signalStrength: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long? = null,
    val callDuration: Long? = null

) {
    init {
        require(voiceQuality == null || (voiceQuality in 1..5)) {
            "Voice quality must be between 1 and 5"
        }
    }
}

@Serializable
enum class AudioIssue {
    CALL_PERFECT,
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
