package com.scenefinder.data.repository

import android.graphics.Bitmap
import androidx.paging.PagingSource
import com.scenefinder.data.local.dao.EnrichedMediaDao
import com.scenefinder.data.local.entity.EnrichedMediaEntity
import com.scenefinder.data.remote.api.AniListApi
import com.scenefinder.data.remote.api.AniListGraphQLRequest
import com.scenefinder.data.remote.api.AniListOAuthApi
import com.scenefinder.data.remote.api.AniListTag
import com.scenefinder.data.remote.api.MangaDexApi
import com.scenefinder.data.remote.api.TmdbAccountApi
import com.scenefinder.data.remote.api.TmdbApi
import com.scenefinder.data.remote.api.TraceMoeApi
import com.scenefinder.domain.model.MediaMatchCandidate
import com.scenefinder.domain.model.MediaType
import com.scenefinder.domain.ocr.ScreenOcrService
import com.scenefinder.domain.parser.CandidateExtractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaEnrichmentRepository @Inject constructor(
    private val dao: EnrichedMediaDao,
    private val aniListApi: AniListApi,
    private val aniListOAuthApi: AniListOAuthApi,
    private val tmdbApi: TmdbApi,
    private val tmdbAccountApi: TmdbAccountApi,
    private val mangaDexApi: MangaDexApi,
    private val traceMoeApi: TraceMoeApi
) {
    companion object {
        const val DEFAULT_TMDB_API_KEY = "c904da47044dae75c247aa2a6e93ad95"

        fun resolveAniListAgeRating(
            isAdult: Boolean? = null,
            genres: List<String>? = null,
            tags: List<AniListTag>? = null,
            mediaType: String? = null
        ): String {
            if (isAdult == true) return "18+"
            if (tags?.any { it.isAdult == true } == true) return "18+"
            if (!genres.isNullOrEmpty()) {
                val lower = genres.map { it.lowercase() }
                if (lower.any { it == "hentai" || it == "ecchi" }) return "18+"
            }
            val matureTags = setOf(
                "nudity", "sexual content", "ecchi", "gore", "violence",
                "torture", "cannibalism", "rape", "harem", "erotica", "sadism"
            )
            val hasMatureTag = tags?.any { tag ->
                val nameLower = tag.name?.lowercase() ?: ""
                matureTags.contains(nameLower) && (tag.rank ?: 0) >= 30
            } == true
            if (hasMatureTag) return "18+"

            if (!genres.isNullOrEmpty()) {
                val lower = genres.map { it.lowercase() }
                if (lower.any { it == "horror" || it == "psychological" }) return "16+"
                if (lower.any { it == "kids" || it == "children" }) return "PG"
            }
            return if (mediaType?.equals("Kids", true) == true) "PG" else "13+"
        }

        fun resolveAgeRating(
            isAdult: Boolean? = null,
            genres: List<String>? = null,
            genreIds: List<Int>? = null,
            contentRating: String? = null,
            mediaType: String? = null
        ): String {
            if (isAdult == true) return "18+"
            if (!contentRating.isNullOrBlank()) {
                val cr = contentRating.lowercase().trim()
                return when {
                    cr.contains("porn") || cr.contains("r18") || cr.contains("nc-17") || cr == "adult" || cr == "tv-ma" -> "18+"
                    cr.contains("erotica") || cr.contains("tv-14") || cr == "r" || cr == "18+" || cr == "16+" || cr == "15" -> "16+"
                    cr.contains("suggestive") || cr.contains("pg-13") || cr == "12" || cr == "13" -> "13+"
                    cr.contains("safe") || cr.contains("tv-pg") || cr.contains("tv-g") || cr.contains("tv-y") || cr == "pg" || cr == "g" -> "PG"
                    else -> contentRating.uppercase()
                }
            }
            if (!genres.isNullOrEmpty()) {
                val lower = genres.map { it.lowercase() }
                if (lower.any { it == "hentai" || it == "ecchi" }) return "18+"
                if (lower.any { it == "horror" || it == "psychological" || it == "gore" }) return "16+"
                if (lower.any { it == "kids" || it == "children" }) return "PG"
            }
            if (!genreIds.isNullOrEmpty()) {
                if (genreIds.contains(27) || genreIds.contains(80)) return "16+"
                if (genreIds.contains(10762) || genreIds.contains(10751)) return "PG"
            }
            return if (mediaType?.equals("Kids", true) == true) "PG" else "13+"
        }
    }
    private val cacheExpirationMillis = TimeUnit.DAYS.toMillis(7)

    fun getRecentHistory(): Flow<List<EnrichedMediaEntity>> = dao.getRecentHistory()

    fun getAllHistoryPaged(): PagingSource<Int, EnrichedMediaEntity> = dao.getAllHistoryPaged()

    suspend fun clearAllHistory() {
        dao.clearAll()
    }

    /** Direct API fetch for TMDB official age certification (Movies & TV). */
    suspend fun fetchTmdbAgeRating(tmdbId: Int, isMovie: Boolean, apiKey: String): String? {
        val effectiveKey = apiKey.ifBlank { DEFAULT_TMDB_API_KEY }
        return try {
            if (isMovie) {
                val resp = tmdbApi.getMovieReleaseDates(tmdbId, effectiveKey)
                val us = resp.results?.firstOrNull { it.iso3166_1.equals("US", true) }
                val usCerts = us?.releaseDates?.mapNotNull { it.certification?.trim()?.uppercase() }?.filter { it.isNotEmpty() }.orEmpty()

                for (c in usCerts) {
                    if (c == "R" || c == "NC-17" || c == "18+" || c == "18" || c == "X") return "18+"
                    if (c == "PG-13" || c == "14") return "13+"
                    if (c == "PG") return "PG"
                    if (c == "G") return "All"
                }

                val allCerts = resp.results?.flatMap { it.releaseDates.orEmpty() }?.mapNotNull { it.certification?.trim()?.uppercase() }?.filter { it.isNotEmpty() }.orEmpty()
                if (allCerts.any { it == "18" || it == "18+" || it == "R18" || it == "R18+" || it == "A" || it == "R" || it == "NC-17" || it == "R21" }) return "18+"
                if (allCerts.any { it == "15" || it == "16" || it == "16+" || it == "MA 15+" || it == "MA15+" || it == "R15+" }) return "16+"
                if (allCerts.any { it == "12" || it == "12+" || it == "13" || it == "PG-13" || it == "14" }) return "13+"
                if (allCerts.any { it == "PG" || it == "G" || it == "U" || it == "ALL" }) return "PG"
                null
            } else {
                val resp = tmdbApi.getTvContentRatings(tmdbId, effectiveKey)
                val us = resp.results?.firstOrNull { it.iso3166_1.equals("US", true) }?.rating?.trim()?.uppercase()
                if (!us.isNullOrBlank()) {
                    if (us == "TV-MA" || us == "18+" || us == "18" || us == "R") return "18+"
                    if (us == "TV-14") return "16+"
                    if (us == "TV-PG") return "PG"
                    if (us == "TV-G" || us == "TV-Y" || us == "TV-Y7") return "All"
                }
                val allRatings = resp.results?.mapNotNull { it.rating?.trim()?.uppercase() }?.filter { it.isNotEmpty() }.orEmpty()
                if (allRatings.any { it == "TV-MA" || it == "18" || it == "18+" || it == "19" || it == "R21" || it == "A" || it == "R" || it == "NC-17" }) return "18+"
                if (allRatings.any { it == "TV-14" || it == "15" || it == "16" || it == "16+" || it == "MA 15+" || it == "MA15+" }) return "16+"
                if (allRatings.any { it == "12" || it == "12+" || it == "13" || it == "PG-13" }) return "13+"
                if (allRatings.any { it == "TV-PG" || it == "PG" || it == "TV-G" || it == "G" || it == "U" || it == "TV-Y" || it == "TV-Y7" }) return "PG"
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Background refresh to update all library items with accurate direct API age ratings. */
    suspend fun refreshAllAgeRatings(tmdbApiKey: String = "") = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val items = dao.getAllList()
        val effectiveKey = tmdbApiKey.ifBlank { DEFAULT_TMDB_API_KEY }
        for (item in items) {
            try {
                val updatedRating = when {
                    item.id.startsWith("tmdb_movie_") -> {
                        val tmdbId = item.id.removePrefix("tmdb_movie_").toIntOrNull()
                        if (tmdbId != null) fetchTmdbAgeRating(tmdbId, isMovie = true, effectiveKey) else null
                    }
                    item.id.startsWith("tmdb_tv_") -> {
                        val tmdbId = item.id.removePrefix("tmdb_tv_").toIntOrNull()
                        if (tmdbId != null) fetchTmdbAgeRating(tmdbId, isMovie = false, effectiveKey) else null
                    }
                    item.id.startsWith("anilist_") -> {
                        val anilistId = item.id.substringAfterLast("_").toIntOrNull()
                        if (anilistId != null) {
                            val detailed = searchAniListById(anilistId)
                            detailed?.ageRating
                        } else null
                    }
                    else -> null
                }
                if (!updatedRating.isNullOrBlank() && updatedRating != item.ageRating) {
                    dao.updateAgeRating(item.id, updatedRating)
                }
            } catch (_: Exception) {}
        }
    }

    /** Exchange an AniList OAuth authorisation code for an access token. */
    suspend fun exchangeAniListCode(
        code: String,
        clientId: String,
        clientSecret: String,
        redirectUri: String
    ): String? = try {
        val resp = aniListOAuthApi.exchangeCodeForToken(
            clientId = clientId,
            clientSecret = clientSecret,
            redirectUri = redirectUri,
            code = code
        )
        resp.accessToken
    } catch (e: Exception) {
        null
    }

    /** Fuzzy AniList search for predictive / autocomplete suggestions (fast, lightweight). */
    suspend fun searchAniListSuggestions(
        query: String,
        limit: Int = 8
    ): List<EnrichedMediaEntity> = try {
        val gql = """
            query(${'$'}search: String, ${'$'}perPage: Int) {
              Page(perPage: ${'$'}perPage) {
                media(search: ${'$'}search, sort: SEARCH_MATCH) {
                  id type format
                  title { romaji english }
                  coverImage { large }
                  averageScore
                  genres
                  isAdult
                  tags { name isAdult rank }
                }
              }
            }
        """.trimIndent()
        val resp = aniListApi.queryMedia(
            AniListGraphQLRequest(gql, mapOf("search" to query, "perPage" to limit))
        )
        resp.data?.page?.media?.map { item ->
            val title = item.title?.english ?: item.title?.romaji ?: "Unknown"
            val type = when {
                item.format in listOf("MANGA", "ONE_SHOT", "NOVEL") -> "Manga"
                item.type == "ANIME" -> "Anime"
                else -> "Anime"
            }
            EnrichedMediaEntity(
                id = "anilist_suggest_${item.id}",
                searchKey = query.lowercase().trim(),
                cleanTitle = title,
                englishTitle = item.title?.english,
                originalTitle = item.title?.romaji,
                mediaType = type,
                year = null,
                season = null,
                episode = null,
                chapter = null,
                overview = null,
                posterUrl = item.coverImage?.large,
                bannerUrl = null,
                rating = item.averageScore?.let { it / 10.0f },
                sourceService = "AniList",
                directWebUrl = "https://anilist.co/anime/${item.id}",
                userStatus = "PLANNING",
                ageRating = resolveAniListAgeRating(isAdult = item.isAdult, genres = item.genres, tags = item.tags, mediaType = type)
            )
        } ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun saveToLibrary(
        media: EnrichedMediaEntity,
        anilistToken: String = "",
        tmdbApiKey: String = "",
        tmdbSessionId: String = "",
        tmdbAccountId: Int = -1
    ): Boolean {
        dao.insert(media)
        var cloudSynced = false

        val rawStatus = media.userStatus?.uppercase() ?: "PLANNING"
        val anilistStatus = when (rawStatus) {
            "CURRENT" -> "CURRENT"
            "COMPLETED" -> "COMPLETED"
            "DROPPED" -> "DROPPED"
            "PAUSED" -> "PAUSED"
            "REPEATING" -> "REPEATING"
            else -> "PLANNING"
        }

        val isAnime = media.mediaType.equals("Anime", ignoreCase = true)
        val isManga = media.mediaType.equals("Manga", ignoreCase = true) || media.mediaType.equals("Manhwa", ignoreCase = true)
        val cleanAniToken = anilistToken.trim().removePrefix("Bearer ").trim()

        // 1. Anime / Manga: If AniList is connected, add to AniList ONLY! Do not add to TMDB.
        if (isAnime || isManga || media.sourceService == "AniList") {
            if (cleanAniToken.isNotBlank()) {
                var aniId = if (media.sourceService == "AniList") {
                    media.id.substringAfterLast("_").toIntOrNull()
                } else null

                if (aniId == null) {
                    // Item was searched from TMDB or suggestion, resolve AniList ID
                    val targetType = if (isAnime) MediaType.ANIME else MediaType.MANGA
                    val aniMatch = searchAniList(
                        MediaMatchCandidate(rawText = media.cleanTitle, cleanTitle = media.cleanTitle, mediaType = targetType),
                        if (isAnime) "ANIME" else "MANGA"
                    )
                    aniId = aniMatch?.id?.substringAfterLast("_")?.toIntOrNull()
                }

                if (aniId != null) {
                    try {
                        val mutation = """
                            mutation (${"$"}mediaId: Int, ${"$"}status: MediaListStatus) {
                              SaveMediaListEntry(mediaId: ${"$"}mediaId, status: ${"$"}status) {
                                id
                                status
                              }
                            }
                        """.trimIndent()
                        val resp = aniListApi.executeMutation(
                            "Bearer $cleanAniToken",
                            AniListGraphQLRequest(mutation, mapOf("mediaId" to aniId, "status" to anilistStatus))
                        )
                        if (resp.data?.savedEntry != null) {
                            cloudSynced = true
                        } else if (resp.errors != null) {
                            android.util.Log.e("Sync", "AniList error: ${resp.errors.map { it.message }}")
                        }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        android.util.Log.e("Sync", "AniList save failed", e)
                    }
                }
            }
            // Return here so anime is never added to TMDB if AniList is available
            return cloudSynced
        }

        // 2. Movies / TV Shows / Web Series: Sync to TMDB
        if (tmdbSessionId.isNotBlank() && !tmdbSessionId.startsWith("__pending__:") && tmdbAccountId > 0) {
            val numericId = media.id.substringAfterLast("_").toIntOrNull()
            if (numericId != null && media.sourceService == "TMDB") {
                val isMovie = media.mediaType.equals("Movie", ignoreCase = true) || media.id.startsWith("tmdb_movie_")
                val type = if (isMovie) "movie" else "tv"
                val key = tmdbApiKey.ifBlank { DEFAULT_TMDB_API_KEY }
                cloudSynced = updateTmdbWatchlist(key, tmdbSessionId, tmdbAccountId, type, numericId, true)
            } else {
                // Intelligent fallback: search TMDB
                try {
                    val tmdbMatch = searchTmdb(
                        MediaMatchCandidate(rawText = media.cleanTitle, cleanTitle = media.cleanTitle, mediaType = MediaType.MOVIE),
                        tmdbApiKey
                    )
                    val tmdbId = tmdbMatch?.id?.substringAfterLast("_")?.toIntOrNull()
                    if (tmdbId != null) {
                        val isMovie = tmdbMatch.mediaType.equals("Movie", ignoreCase = true)
                        val type = if (isMovie) "movie" else "tv"
                        val key = tmdbApiKey.ifBlank { DEFAULT_TMDB_API_KEY }
                        cloudSynced = updateTmdbWatchlist(key, tmdbSessionId, tmdbAccountId, type, tmdbId, true)
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    android.util.Log.e("Sync", "TMDB fallback save failed", e)
                }
            }
        }
        return cloudSynced
    }

    suspend fun updateMediaStatus(
        media: EnrichedMediaEntity,
        newStatus: String,
        anilistToken: String = "",
        tmdbApiKey: String = "",
        tmdbSessionId: String = "",
        tmdbAccountId: Int = -1
    ): Boolean {
        // 1. Update local database with new status and fresh timestamp so it stays on top of feed
        val now = System.currentTimeMillis()
        dao.updateStatus(media.id, newStatus, now)

        val isAnime = media.mediaType.equals("Anime", ignoreCase = true)
        val isManga = media.mediaType.equals("Manga", ignoreCase = true) || media.mediaType.equals("Manhwa", ignoreCase = true)
        val cleanAniToken = anilistToken.trim().removePrefix("Bearer ").trim()

        // 2. Anime / Manga: Route status changes strictly to AniList
        if ((isAnime || isManga || media.sourceService == "AniList") && cleanAniToken.isNotBlank()) {
            var numericId = if (media.sourceService == "AniList") media.id.substringAfterLast("_").toIntOrNull() else null
            if (numericId == null) {
                val targetType = if (isAnime) MediaType.ANIME else MediaType.MANGA
                val aniMatch = searchAniList(
                    MediaMatchCandidate(rawText = media.cleanTitle, cleanTitle = media.cleanTitle, mediaType = targetType),
                    if (isAnime) "ANIME" else "MANGA"
                )
                numericId = aniMatch?.id?.substringAfterLast("_")?.toIntOrNull()
            }
            if (numericId != null) {
                return try {
                    val anilistStatus = when (newStatus.uppercase()) {
                        "CURRENT" -> "CURRENT"
                        "COMPLETED" -> "COMPLETED"
                        "DROPPED" -> "DROPPED"
                        "PAUSED" -> "PAUSED"
                        "REPEATING" -> "REPEATING"
                        else -> "PLANNING"
                    }
                    val mutation = """
                        mutation (${"$"}mediaId: Int, ${"$"}status: MediaListStatus) {
                          SaveMediaListEntry(mediaId: ${"$"}mediaId, status: ${"$"}status) {
                            id
                            status
                          }
                        }
                    """.trimIndent()
                    val vars = mapOf("mediaId" to numericId, "status" to anilistStatus)
                    val resp = aniListApi.executeMutation("Bearer $cleanAniToken", AniListGraphQLRequest(mutation, vars))
                    val ok = resp.data?.savedEntry != null
                    if (!ok && resp.errors != null) {
                        android.util.Log.e("AniListStatus", "AniList error: ${resp.errors.map { it.message }}")
                    }
                    ok
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    android.util.Log.e("Sync", "AniList update status failed", e)
                    false
                }
            }
        } else if (!isAnime && !isManga && media.sourceService == "TMDB" && tmdbSessionId.isNotBlank() && !tmdbSessionId.startsWith("__pending__:") && tmdbAccountId > 0) {
            val numericId = media.id.substringAfterLast("_").toIntOrNull()
            if (numericId != null) {
                val isMovie = media.mediaType.equals("Movie", ignoreCase = true) || media.id.startsWith("tmdb_movie_")
                val type = if (isMovie) "movie" else "tv"
                val inWatchlist = newStatus != "DROPPED"
                val key = tmdbApiKey.ifBlank { DEFAULT_TMDB_API_KEY }
                return updateTmdbWatchlist(key, tmdbSessionId, tmdbAccountId, type, numericId, inWatchlist)
            }
        }
        return true
    }

    suspend fun removeFromLibrary(
        media: EnrichedMediaEntity,
        anilistToken: String = "",
        tmdbApiKey: String = "",
        tmdbSessionId: String = "",
        tmdbAccountId: Int = -1
    ) {
        dao.deleteById(media.id)
        if (media.sourceService == "TMDB" && tmdbSessionId.isNotBlank() && !tmdbSessionId.startsWith("__pending__:") && tmdbAccountId > 0) {
            val numericId = media.id.substringAfterLast("_").toIntOrNull()
            if (numericId != null) {
                val isMovie = media.mediaType.equals("Movie", ignoreCase = true) || media.id.startsWith("tmdb_movie_")
                val type = if (isMovie) "movie" else "tv"
                val key = tmdbApiKey.ifBlank { DEFAULT_TMDB_API_KEY }
                updateTmdbWatchlist(key, tmdbSessionId, tmdbAccountId, type, numericId, false)
            }
        }
    }

    suspend fun searchAllMedia(query: String, tmdbApiKey: String = ""): List<EnrichedMediaEntity> = coroutineScope {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@coroutineScope emptyList()

        val aniListDeferred = async(Dispatchers.IO) {
            val list = mutableListOf<EnrichedMediaEntity>()
            try {
                val gql = """
                    query (${"$"}search: String) {
                      Page(page: 1, perPage: 12) {
                        media(search: ${"$"}search, sort: [SEARCH_MATCH]) {
                          id
                          title { romaji english native }
                          type format countryOfOrigin status
                          episodes chapters averageScore description
                          coverImage { extraLarge large }
                          bannerImage siteUrl genres isAdult
                          tags { name isAdult rank }
                        }
                      }
                    }
                """.trimIndent()
                val resp = aniListApi.queryMedia(AniListGraphQLRequest(gql, mapOf("search" to trimmed)))
                val items = resp.data?.page?.media.orEmpty()
                for (first in items) {
                    val displayTitle = first.title?.english ?: first.title?.romaji ?: trimmed
                    val detectedType = when {
                        first.type == "MANGA" && first.countryOfOrigin == "KR" -> "Manhwa"
                        first.type == "MANGA" -> "Manga"
                        first.format == "MOVIE" -> "Movie"
                        else -> "Anime"
                    }
                    list.add(
                        EnrichedMediaEntity(
                            id = "anilist_${first.type ?: "media"}_${first.id}",
                            searchKey = trimmed.lowercase(),
                            cleanTitle = displayTitle,
                            englishTitle = first.title?.english,
                            originalTitle = first.title?.native ?: first.title?.romaji,
                            mediaType = detectedType,
                            year = null,
                            season = null,
                            episode = first.episodes,
                            chapter = first.chapters,
                            overview = stripHtml(first.description),
                            posterUrl = first.coverImage?.extraLarge ?: first.coverImage?.large,
                            bannerUrl = first.bannerImage,
                            rating = first.averageScore?.let { it / 10.0f },
                            sourceService = "AniList",
                            directWebUrl = first.siteUrl ?: "https://anilist.co/anime/${first.id}",
                            userStatus = "PLANNING",
                            ageRating = resolveAniListAgeRating(isAdult = first.isAdult, genres = first.genres, tags = first.tags, mediaType = detectedType)
                        )
                    )
                }
            } catch (_: Exception) {}
            list
        }

        val tmdbDeferred = async(Dispatchers.IO) {
            val list = mutableListOf<EnrichedMediaEntity>()
            val effectiveTmdbKey = tmdbApiKey.ifBlank { DEFAULT_TMDB_API_KEY }
            try {
                val tmdbResp = tmdbApi.searchMulti(trimmed, effectiveTmdbKey)
                val tmdbItems = tmdbResp.results?.filter { it.mediaType == "movie" || it.mediaType == "tv" }.orEmpty()
                for (item in tmdbItems) {
                    val isMovie = item.mediaType == "movie"
                    val year = (item.releaseDate ?: item.firstAirDate)?.take(4)?.toIntOrNull()
                    // Classify TV results: high-vote-count items on certain streaming networks => Web Series
                    val resolvedType = when {
                        isMovie -> "Movie"
                        else -> "TV Shows"
                    }
                    val directRating = fetchTmdbAgeRating(item.id, isMovie = isMovie, apiKey = effectiveTmdbKey)
                    val tmdbRating = directRating ?: resolveAgeRating(isAdult = item.adult, genreIds = item.genreIds, mediaType = resolvedType)
                    list.add(
                        EnrichedMediaEntity(
                            id = "tmdb_${item.mediaType}_${item.id}",
                            searchKey = trimmed.lowercase(),
                            cleanTitle = item.displayTitle,
                            englishTitle = item.displayTitle,
                            originalTitle = item.originalTitle ?: item.originalName,
                            mediaType = resolvedType,
                            year = year,
                            season = null,
                            episode = null,
                            chapter = null,
                            overview = item.overview,
                            posterUrl = item.fullPosterUrl,
                            bannerUrl = item.fullBackdropUrl,
                            rating = item.voteAverage,
                            sourceService = "TMDB",
                            directWebUrl = "https://www.themoviedb.org/${item.mediaType}/${item.id}",
                            userStatus = "PLANNING",
                            ageRating = tmdbRating
                        )
                    )
                }
            } catch (_: Exception) {}
            list
        }

        val mangaDexDeferred = async(Dispatchers.IO) {
            val list = mutableListOf<EnrichedMediaEntity>()
            try {
                val mdResp = mangaDexApi.searchManga(trimmed, 5)
                val mdItems = mdResp.data.orEmpty()
                for (item in mdItems) {
                    val title = item.attributes?.title?.values?.firstOrNull() ?: trimmed
                    val desc = item.attributes?.description?.get("en")
                    val isManhwa = item.attributes?.originalLanguage == "ko"
                    list.add(
                        EnrichedMediaEntity(
                            id = "mangadex_${item.id}",
                            searchKey = trimmed.lowercase(),
                            cleanTitle = title,
                            englishTitle = title,
                            originalTitle = null,
                            mediaType = if (isManhwa) "Manhwa" else "Manga",
                            year = item.attributes?.year,
                            season = null,
                            episode = null,
                            chapter = null,
                            overview = desc,
                            posterUrl = null,
                            bannerUrl = null,
                            rating = null,
                            sourceService = "MangaDex",
                            directWebUrl = "https://mangadex.org/title/${item.id}",
                            userStatus = "PLANNING",
                            ageRating = resolveAgeRating(contentRating = item.attributes?.contentRating, mediaType = if (isManhwa) "Manhwa" else "Manga")
                        )
                    )
                }
            } catch (_: Exception) {}
            list
        }

        (aniListDeferred.await() + tmdbDeferred.await() + mangaDexDeferred.await()).distinctBy { it.id }
    }

    suspend fun enrichCandidate(
        candidate: MediaMatchCandidate,
        tmdbApiKey: String = ""
    ): EnrichedMediaEntity? {
        val searchKey = candidate.cleanTitle.trim().lowercase()
        if (searchKey.isBlank()) return null

        val cached = dao.getBySearchKey(searchKey)
        if (cached != null && (System.currentTimeMillis() - cached.cachedAt) < cacheExpirationMillis) {
            return cached
        }

        val enriched = when (candidate.mediaType) {
            MediaType.ANIME -> searchAniList(candidate, "ANIME") ?: searchTmdb(candidate, tmdbApiKey)
            MediaType.MANGA -> searchAniList(candidate, "MANGA") ?: searchMangaDex(candidate)
            MediaType.MANHWA -> searchAniList(candidate, "MANGA") ?: searchMangaDex(candidate)
            MediaType.MOVIE, MediaType.TV_SERIES, MediaType.TV_SHOWS, MediaType.WEB_SERIES ->
                searchTmdb(candidate, tmdbApiKey) ?: searchAniList(candidate, null)
            MediaType.UNKNOWN -> {
                // For unknown type: try TMDB first (live-action bias) -- only fall back to
                // AniList if TMDB returns nothing with decent confidence (voteCount > 10).
                val tmdbResult = searchTmdb(candidate, tmdbApiKey)
                if (tmdbResult != null) tmdbResult else searchAniList(candidate, null)
            }
        }

        return enriched
    }

    /**
     * Searches across all platforms (AniList Anime, AniList Manga, TMDB TV Shows, TMDB Movies)
     * concurrently to return all related adaptations and formats for a title.
     * E.g. searching "One Piece" returns its Anime, Manga, and Netflix TV Show!
     */
    suspend fun searchAllRelated(
        candidate: MediaMatchCandidate,
        tmdbApiKey: String = ""
    ): List<EnrichedMediaEntity> = coroutineScope {
        val effectiveKey = tmdbApiKey.ifBlank { DEFAULT_TMDB_API_KEY }

        val animeDeferred = async { searchAniList(candidate, "ANIME") }
        val mangaDeferred = async { searchAniList(candidate, "MANGA") }
        val tmdbDeferred = async { searchTmdbMultiList(candidate, effectiveKey) }

        val anime = animeDeferred.await()
        val manga = mangaDeferred.await()
        val tmdbList = tmdbDeferred.await()

        val results = mutableListOf<EnrichedMediaEntity>()
        if (anime != null) results.add(anime)
        if (manga != null) results.add(manga)
        for (item in tmdbList) {
            if (results.none { it.id == item.id || (it.mediaType == item.mediaType && it.cleanTitle.equals(item.cleanTitle, ignoreCase = true)) }) {
                results.add(item)
            }
        }
        results
    }

    private suspend fun searchTmdbMultiList(
        candidate: MediaMatchCandidate,
        apiKey: String
    ): List<EnrichedMediaEntity> {
        val effectiveKey = apiKey.ifBlank { DEFAULT_TMDB_API_KEY }
        return try {
            if (candidate.cleanTitle.lowercase().contains("google")) return emptyList()
            val response = tmdbApi.searchMulti(candidate.cleanTitle, effectiveKey)
            val filtered = response.results?.filter {
                (it.mediaType == "movie" || it.mediaType == "tv") &&
                        !it.displayTitle.lowercase().contains("google") &&
                        !com.scenefinder.domain.parser.HeuristicCleaner.isJunkText(it.displayTitle)
            }?.take(3).orEmpty()

            filtered.map { item ->
                val isMovie = item.mediaType == "movie"
                val displayTitle = item.displayTitle
                val year = (item.releaseDate ?: item.firstAirDate)?.take(4)?.toIntOrNull() ?: candidate.year
                val resolvedMediaType = when {
                    isMovie -> "Movie"
                    candidate.mediaType == MediaType.WEB_SERIES -> "Web Series"
                    else -> "TV Shows"
                }
                val directRating = fetchTmdbAgeRating(item.id, isMovie = isMovie, apiKey = effectiveKey)
                val finalAgeRating = directRating ?: resolveAgeRating(isAdult = item.adult, genreIds = item.genreIds, mediaType = resolvedMediaType)

                EnrichedMediaEntity(
                    id = "tmdb_${item.mediaType}_${item.id}",
                    searchKey = candidate.cleanTitle.trim().lowercase(),
                    cleanTitle = displayTitle,
                    englishTitle = displayTitle,
                    originalTitle = item.originalTitle ?: item.originalName,
                    mediaType = resolvedMediaType,
                    year = year,
                    season = candidate.season,
                    episode = candidate.episode,
                    chapter = null,
                    overview = item.overview,
                    posterUrl = item.fullPosterUrl,
                    bannerUrl = item.fullBackdropUrl,
                    rating = item.voteAverage,
                    sourceService = "TMDB",
                    directWebUrl = "https://www.themoviedb.org/${item.mediaType}/${item.id}",
                    userStatus = "PLANNING",
                    ageRating = finalAgeRating
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun searchAniList(candidate: MediaMatchCandidate, typeFilter: String?): EnrichedMediaEntity? {
        val query = """
            query (${"$"}search: String, ${"$"}type: MediaType) {
              Page(page: 1, perPage: 3) {
                media(search: ${"$"}search, type: ${"$"}type) {
                  id
                  title {
                    romaji
                    english
                    native
                  }
                  type
                  format
                  status
                  episodes
                  chapters
                  averageScore
                  description
                  coverImage {
                    extraLarge
                    large
                  }
                  bannerImage
                  siteUrl
                  genres
                  isAdult
                  tags { name isAdult rank }
                }
              }
            }
        """.trimIndent()

        val variables = mutableMapOf("search" to candidate.cleanTitle)
        if (typeFilter != null) {
            variables["type"] = typeFilter
        }

        return try {
            val response = aniListApi.queryMedia(AniListGraphQLRequest(query, variables))
            val first = response.data?.page?.media?.firstOrNull() ?: return null
            val displayTitle = first.title?.english ?: first.title?.romaji ?: candidate.cleanTitle

            EnrichedMediaEntity(
                id = "anilist_${first.type ?: "media"}_${first.id}",
                searchKey = candidate.cleanTitle.trim().lowercase(),
                cleanTitle = displayTitle,
                englishTitle = first.title?.english,
                originalTitle = first.title?.native ?: first.title?.romaji,
                mediaType = if (first.type == "MANGA") "Manga" else "Anime",
                year = candidate.year,
                season = candidate.season,
                episode = candidate.episode ?: first.episodes,
                chapter = candidate.chapter ?: first.chapters,
                overview = stripHtml(first.description),
                posterUrl = first.coverImage?.extraLarge ?: first.coverImage?.large,
                bannerUrl = first.bannerImage,
                rating = first.averageScore?.let { it / 10.0f },
                sourceService = "AniList",
                directWebUrl = first.siteUrl ?: "https://anilist.co/anime/${first.id}",
                userStatus = "PLANNING",
                ageRating = resolveAniListAgeRating(isAdult = first.isAdult, genres = first.genres, tags = first.tags, mediaType = if (first.type == "MANGA") "Manga" else "Anime")
            )
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun searchTmdb(candidate: MediaMatchCandidate, apiKey: String): EnrichedMediaEntity? {
        val effectiveKey = apiKey.ifBlank { DEFAULT_TMDB_API_KEY }
        return try {
            if (candidate.cleanTitle.lowercase().contains("google")) return null
            val response = tmdbApi.searchMulti(candidate.cleanTitle, effectiveKey)
            val first = response.results?.firstOrNull { 
                (it.mediaType == "movie" || it.mediaType == "tv") &&
                !it.displayTitle.lowercase().contains("google") &&
                !com.scenefinder.domain.parser.HeuristicCleaner.isJunkText(it.displayTitle)
            } ?: return null
            val isMovie = first.mediaType == "movie"
            val displayTitle = first.displayTitle
            val year = (first.releaseDate ?: first.firstAirDate)?.take(4)?.toIntOrNull() ?: candidate.year

            // Classify TV results: Web Series vs TV Shows
            // Web Series = streaming originals (Netflix, Prime, Disney+, Apple TV+, Hotstar, HBO Max, etc.)
            // We detect this by checking networks via a detail call when voteCount > 0,
            // but for the lightweight search path, we use keywords in the title or candidate context.
            val resolvedMediaType = when {
                isMovie -> "Movie"
                candidate.mediaType == MediaType.WEB_SERIES -> "Web Series"
                else -> "TV Shows"
            }

            val directRating = fetchTmdbAgeRating(first.id, isMovie = isMovie, apiKey = effectiveKey)
            val finalAgeRating = directRating ?: resolveAgeRating(isAdult = first.adult, genreIds = first.genreIds, mediaType = resolvedMediaType)

            EnrichedMediaEntity(
                id = "tmdb_${first.mediaType}_${first.id}",
                searchKey = candidate.cleanTitle.trim().lowercase(),
                cleanTitle = displayTitle,
                englishTitle = displayTitle,
                originalTitle = first.originalTitle ?: first.originalName,
                mediaType = resolvedMediaType,
                year = year,
                season = candidate.season,
                episode = candidate.episode,
                chapter = null,
                overview = first.overview,
                posterUrl = first.fullPosterUrl,
                bannerUrl = first.fullBackdropUrl,
                rating = first.voteAverage,
                sourceService = "TMDB",
                directWebUrl = "https://www.themoviedb.org/${first.mediaType}/${first.id}",
                userStatus = "PLANNING",
                ageRating = finalAgeRating
            )
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun searchMangaDex(candidate: MediaMatchCandidate): EnrichedMediaEntity? {
        return try {
            val response = mangaDexApi.searchManga(candidate.cleanTitle)
            val first = response.data?.firstOrNull() ?: return null
            val title = first.attributes?.title?.values?.firstOrNull() ?: candidate.cleanTitle
            val desc = first.attributes?.description?.get("en")

            EnrichedMediaEntity(
                id = "mangadex_${first.id}",
                searchKey = candidate.cleanTitle.trim().lowercase(),
                cleanTitle = title,
                englishTitle = title,
                originalTitle = null,
                mediaType = "Manga",
                year = first.attributes?.year ?: candidate.year,
                season = null,
                episode = null,
                chapter = candidate.chapter,
                overview = desc,
                posterUrl = null,
                bannerUrl = null,
                rating = null,
                sourceService = "MangaDex",
                directWebUrl = "https://mangadex.org/title/${first.id}",
                userStatus = "PLANNING",
                ageRating = resolveAgeRating(contentRating = first.attributes?.contentRating, mediaType = "Manga")
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Crop black letterbox bars from a screenshot before sending to trace.moe.
     * Scans from top/bottom edges inward to find the first row/column that isn't
     * nearly pure black (luminance > 15/255). Handles 20:9 phone screenshots of
     * widescreen content where black bars appear at top and bottom.
     */
    private fun autoCropLetterbox(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        if (w == 0 || h == 0) return src

        val blackThreshold = 20 // luminance threshold 0-255

        fun rowIsBlack(y: Int): Boolean {
            // Sample every 4th pixel for speed
            val step = maxOf(1, w / 16)
            for (x in 0 until w step step) {
                val px = src.getPixel(x, y)
                val r = (px shr 16) and 0xFF
                val g = (px shr 8) and 0xFF
                val b = px and 0xFF
                val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                if (lum > blackThreshold) return false
            }
            return true
        }

        var top = 0
        while (top < h / 3 && rowIsBlack(top)) top++

        var bottom = h - 1
        while (bottom > 2 * h / 3 && rowIsBlack(bottom)) bottom--

        if (top >= bottom || (top == 0 && bottom == h - 1)) return src

        return try {
            Bitmap.createBitmap(src, 0, top, w, bottom - top + 1)
        } catch (_: Exception) {
            src
        }
    }

    private fun stripHtml(html: String?): String? {
        if (html == null) return null
        return html.replace(Regex("<[^>]*>"), "").replace("&quot;", "\"").replace("&#039;", "'").trim()
    }

    suspend fun recognizeSceneFromBitmap(
        bitmap: Bitmap,
        ocrService: ScreenOcrService,
        candidateExtractor: CandidateExtractor,
        tmdbApiKey: String = ""
    ): EnrichedMediaEntity? {
        val effectiveApiKey = tmdbApiKey.ifBlank { DEFAULT_TMDB_API_KEY }

        // 1. Anime Scene Recognition via trace.moe visual search
        try {
            // Crop letterbox black bars first (common on 20:9 phones) so trace.moe
            // gets the actual scene pixels, not distorted by bars.
            val cropped = autoCropLetterbox(bitmap)
            val maxDim = maxOf(cropped.width, cropped.height)
            val scaled = if (maxDim > 1280) {
                val scale = 1280f / maxDim
                Bitmap.createScaledBitmap(cropped, (cropped.width * scale).toInt(), (cropped.height * scale).toInt(), true)
            } else {
                cropped
            }
            val stream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val bytes = stream.toByteArray()
            val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())

            val traceResponse = traceMoeApi.searchScene(requestBody)
            val topMatch = traceResponse.result?.firstOrNull()
            val similarity = topMatch?.similarity ?: 0.0

            if (similarity >= 0.87) {
                val anilistId = topMatch?.anilist?.id
                if (anilistId != null) {
                    val detailed = searchAniListById(anilistId)
                    if (detailed != null) {
                        val entity = detailed.copy(
                            mediaType = "Anime",
                            episode = topMatch.episode?.toInt() ?: detailed.episode,
                            sourceService = "AniList (trace.moe)",
                            overview = if (detailed.overview.isNullOrBlank()) {
                                "Matched anime scene with ${(similarity * 100).toInt()}% visual similarity"
                            } else {
                                detailed.overview
                            }
                        )
                        return entity
                    }
                }

                val title = topMatch?.anilist?.title?.english
                    ?: topMatch?.anilist?.title?.romaji
                    ?: topMatch?.filename
                    ?: "Anime Scene"
                val entity = EnrichedMediaEntity(
                    id = "anilist_anime_${topMatch?.anilist?.id ?: System.currentTimeMillis()}",
                    searchKey = title.lowercase(),
                    cleanTitle = title,
                    englishTitle = topMatch?.anilist?.title?.english,
                    originalTitle = topMatch?.anilist?.title?.native ?: topMatch?.anilist?.title?.romaji,
                    mediaType = "Anime",
                    year = null,
                    season = null,
                    episode = topMatch?.episode?.toInt(),
                    chapter = null,
                    overview = "Visual scene match (${(similarity * 100).toInt()}% similarity)",
                    posterUrl = topMatch?.image,
                    bannerUrl = null,
                    rating = null,
                    sourceService = "trace.moe",
                    directWebUrl = "https://anilist.co/anime/${topMatch?.anilist?.id}",
                    userStatus = "PLANNING",
                    ageRating = "13+"
                )
                return entity
            }
        } catch (_: Exception) {
            // Ignore trace.moe error and proceed to OCR
        }

        // 2. Alternative Scene Recognition: Manga, Manhwa, Movies & TV Shows via OCR
        val ocrLines = ocrService.processImage(bitmap)
        if (ocrLines.isNotEmpty()) {
            val candidates = candidateExtractor.extractCandidates(ocrLines, "Scene Recognizer")
            for (candidate in candidates.take(5)) {
                val enriched = enrichCandidate(candidate, effectiveApiKey)
                if (enriched != null) {
                    return enriched
                }
            }

            // Fallback: search prominent text lines directly
            for (line in ocrLines.filter { it.length in 3..50 }.take(4)) {
                val searchResults = searchAllMedia(line, effectiveApiKey)
                if (searchResults.isNotEmpty()) {
                    val best = searchResults.first()
                    return best
                }
            }
        }

        return null
    }

    suspend fun searchAniListById(id: Int): EnrichedMediaEntity? {
        val query = """
            query (${"$"}id: Int) {
              Page(page: 1, perPage: 1) {
                media(id: ${"$"}id) {
                  id
                  title {
                    romaji
                    english
                    native
                  }
                  type
                  format
                  status
                  episodes
                  chapters
                  averageScore
                  description
                  coverImage {
                    extraLarge
                    large
                  }
                  bannerImage
                  siteUrl
                  genres
                  isAdult
                  tags { name isAdult rank }
                }
              }
            }
        """.trimIndent()

        return try {
            val response = aniListApi.queryMedia(AniListGraphQLRequest(query, mapOf("id" to id)))
            val media = response.data?.page?.media?.firstOrNull() ?: return null
            val displayTitle = media.title?.english ?: media.title?.romaji ?: "Unknown"

            EnrichedMediaEntity(
                id = "anilist_${media.type ?: "media"}_${media.id}",
                searchKey = displayTitle.trim().lowercase(),
                cleanTitle = displayTitle,
                englishTitle = media.title?.english,
                originalTitle = media.title?.native ?: media.title?.romaji,
                mediaType = if (media.type == "MANGA") "Manga" else "Anime",
                year = null,
                season = null,
                episode = media.episodes,
                chapter = media.chapters,
                overview = stripHtml(media.description),
                posterUrl = media.coverImage?.extraLarge ?: media.coverImage?.large,
                bannerUrl = media.bannerImage,
                rating = media.averageScore?.let { it / 10.0f },
                sourceService = "AniList",
                directWebUrl = media.siteUrl ?: "https://anilist.co/anime/${media.id}",
                userStatus = "PLANNING",
                ageRating = resolveAniListAgeRating(isAdult = media.isAdult, genres = media.genres, tags = media.tags, mediaType = if (media.type == "MANGA") "Manga" else "Anime")
            )
        } catch (_: Exception) {
            null
        }
    }
    // ── Library Sync Functions ──────────────────────────────────────────────────

    /**
     * Pull the entire AniList MediaList for the authenticated user and upsert
     * every entry into the local Room database.  Returns the count of synced items.
     */
    suspend fun syncAniListLibrary(anilistToken: String): Int {
        val cleanToken = anilistToken.trim().removePrefix("Bearer ").trim()
        if (cleanToken.isBlank()) return 0
        return try {
            // Step 1: Query Viewer to get the authenticated user's ID
            val viewerGql = "query { Viewer { id name } }"
            val viewerResp = aniListApi.queryUserLibrary(
                "Bearer $cleanToken",
                AniListGraphQLRequest(viewerGql, emptyMap())
            )
            val userId = viewerResp.data?.viewer?.id ?: return 0

            // Step 2: Fetch both Anime and Manga collections with updatedAt and createdAt
            val listGql = """
                query (${"$"}userId: Int, ${"$"}type: MediaType) {
                  MediaListCollection(userId: ${"$"}userId, type: ${"$"}type) {
                    lists {
                      name
                      status
                      entries {
                        id
                        status
                        score
                        progress
                        updatedAt
                        createdAt
                        media {
                          id type format
                          countryOfOrigin
                          title { romaji english native }
                          coverImage { extraLarge large }
                          bannerImage
                          averageScore
                          description
                          episodes chapters
                          siteUrl
                          genres
                          isAdult
                          tags { name isAdult rank }
                        }
                      }
                    }
                  }
                }
            """.trimIndent()

            val entitiesToInsert = mutableListOf<EnrichedMediaEntity>()
            for (mediaType in listOf("ANIME", "MANGA")) {
                val resp = aniListApi.queryUserLibrary(
                    "Bearer $cleanToken",
                    AniListGraphQLRequest(listGql, mapOf("userId" to userId, "type" to mediaType))
                )
                val lists = resp.data?.mediaListCollection?.lists.orEmpty()
                for (group in lists) {
                    val groupStatus = group.status ?: "PLANNING"
                    for (entry in group.entries.orEmpty()) {
                        val media = entry.media ?: continue
                        val displayTitle = media.title?.english ?: media.title?.romaji ?: "Unknown"
                        val detectedType = when {
                            media.type == "MANGA" && media.countryOfOrigin == "KR" -> "Manhwa"
                            media.type == "MANGA" -> "Manga"
                            media.format == "MOVIE" -> "Movie"
                            else -> "Anime"
                        }
                        val entityId = "anilist_${media.type ?: "media"}_${media.id}"
                        val existing = dao.getById(entityId)
                        val entryTimestamp = if ((entry.updatedAt ?: 0L) > 0L) (entry.updatedAt!! * 1000L) else System.currentTimeMillis()
                        val finalCachedAt = existing?.cachedAt ?: entryTimestamp

                        // If user modified status locally in the last 3 minutes, do not let stale cloud cache overwrite it
                        val finalStatus = if (existing != null && (System.currentTimeMillis() - existing.cachedAt < 180_000L)) {
                            existing.userStatus
                        } else {
                            entry.status ?: groupStatus
                        }

                        val entity = EnrichedMediaEntity(
                            id = entityId,
                            searchKey = displayTitle.trim().lowercase(),
                            cleanTitle = displayTitle,
                            englishTitle = media.title?.english,
                            originalTitle = media.title?.native ?: media.title?.romaji,
                            mediaType = detectedType,
                            year = null,
                            season = null,
                            episode = media.episodes,
                            chapter = media.chapters,
                            overview = stripHtml(media.description),
                            posterUrl = media.coverImage?.extraLarge ?: media.coverImage?.large,
                            bannerUrl = media.bannerImage,
                            rating = media.averageScore?.let { it / 10.0f },
                            sourceService = "AniList",
                            directWebUrl = media.siteUrl ?: "https://anilist.co/anime/${media.id}",
                            userStatus = finalStatus,
                            ageRating = resolveAniListAgeRating(isAdult = media.isAdult, genres = media.genres, tags = media.tags, mediaType = detectedType),
                            cachedAt = finalCachedAt
                        )
                        entitiesToInsert.add(entity)
                    }
                }
            }
            if (entitiesToInsert.isNotEmpty()) {
                dao.insertAll(entitiesToInsert)
            }
            entitiesToInsert.size
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("AniListSync", "Failed to sync AniList library", e)
            0
        }
    }

    /**
     * Pull the TMDB watchlist + favorites (movies + TV) for the authenticated user and upsert
     * every item into the local Room database. Returns the count of synced items.
     */
    suspend fun syncTmdbLibrary(
        apiKey: String,
        sessionId: String,
        accountId: Int
    ): Int {
        if (apiKey.isBlank() || sessionId.isBlank() || accountId < 0) return 0
        val entitiesToInsert = mutableListOf<EnrichedMediaEntity>()
        try {
            // Fetch watchlist movies
            val moviesResp = tmdbAccountApi.getWatchlistMovies(accountId, apiKey, sessionId)
            for (item in moviesResp.results.orEmpty()) {
                val year = item.releaseDate?.take(4)?.toIntOrNull()
                val entityId = "tmdb_movie_${item.id}"
                val existing = dao.getById(entityId)
                val directRating = fetchTmdbAgeRating(item.id, isMovie = true, apiKey = apiKey)
                val ageRating = directRating ?: resolveAgeRating(isAdult = item.adult, genreIds = item.genreIds, mediaType = "Movie")
                val entity = EnrichedMediaEntity(
                    id = entityId,
                    searchKey = item.displayTitle.trim().lowercase(),
                    cleanTitle = item.displayTitle,
                    englishTitle = item.displayTitle,
                    originalTitle = item.originalTitle,
                    mediaType = "Movie",
                    year = year,
                    season = null, episode = null, chapter = null,
                    overview = item.overview,
                    posterUrl = item.fullPosterUrl,
                    bannerUrl = item.fullBackdropUrl,
                    rating = item.voteAverage,
                    sourceService = "TMDB",
                    directWebUrl = "https://www.themoviedb.org/movie/${item.id}",
                    userStatus = existing?.userStatus ?: "PLANNING",
                    ageRating = ageRating,
                    cachedAt = existing?.cachedAt ?: System.currentTimeMillis()
                )
                entitiesToInsert.add(entity)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("TmdbSync", "Failed to sync TMDB watchlist movies", e)
        }
        try {
            // Fetch watchlist TV shows
            val tvResp = tmdbAccountApi.getWatchlistTv(accountId, apiKey, sessionId)
            for (item in tvResp.results.orEmpty()) {
                val year = item.firstAirDate?.take(4)?.toIntOrNull()
                val entityId = "tmdb_tv_${item.id}"
                val existing = dao.getById(entityId)
                val directRating = fetchTmdbAgeRating(item.id, isMovie = false, apiKey = apiKey)
                val ageRating = directRating ?: resolveAgeRating(isAdult = item.adult, genreIds = item.genreIds, mediaType = "TV Shows")
                val entity = EnrichedMediaEntity(
                    id = entityId,
                    searchKey = item.displayTitle.trim().lowercase(),
                    cleanTitle = item.displayTitle,
                    englishTitle = item.displayTitle,
                    originalTitle = item.originalName,
                    mediaType = "TV Shows",
                    year = year,
                    season = null, episode = null, chapter = null,
                    overview = item.overview,
                    posterUrl = item.fullPosterUrl,
                    bannerUrl = item.fullBackdropUrl,
                    rating = item.voteAverage,
                    sourceService = "TMDB",
                    directWebUrl = "https://www.themoviedb.org/tv/${item.id}",
                    userStatus = existing?.userStatus ?: "PLANNING",
                    ageRating = ageRating,
                    cachedAt = existing?.cachedAt ?: System.currentTimeMillis()
                )
                entitiesToInsert.add(entity)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("TmdbSync", "Failed to sync TMDB watchlist TV", e)
        }
        try {
            // Fetch favorite movies
            val favMovies = tmdbAccountApi.getFavoriteMovies(accountId, apiKey, sessionId)
            for (item in favMovies.results.orEmpty()) {
                val year = item.releaseDate?.take(4)?.toIntOrNull()
                val entityId = "tmdb_movie_${item.id}"
                val existing = dao.getById(entityId)
                val directRating = fetchTmdbAgeRating(item.id, isMovie = true, apiKey = apiKey)
                val ageRating = directRating ?: resolveAgeRating(isAdult = item.adult, genreIds = item.genreIds, mediaType = "Movie")
                val entity = EnrichedMediaEntity(
                    id = entityId,
                    searchKey = item.displayTitle.trim().lowercase(),
                    cleanTitle = item.displayTitle,
                    englishTitle = item.displayTitle,
                    originalTitle = item.originalTitle,
                    mediaType = "Movie",
                    year = year,
                    season = null, episode = null, chapter = null,
                    overview = item.overview,
                    posterUrl = item.fullPosterUrl,
                    bannerUrl = item.fullBackdropUrl,
                    rating = item.voteAverage,
                    sourceService = "TMDB",
                    directWebUrl = "https://www.themoviedb.org/movie/${item.id}",
                    userStatus = existing?.userStatus ?: "COMPLETED",
                    ageRating = ageRating,
                    cachedAt = existing?.cachedAt ?: System.currentTimeMillis()
                )
                entitiesToInsert.add(entity)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("TmdbSync", "Failed to sync TMDB favorite movies", e)
        }
        try {
            // Fetch favorite TV shows
            val favTv = tmdbAccountApi.getFavoriteTv(accountId, apiKey, sessionId)
            for (item in favTv.results.orEmpty()) {
                val year = item.firstAirDate?.take(4)?.toIntOrNull()
                val entityId = "tmdb_tv_${item.id}"
                val existing = dao.getById(entityId)
                val directRating = fetchTmdbAgeRating(item.id, isMovie = false, apiKey = apiKey)
                val ageRating = directRating ?: resolveAgeRating(isAdult = item.adult, genreIds = item.genreIds, mediaType = "TV Shows")
                val entity = EnrichedMediaEntity(
                    id = entityId,
                    searchKey = item.displayTitle.trim().lowercase(),
                    cleanTitle = item.displayTitle,
                    englishTitle = item.displayTitle,
                    originalTitle = item.originalName,
                    mediaType = "TV Shows",
                    year = year,
                    season = null, episode = null, chapter = null,
                    overview = item.overview,
                    posterUrl = item.fullPosterUrl,
                    bannerUrl = item.fullBackdropUrl,
                    rating = item.voteAverage,
                    sourceService = "TMDB",
                    directWebUrl = "https://www.themoviedb.org/tv/${item.id}",
                    userStatus = existing?.userStatus ?: "COMPLETED",
                    ageRating = ageRating,
                    cachedAt = existing?.cachedAt ?: System.currentTimeMillis()
                )
                entitiesToInsert.add(entity)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("TmdbSync", "Failed to sync TMDB favorite TV", e)
        }
        if (entitiesToInsert.isNotEmpty()) {
            dao.insertAll(entitiesToInsert)
        }
        return entitiesToInsert.size
    }

    /**
     * Add or remove a TMDB item from the user's watchlist.
     * [mediaType] should be "movie" or "tv".
     */
    suspend fun updateTmdbWatchlist(
        apiKey: String,
        sessionId: String,
        accountId: Int,
        mediaType: String,
        mediaId: Int,
        inWatchlist: Boolean
    ): Boolean = try {
        val resp = tmdbAccountApi.addToWatchlist(
            accountId, apiKey, sessionId,
            com.scenefinder.data.remote.api.TmdbWatchlistBody(mediaType, mediaId, inWatchlist)
        )
        resp.success == true || resp.statusCode == 1 || resp.statusCode == 12 || resp.statusCode == 13
    } catch (e: Exception) {
        android.util.Log.e("TMDB", "Failed to update watchlist", e)
        false
    }

    /**
     * Create a TMDB session from a validated request token and fetch + store account details.
     * Returns (sessionId, accountId) or null on failure.
     */
    suspend fun createTmdbSession(apiKey: String, requestToken: String): Pair<String, Int>? = try {
        val sessionResp = tmdbAccountApi.createSession(
            apiKey,
            com.scenefinder.data.remote.api.TmdbSessionBody(requestToken)
        )
        val sessionId = sessionResp.sessionId ?: return null
        val account = tmdbAccountApi.getAccountDetails(apiKey, sessionId)
        Pair(sessionId, account.id)
    } catch (_: Exception) { null }

    /** Get a TMDB request token to begin the login flow. */
    suspend fun getTmdbRequestToken(apiKey: String): String? = try {
        tmdbAccountApi.createRequestToken(apiKey).requestToken
    } catch (_: Exception) { null }
}
