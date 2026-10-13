package com.scenefinder.domain.parser

import java.util.regex.Pattern

object HeuristicCleaner {

    private val BLOCKED_EXACT_OR_SUBSTRING = setOf(
        // Social Media & Common Apps
        "instagram", "youtube", "tiktok", "reels", "shorts", "facebook", "whatsapp",
        "twitter", "reddit", "telegram", "snapchat", "chrome", "google", "settings",
        "camera", "photos", "gallery", "music", "play store", "gmail", "spotify",
        "system ui", "android", "vivo", "funtouch", "launcher", "home screen",

        // Social Media Controls & Buttons
        "like", "likes", "share", "shares", "comment", "comments", "remix", "subscribe",
        "following", "for you", "follow", "original audio", "audio", "views", "view",
        "send message", "add comment", "bookmark", "save", "saved", "profile", "explore",
        "notifications", "inbox", "search", "direct", "story", "stories", "tagged",
        "reply", "replies", "pin", "pinned", "caption", "sound", "music",

        // Google UI & Lens elements
        "google lens", "lens", "visual matches", "visual match", "search inside image",
        "search with image", "text", "translate", "shopping", "places", "dining", "all",
        "images", "videos", "news", "send feedback", "feedback", "about this result",
        "more results", "more like this", "filters", "filter", "visit", "open in app",
        "copy text", "listen", "select all", "web results", "people also ask",
        "related searches", "overview", "cast", "reviews", "similar"
    )

    private val URL_PATTERN = Pattern.compile("^(https?://|www\\.|[a-zA-Z0-9-]+\\.(com|org|net|io|edu|gov|co|app|tv|me)).*", Pattern.CASE_INSENSITIVE)
    private val TIME_BATTERY_PATTERN = Pattern.compile("^(\\d{1,2}:\\d{2}|\\d{1,3}%|LTE|5G|4G|Wi-Fi|VoLTE|KB/s|MB/s)$", Pattern.CASE_INSENSITIVE)

    private val NOISE_SUFFIXES = listOf(
        " - Wikipedia", " | Wikipedia",
        " - IMDb", " | IMDb",
        " - MyAnimeList", " | MyAnimeList", " - MAL",
        " - AniList", " | AniList",
        " - Crunchyroll", " | Crunchyroll",
        " - Fandom", " | Fandom Wiki", " Wiki",
        " - Rotten Tomatoes", " | Rotten Tomatoes",
        " - Reddit", " | Reddit",
        " - MangaDex", " | MangaDex",
        " - Netflix", " | Netflix",
        " - Prime Video", " | Prime Video",
        " - Disney+", " | Disney+",
        " - YouTube", " | YouTube",
        " : Watch Online", " | Watch Free", " Full HD", " HD"
    )

    fun isJunkText(text: String?): Boolean {
        if (text == null) return true
        val trimmed = text.trim()
        if (trimmed.length < 2) return true
        if (trimmed.length > 80) return true // Reject paragraphs / long captions
        if (trimmed.all { it.isDigit() || it.isWhitespace() || it == '.' || it == ':' || it == '%' }) return true

        val lower = trimmed.lowercase()
        if (BLOCKED_EXACT_OR_SUBSTRING.contains(lower)) return true

        // Reject if matches URL or battery/clock
        if (URL_PATTERN.matcher(trimmed).matches()) return true
        if (TIME_BATTERY_PATTERN.matcher(trimmed).matches()) return true

        // Reject handles starting with @ or #
        if (trimmed.startsWith("@") || trimmed.startsWith("#")) return true

        return false
    }

    fun cleanTitle(raw: String): String {
        var result = raw.trim()

        for (suffix in NOISE_SUFFIXES) {
            if (result.endsWith(suffix, ignoreCase = true)) {
                result = result.substring(0, result.length - suffix.length).trim()
            }
        }

        result = result.replace(Regex("[-:|•]+\\s*$"), "").trim()

        if ((result.startsWith("\"") && result.endsWith("\"")) ||
            (result.startsWith("'") && result.endsWith("'")) ||
            (result.startsWith("“") && result.endsWith("”"))) {
            result = result.substring(1, result.length - 1).trim()
        }

        return result
    }
}
