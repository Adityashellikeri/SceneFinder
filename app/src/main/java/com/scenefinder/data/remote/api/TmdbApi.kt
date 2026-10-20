package com.scenefinder.data.remote.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {

    @GET("3/search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("api_key") apiKey: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("language") language: String = "en-US"
    ): TmdbSearchResponse

    @GET("3/movie/{movie_id}/release_dates")
    suspend fun getMovieReleaseDates(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): TmdbMovieReleaseDatesResponse

    @GET("3/tv/{tv_id}/content_ratings")
    suspend fun getTvContentRatings(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String
    ): TmdbTvContentRatingsResponse

    @GET("3/trending/movie/week")
    suspend fun getTrendingMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TmdbSearchResponse

    @GET("3/trending/tv/week")
    suspend fun getTrendingTv(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TmdbSearchResponse

    @GET("3/movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("3/tv/top_rated")
    suspend fun getTopRatedTv(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbSearchResponse
}

@JsonClass(generateAdapter = true)
data class TmdbMovieReleaseDatesResponse(
    val id: Int?,
    val results: List<TmdbCountryReleaseDates>?
)

@JsonClass(generateAdapter = true)
data class TmdbCountryReleaseDates(
    @Json(name = "iso_3166_1") val iso3166_1: String?,
    @Json(name = "release_dates") val releaseDates: List<TmdbReleaseDateItem>?
)

@JsonClass(generateAdapter = true)
data class TmdbReleaseDateItem(
    val certification: String?,
    val type: Int? = null
)

@JsonClass(generateAdapter = true)
data class TmdbTvContentRatingsResponse(
    val id: Int?,
    val results: List<TmdbCountryContentRating>?
)

@JsonClass(generateAdapter = true)
data class TmdbCountryContentRating(
    @Json(name = "iso_3166_1") val iso3166_1: String?,
    val rating: String?
)

@JsonClass(generateAdapter = true)
data class TmdbSearchResponse(
    val page: Int?,
    val results: List<TmdbItem>?,
    @Json(name = "total_results") val totalResults: Int?
)

@JsonClass(generateAdapter = true)
data class TmdbItem(
    val id: Int,
    @Json(name = "media_type") val mediaType: String?,
    val title: String?,
    val name: String?,
    @Json(name = "original_title") val originalTitle: String?,
    @Json(name = "original_name") val originalName: String?,
    val overview: String?,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "backdrop_path") val backdropPath: String?,
    @Json(name = "release_date") val releaseDate: String?,
    @Json(name = "first_air_date") val firstAirDate: String?,
    @Json(name = "vote_average") val voteAverage: Float?,
    @Json(name = "vote_count") val voteCount: Int?,
    val adult: Boolean? = null,
    @Json(name = "genre_ids") val genreIds: List<Int>? = null
) {
    val displayTitle: String
        get() = title ?: name ?: originalTitle ?: originalName ?: "Unknown"

    val fullPosterUrl: String?
        get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }

    val fullBackdropUrl: String?
        get() = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }
}
