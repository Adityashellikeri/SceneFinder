package com.scenefinder.domain.model

enum class MediaType {
    ANIME,
    MANGA,
    MANHWA,
    MOVIE,
    TV_SERIES,
    TV_SHOWS,
    WEB_SERIES,
    UNKNOWN;

    val displayName: String
        get() = when (this) {
            ANIME -> "Anime"
            MANGA -> "Manga"
            MANHWA -> "Manhwa"
            MOVIE -> "Movie"
            TV_SERIES -> "TV Shows"
            TV_SHOWS -> "TV Shows"
            WEB_SERIES -> "Web Series"
            UNKNOWN -> "Media"
        }
}
