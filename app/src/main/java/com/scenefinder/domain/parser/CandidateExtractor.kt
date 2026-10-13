package com.scenefinder.domain.parser

import com.scenefinder.domain.model.MediaMatchCandidate
import com.scenefinder.domain.model.MediaType
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CandidateExtractor @Inject constructor() {

    private val yearPattern = Pattern.compile("\\b(19\\d\\d|20\\d\\d)\\b")
    private val seasonPattern = Pattern.compile("(?i)(?:season|s)\\s*(\\d+)")
    private val episodePattern = Pattern.compile("(?i)(?:episode|ep|e)\\s*(\\d+)")
    private val chapterPattern = Pattern.compile("(?i)(?:chapter|ch)\\s*(\\d+)")
    private val volumePattern = Pattern.compile("(?i)(?:volume|vol|v)\\s*(\\d+)")

    // Explicit keyword lists for each media type -- ordered from most-specific to least
    private val animeKeywords = listOf(
        "anime", "animation", "subbed", "dubbed", "ova", "ona", "oav",
        "studio ghibli", "toei animation", "mappa", "ufotable", "a-1 pictures",
        "wit studio", "trigger studio", "madhouse", "kyoto animation", "bones studio",
        "shonen", "shounen", "isekai", "mecha", "ecchi", "seinen"
    )
    private val mangaKeywords = listOf(
        "manga", "manhwa", "manhua", "webtoon", "light novel", "ln", "tankobon",
        "scanlation", "raw manga", "chapter"
    )
    private val webSeriesKeywords = listOf(
        "netflix original", "prime original", "amazon original", "disney+ original",
        "apple tv+ original", "hotstar original", "hbo original", "peacock original",
        "zee5 original", "sonyliv original", "voot original",
        "web series", "streaming series", "digital series", "ott series",
        "netflix series", "prime series", "hulu series"
    )
    private val tvKeywords = listOf(
        "tv series", "tv show", "television series", "broadcast series",
        "kdrama", "k-drama", "drama series", "bbc series", "nbc", "cbs", "abc series",
        "network series", "cable series", "hbo", "showtime", "fx series"
    )
    private val movieKeywords = listOf(
        "movie", "film", "motion picture", "feature film", "cinema",
        "theatrical", "box office", "blockbuster"
    )

    // Streaming service names that indicate a streaming show (could be Web Series)
    private val streamingServices = listOf(
        "netflix", "prime video", "amazon prime", "disney+", "apple tv+",
        "hulu", "hbo max", "peacock", "hotstar", "sonyliv", "zee5", "voot",
        "alt balaji", "mxplayer", "jio cinema", "youtube premium"
    )

    fun extractCandidates(rawLines: List<String>, source: String = "Accessibility"): List<MediaMatchCandidate> {
        val candidates = mutableListOf<MediaMatchCandidate>()

        // Pass 1: look for player-bar style lines like "Title S01E03" -- these are gold
        val playerTitles = mutableListOf<Pair<String, Int>>() // (title, originalIndex)
        for ((index, line) in rawLines.withIndex()) {
            val playerTitle = HeuristicCleaner.extractPlayerTitle(line)
            if (playerTitle != null && !HeuristicCleaner.isJunkText(playerTitle)) {
                playerTitles.add(Pair(playerTitle, index))
            }
        }

        // Pass 2: normal candidate extraction
        for ((index, line) in rawLines.withIndex()) {
            if (HeuristicCleaner.isJunkText(line)) continue

            val baseCleaned = HeuristicCleaner.cleanTitle(line)
            if (baseCleaned.length < 2) continue

            val year = extractFirstInt(yearPattern, line)
            val season = extractFirstInt(seasonPattern, line)
            val episode = extractFirstInt(episodePattern, line)
            val chapter = extractFirstInt(chapterPattern, line)
            val volume = extractFirstInt(volumePattern, line)

            val cleanTitle = sanitizeForApiSearch(baseCleaned)
            if (cleanTitle.length < 2) continue

            val mediaType = detectMediaType(line)

            // Scoring: higher is more likely to be the real title
            var score = 0.5f
            if (index < 3) score += 0.30f     // Top lines in OCR output are more likely titles
            else if (index < 6) score += 0.15f
            if (year != null) score += 0.15f
            if (season != null || episode != null) score += 0.20f
            if (chapter != null) score += 0.15f
            if (mediaType != MediaType.UNKNOWN) score += 0.10f
            if (cleanTitle.length in 3..50) score += 0.10f

            // Bonus: if this line was identified as a player-bar title
            val isPlayerTitle = playerTitles.any { it.first.equals(cleanTitle, ignoreCase = true) }
            if (isPlayerTitle) score += 0.30f

            // Penalty: if the line contains streaming service name but no clear title keywords,
            // it might just be a UI label -- mild penalty
            val lower = line.lowercase()
            val hasStreamingSvc = streamingServices.any { lower.contains(it) }
            val hasKeyword = (animeKeywords + mangaKeywords + webSeriesKeywords + tvKeywords + movieKeywords)
                .any { lower.contains(it) }
            if (hasStreamingSvc && !hasKeyword) score -= 0.05f

            candidates.add(
                MediaMatchCandidate(
                    rawText = line,
                    cleanTitle = cleanTitle,
                    year = year,
                    season = season,
                    episode = episode,
                    chapter = chapter,
                    volume = volume,
                    mediaType = mediaType,
                    confidence = score.coerceIn(0.1f, 1.0f),
                    source = source
                )
            )
        }

        // Deduplicate by normalized title, keeping highest confidence
        return candidates
            .groupBy { it.cleanTitle.lowercase() }
            .map { (_, group) -> group.maxByOrNull { it.confidence }!! }
            .sortedByDescending { it.confidence }
    }

    private fun detectMediaType(text: String): MediaType {
        val lower = text.lowercase()
        return when {
            // Most specific first
            mangaKeywords.any { lower.contains(it) } -> MediaType.MANGA
            animeKeywords.any { lower.contains(it) } -> MediaType.ANIME
            webSeriesKeywords.any { lower.contains(it) } -> MediaType.WEB_SERIES
            movieKeywords.any { lower.contains(it) } -> MediaType.MOVIE
            tvKeywords.any { lower.contains(it) } -> MediaType.TV_SERIES
            else -> MediaType.UNKNOWN
        }
    }

    private fun sanitizeForApiSearch(title: String): String {
        return title
            .replace(Regex("(?i)\\b(?:season|s)\\s*\\d+\\b"), "")
            .replace(Regex("(?i)\\b(?:episode|ep|e)\\s*\\d+\\b"), "")
            .replace(Regex("(?i)\\b(?:chapter|ch)\\s*\\d+\\b"), "")
            .replace(Regex("(?i)\\b(?:volume|vol|v)\\s*\\d+\\b"), "")
            .replace(Regex("\\b(19\\d\\d|20\\d\\d)\\b"), "")
            .replace(Regex("(?i)\\b(anime|manga|movie|film|tv series|official|dubbed|subbed)\\b"), "")
            .replace(Regex("[\\[\\(][^\\]\\)]*[\\]\\)]"), "") // Remove [brackets] and (parentheses)
            .replace(Regex("[^a-zA-Z0-9\\s''\\-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractFirstInt(pattern: Pattern, text: String): Int? {
        val matcher = pattern.matcher(text)
        return if (matcher.find()) {
            matcher.group(1)?.toIntOrNull()
        } else {
            null
        }
    }
}
