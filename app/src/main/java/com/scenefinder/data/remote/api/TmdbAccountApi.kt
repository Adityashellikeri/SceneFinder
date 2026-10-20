package com.scenefinder.data.remote.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbAccountApi {

    /** Step 1: Create a request token */
    @GET("3/authentication/token/new")
    suspend fun createRequestToken(
        @Query("api_key") apiKey: String
    ): TmdbRequestTokenResponse

    /** Step 2: Create session from validated request token */
    @POST("3/authentication/session/new")
    suspend fun createSession(
        @Query("api_key") apiKey: String,
        @Body body: TmdbSessionBody
    ): TmdbSessionResponse

    /** Get account details (needed to get account_id) */
    @GET("3/account")
    suspend fun getAccountDetails(
        @Query("api_key") apiKey: String,
        @Query("session_id") sessionId: String
    ): TmdbAccountDetails

    /** Fetch user's watchlist movies */
    @GET("3/account/{account_id}/watchlist/movies")
    suspend fun getWatchlistMovies(
        @Path("account_id") accountId: Int,
        @Query("api_key") apiKey: String,
        @Query("session_id") sessionId: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    /** Fetch user's watchlist TV shows */
    @GET("3/account/{account_id}/watchlist/tv")
    suspend fun getWatchlistTv(
        @Path("account_id") accountId: Int,
        @Query("api_key") apiKey: String,
        @Query("session_id") sessionId: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    /** Fetch user's favorite movies */
    @GET("3/account/{account_id}/favorite/movies")
    suspend fun getFavoriteMovies(
        @Path("account_id") accountId: Int,
        @Query("api_key") apiKey: String,
        @Query("session_id") sessionId: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    /** Fetch user's favorite TV shows */
    @GET("3/account/{account_id}/favorite/tv")
    suspend fun getFavoriteTv(
        @Path("account_id") accountId: Int,
        @Query("api_key") apiKey: String,
        @Query("session_id") sessionId: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    /** Fetch user's rated movies */
    @GET("3/account/{account_id}/rated/movies")
    suspend fun getRatedMovies(
        @Path("account_id") accountId: Int,
        @Query("api_key") apiKey: String,
        @Query("session_id") sessionId: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    /** Fetch user's rated TV shows */
    @GET("3/account/{account_id}/rated/tv")
    suspend fun getRatedTv(
        @Path("account_id") accountId: Int,
        @Query("api_key") apiKey: String,
        @Query("session_id") sessionId: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    /** Add/remove item from watchlist */
    @POST("3/account/{account_id}/watchlist")
    suspend fun addToWatchlist(
        @Path("account_id") accountId: Int,
        @Query("api_key") apiKey: String,
        @Query("session_id") sessionId: String,
        @Body body: TmdbWatchlistBody
    ): TmdbStatusResponse

    /** Rate a movie */
    @POST("3/movie/{movie_id}/rating")
    suspend fun rateMovie(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("session_id") sessionId: String,
        @Body body: TmdbRateBody
    ): TmdbStatusResponse

    /** Rate a TV show */
    @POST("3/tv/{tv_id}/rating")
    suspend fun rateTv(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("session_id") sessionId: String,
        @Body body: TmdbRateBody
    ): TmdbStatusResponse
}

@JsonClass(generateAdapter = true)
data class TmdbRateBody(
    val value: Double = 10.0
)

@JsonClass(generateAdapter = true)
data class TmdbRequestTokenResponse(
    val success: Boolean?,
    @Json(name = "request_token") val requestToken: String?,
    @Json(name = "expires_at") val expiresAt: String?
)

@JsonClass(generateAdapter = true)
data class TmdbSessionBody(
    @Json(name = "request_token") val requestToken: String
)

@JsonClass(generateAdapter = true)
data class TmdbSessionResponse(
    val success: Boolean?,
    @Json(name = "session_id") val sessionId: String?
)

@JsonClass(generateAdapter = true)
data class TmdbAccountDetails(
    val id: Int,
    val name: String?,
    val username: String?,
    @Json(name = "avatar") val avatar: TmdbAvatar?
)

@JsonClass(generateAdapter = true)
data class TmdbAvatar(
    val gravatar: TmdbGravatar?,
    val tmdb: TmdbAvatarTmdb?
)

@JsonClass(generateAdapter = true)
data class TmdbGravatar(val hash: String?)

@JsonClass(generateAdapter = true)
data class TmdbAvatarTmdb(@Json(name = "avatar_path") val avatarPath: String?)

@JsonClass(generateAdapter = true)
data class TmdbWatchlistBody(
    @Json(name = "media_type") val mediaType: String,
    @Json(name = "media_id") val mediaId: Int,
    val watchlist: Boolean
)

@JsonClass(generateAdapter = true)
data class TmdbStatusResponse(
    val success: Boolean?,
    @Json(name = "status_code") val statusCode: Int?,
    @Json(name = "status_message") val statusMessage: String?
)
