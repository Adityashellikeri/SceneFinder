package com.scenefinder.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.scenefinder.data.local.dao.EnrichedMediaDao
import com.scenefinder.data.local.entity.EnrichedMediaEntity

@Database(
    entities = [EnrichedMediaEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun enrichedMediaDao(): EnrichedMediaDao
}
