package com.scenefinder.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "enriched_media")
data class EnrichedMediaEntity(
    @PrimaryKey
    val id: String,
    val searchKey: String,
    val cleanTitle: String,
    val englishTitle: String?,
    val originalTitle: String?,
    val mediaType: String,
    val year: Int?,
    val season: Int?,
    val episode: Int?,
    val chapter: Int?,
    val overview: String?,
    val posterUrl: String?,
    val bannerUrl: String?,
    val rating: Float?,
    val sourceService: String,
    val directWebUrl: String?,
    val userStatus: String = "PLANNING",
    val cachedAt: Long = System.currentTimeMillis()
)
