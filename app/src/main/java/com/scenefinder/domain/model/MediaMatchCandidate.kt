package com.scenefinder.domain.model

data class MediaMatchCandidate(
    val rawText: String,
    val cleanTitle: String,
    val year: Int? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val chapter: Int? = null,
    val volume: Int? = null,
    val mediaType: MediaType = MediaType.UNKNOWN,
    val confidence: Float = 0.5f,
    val source: String = "Accessibility"
)
