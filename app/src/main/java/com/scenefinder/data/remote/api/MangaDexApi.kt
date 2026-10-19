package com.scenefinder.data.remote.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

interface MangaDexApi {

    @GET("manga")
    suspend fun searchManga(
        @Query("title") title: String,
        @Query("limit") limit: Int = 5,
        @Query("includes[]") includes: List<String> = listOf("cover_art")
    ): MangaDexSearchResponse
}

@JsonClass(generateAdapter = true)
data class MangaDexSearchResponse(
    val result: String?,
    val data: List<MangaDexItem>?
)

@JsonClass(generateAdapter = true)
data class MangaDexItem(
    val id: String,
    val type: String?,
    val attributes: MangaDexAttributes?,
    val relationships: List<MangaDexRelationship>?
)

@JsonClass(generateAdapter = true)
data class MangaDexAttributes(
    val title: Map<String, String>?,
    val altTitles: List<Map<String, String>>?,
    val description: Map<String, String>?,
    val status: String?,
    val year: Int?,
    val originalLanguage: String? = null,
    val contentRating: String? = null
)

@JsonClass(generateAdapter = true)
data class MangaDexRelationship(
    val id: String,
    val type: String?,
    val attributes: Map<String, Any?>?
)
