package com.scenefinder.data.remote.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface AniListApi {

    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("/")
    suspend fun queryMedia(@Body request: AniListGraphQLRequest): AniListGraphQLResponse

    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("/")
    suspend fun executeMutation(
        @Header("Authorization") authHeader: String,
        @Body request: AniListGraphQLRequest
    ): AniListGraphQLResponse

    /** Fetch the authenticated user's full MediaList (requires Bearer token). */
    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("/")
    suspend fun queryUserLibrary(
        @Header("Authorization") authHeader: String,
        @Body request: AniListGraphQLRequest
    ): AniListUserLibraryResponse
}

/** Separate Retrofit interface pointed at https://anilist.co/api/v2/oauth/token */
interface AniListOAuthApi {
    @FormUrlEncoded
    @POST("token")
    suspend fun exchangeCodeForToken(
        @Field("grant_type")    grantType: String = "authorization_code",
        @Field("client_id")     clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("redirect_uri")  redirectUri: String,
        @Field("code")          code: String
    ): AniListTokenResponse
}

@JsonClass(generateAdapter = true)
data class AniListTokenResponse(
    @Json(name = "access_token")  val accessToken: String?,
    @Json(name = "token_type")    val tokenType: String?,
    @Json(name = "expires_in")    val expiresIn: Long?
)

@JsonClass(generateAdapter = true)
data class AniListGraphQLRequest(
    val query: String,
    val variables: Map<String, Any?>
)

@JsonClass(generateAdapter = true)
data class AniListGraphQLResponse(
    val data: AniListData?,
    val errors: List<AniListGraphQLError>? = null
)

@JsonClass(generateAdapter = true)
data class AniListGraphQLError(
    val message: String?,
    val status: Int? = null
)

@JsonClass(generateAdapter = true)
data class AniListData(
    @Json(name = "Page")
    val page: AniListPage?,
    @Json(name = "SaveMediaListEntry")
    val savedEntry: AniListSavedEntry?
)

@JsonClass(generateAdapter = true)
data class AniListSavedEntry(
    val id: Int?,
    val status: String?
)

@JsonClass(generateAdapter = true)
data class AniListPage(
    val media: List<AniListMediaItem>?
)

@JsonClass(generateAdapter = true)
data class AniListMediaItem(
    val id: Int,
    val title: AniListTitle?,
    val type: String?,
    val format: String?,
    val status: String?,
    val episodes: Int?,
    val chapters: Int?,
    val averageScore: Int?,
    val description: String?,
    val coverImage: AniListCoverImage?,
    val bannerImage: String?,
    val siteUrl: String?,
    val genres: List<String>?,
    val countryOfOrigin: String? = null
)

@JsonClass(generateAdapter = true)
data class AniListTitle(
    val romaji: String?,
    val english: String?,
    val native: String?
)

@JsonClass(generateAdapter = true)
data class AniListCoverImage(
    val extraLarge: String?,
    val large: String?
)

// ── User Library Models ──────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class AniListUserLibraryResponse(
    val data: AniListUserLibraryData?
)

@JsonClass(generateAdapter = true)
data class AniListUserLibraryData(
    @Json(name = "Viewer")
    val viewer: AniListViewer? = null,
    @Json(name = "MediaListCollection")
    val mediaListCollection: AniListMediaListCollection? = null
)

@JsonClass(generateAdapter = true)
data class AniListViewer(
    val id: Int,
    val name: String? = null
)

@JsonClass(generateAdapter = true)
data class AniListMediaListCollection(
    val lists: List<AniListMediaListGroup>?
)

@JsonClass(generateAdapter = true)
data class AniListMediaListGroup(
    val name: String?,
    val status: String?,
    val entries: List<AniListMediaListEntry>?
)

@JsonClass(generateAdapter = true)
data class AniListMediaListEntry(
    val id: Int,
    val status: String?,
    val score: Float?,
    val progress: Int?,
    val updatedAt: Long? = null,
    val createdAt: Long? = null,
    val media: AniListMediaItem?
)
