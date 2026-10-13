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

    private val animeKeywords = listOf("anime", "animation", "subbed", "dubbed", "ova", "ona", "studio ghibli", "toei", "mappa", "ufotable")
    private val mangaKeywords = listOf("manga", "manhwa", "manhua", "webtoon", "light novel", "tankobon")
    private val tvKeywords = listOf("tv series", "tv show", "television series", "season", "series", "kdrama", "drama series")
    private val movieKeywords = listOf("movie", "film", "motion picture", "feature film", "cinema")

    fun extractCandidates(rawLines: List<String>, source: String = "Accessibility"): List<MediaMatchCandidate> {
        val candidates = mutableListOf<MediaMatchCandidate>()

        for ((index, line) in rawLines.withIndex()) {
            if (HeuristicCleaner.isJunkText(line)) continue

            val baseCleaned = HeuristicCleaner.cleanTitle(line)
            if (baseCleaned.length < 2) continue

            // Metadata extraction
            val year = extractFirstInt(yearPattern, line)
            val season = extractFirstInt(seasonPattern, line)
            val episode = extractFirstInt(episodePattern, line)
            val chapter = extractFirstInt(chapterPattern, line)
            val volume = extractFirstInt(volumePattern, line)

            // Stripped clean title for API matching
            val cleanTitle = sanitizeForApiSearch(baseCleaned)
            if (cleanTitle.length < 2) continue

            val mediaType = detectMediaType(line)

            // Position & quality scoring
            var score = 0.5f
            if (index < 5) score += 0.25f // Top result bias
            if (year != null) score += 0.15f
            if (season != null || episode != null || chapter != null) score += 0.15f
            if (mediaType != MediaType.UNKNOWN) score += 0.1f
            if (cleanTitle.length in 3..50) score += 0.1f

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

        // Deduplicate by normalized clean title, picking the highest confidence
        return candidates
            .groupBy { it.cleanTitle.lowercase() }
            .map { (_, group) -> group.maxByOrNull { it.confidence }!! }
            .sortedByDescending { it.confidence }
    }

    private fun detectMediaType(text: String): MediaType {
        val lower = text.lowercase()
        return when {
            animeKeywords.any { lower.contains(it) } -> MediaType.ANIME
            mangaKeywords.any { lower.contains(it) } -> MediaType.MANGA
            tvKeywords.any { lower.contains(it) } -> MediaType.TV_SERIES
            movieKeywords.any { lower.contains(it) } -> MediaType.MOVIE
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
            .replace(Regex("(?i)\\b(anime|manga|movie|film|tv series|official)\\b"), "")
            .replace(Regex("[\\[\\(][^\\]\\)]*[\\]\\)]"), "") // Remove [brackets] and (parentheses)
            .replace(Regex("[^a-zA-Z0-9\\s'’\\-]"), " ")
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
