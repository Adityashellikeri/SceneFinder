package com.scenefinder.data.remote.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface TraceMoeApi {

    @Headers("Content-Type: image/jpeg")
    @POST("search?anilistInfo")
    suspend fun searchScene(@Body imageBody: RequestBody): TraceMoeResponse
}

@JsonClass(generateAdapter = true)
data class TraceMoeResponse(
    val frameCount: Int?,
    val error: String?,
    val result: List<TraceMoeResult>?
)

@JsonClass(generateAdapter = true)
data class TraceMoeResult(
    val anilist: TraceMoeAnilistInfo?,
    val filename: String?,
    val episode: Double?,
    val from: Double?,
    val to: Double?,
    val similarity: Double?,
    val video: String?,
    val image: String?
)

@JsonClass(generateAdapter = true)
data class TraceMoeAnilistInfo(
    val id: Int?,
    val idMal: Int?,
    val title: TraceMoeTitle?,
    val synonyms: List<String>?,
    val isAdult: Boolean?
)

@JsonClass(generateAdapter = true)
data class TraceMoeTitle(
    val native: String?,
    val romaji: String?,
    val english: String?
)
