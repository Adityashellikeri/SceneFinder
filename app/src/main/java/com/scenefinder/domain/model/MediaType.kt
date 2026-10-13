package com.scenefinder.domain.model

enum class MediaType {
    ANIME,
    MANGA,
    MOVIE,
    TV_SERIES,
    TV_SHOWS,
    UNKNOWN;

    val displayName: String
        get() = when (this) {
            ANIME -> "Anime"
            MANGA -> "Manga"
            MOVIE -> "Movie"
            TV_SERIES -> "TV Shows"
            TV_SHOWS -> "TV Shows"
            UNKNOWN -> "Media"
        }
}
