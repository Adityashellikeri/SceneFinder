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
        "reply", "replies", "pin", "pinned", "caption", "sound",

        // Google UI & Lens elements
        "google lens", "lens", "visual matches", "visual match", "search inside image",
        "search with image", "text", "translate", "shopping", "places", "dining", "all",
        "images", "videos", "news", "send feedback", "feedback", "about this result",
        "more results", "more like this", "filters", "filter", "visit", "open in app",
        "copy text", "listen", "select all", "web results", "people also ask",
        "related searches", "overview", "cast", "reviews", "similar"
    )

    // Common conversational/dialogue words -- if a line is dominated by these it is a subtitle
    private val DIALOGUE_PRONOUNS = setOf(
        "i", "you", "we", "they", "me", "us", "him", "her", "them",
        "my", "your", "our", "their", "his", "its",
        "im", "youre", "were", "theyre", "ive", "youve",
        "ill", "youll", "well", "theyll", "id", "youd", "wed",
        "dont", "cant", "wont", "isnt", "arent", "wasnt", "werent",
        "doesnt", "didnt", "shouldnt", "couldnt", "wouldnt", "mustnt",
        "what", "why", "how", "where", "when", "who", "which",
        "gonna", "wanna", "gotta", "lemme", "gimme", "yeah", "yes", "no",
        "hey", "oh", "ah", "well", "just", "really", "actually", "maybe",
        "please", "sorry", "thank", "thanks", "okay", "ok", "right",
        "know", "think", "said", "told", "look", "wait", "stop", "help",
        "come", "go", "get", "let", "want", "need", "have", "got"
    )

    private val URL_PATTERN = Pattern.compile(
        "^(https?://|www\\.|[a-zA-Z0-9-]+\\.(com|org|net|io|edu|gov|co|app|tv|me)).*",
        Pattern.CASE_INSENSITIVE
    )
    private val TIME_BATTERY_PATTERN = Pattern.compile(
        "^(\\d{1,2}:\\d{2}|\\d{1,3}%|LTE|5G|4G|Wi-Fi|VoLTE|KB/s|MB/s)$",
        Pattern.CASE_INSENSITIVE
    )

    // Pattern for player / episode header: "Title S01E03" or "Title Season 2 Episode 4"
    private val PLAYER_TITLE_PATTERN = Pattern.compile(
        "^(.+?)\\s+[Ss](\\d{1,2})[Ee](\\d{1,2})",
        Pattern.CASE_INSENSITIVE
    )
    private val SEASON_EPISODE_INLINE = Pattern.compile(
        "(?i)\\b(?:season|s)\\s*\\d+\\b|\\b(?:episode|ep|e)\\s*\\d+\\b|\\b(?:chapter|ch)\\s*\\d+"
    )

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
        " - Apple TV+", " | Apple TV+",
        " - Hotstar", " | Hotstar",
        " - HBO Max", " | HBO Max",
        " - YouTube", " | YouTube",
        " : Watch Online", " | Watch Free", " Full HD", " HD"
    )

    fun isJunkText(text: String?): Boolean {
        if (text == null) return true
        val trimmed = text.trim()
        if (trimmed.length < 2) return true
        if (trimmed.length > 80) return true
        if (trimmed.all { it.isDigit() || it.isWhitespace() || it == '.' || it == ':' || it == '%' }) return true

        val lower = trimmed.lowercase()
        if (BLOCKED_EXACT_OR_SUBSTRING.contains(lower)) return true

        if (URL_PATTERN.matcher(trimmed).matches()) return true
        if (TIME_BATTERY_PATTERN.matcher(trimmed).matches()) return true

        if (trimmed.startsWith("@") || trimmed.startsWith("#")) return true

        // Dialogue filter 1: ends with sentence-ending punctuation typical of spoken lines
        if (trimmed.endsWith("?") || trimmed.endsWith("!") || trimmed.endsWith("...")) return true

        // Dialogue filter 2: first word is a pronoun / dialogue filler
        val stripped = lower.replace(Regex("[^a-z0-9 ]"), " ").trim()
        val words = stripped.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.isEmpty()) return true

        val firstWord = words.first()
        if (firstWord in DIALOGUE_PRONOUNS) return true

        // Dialogue filter 3: majority of words in short line are dialogue words
        if (words.size in 2..7) {
            val dialogueCount = words.count { it in DIALOGUE_PRONOUNS }
            if (dialogueCount.toFloat() / words.size >= 0.50f) return true
        }

        return false
    }

    fun cleanTitle(raw: String): String {
        var result = raw.trim()

        for (suffix in NOISE_SUFFIXES) {
            if (result.endsWith(suffix, ignoreCase = true)) {
                result = result.substring(0, result.length - suffix.length).trim()
            }
        }

        result = result.replace(Regex("[-:|]+\\s*$"), "").trim()

        if ((result.startsWith("\"") && result.endsWith("\"")) ||
            (result.startsWith("'") && result.endsWith("'"))
        ) {
            result = result.substring(1, result.length - 1).trim()
        }

        return result
    }

    /**
     * Extract title portion from a player/notification bar line like:
     * "Attack on Titan S04E28 - The Dawn of Humanity" -> "Attack on Titan"
     * Returns null if the line does not match a known player title pattern.
     */
    fun extractPlayerTitle(line: String): String? {
        val m = PLAYER_TITLE_PATTERN.matcher(line.trim())
        return if (m.find()) m.group(1)?.trim()?.takeIf { it.length >= 2 } else null
    }

    /** Returns true if the line contains a season/episode/chapter marker */
    fun hasEpisodeMarker(line: String): Boolean = SEASON_EPISODE_INLINE.matcher(line).find()
}
