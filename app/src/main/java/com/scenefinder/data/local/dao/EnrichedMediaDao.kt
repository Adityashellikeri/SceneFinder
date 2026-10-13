package com.scenefinder.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.scenefinder.data.local.entity.EnrichedMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EnrichedMediaDao {

    @Query("SELECT * FROM enriched_media WHERE searchKey = :searchKey LIMIT 1")
    suspend fun getBySearchKey(searchKey: String): EnrichedMediaEntity?

    @Query("SELECT * FROM enriched_media WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EnrichedMediaEntity?

    @Query("SELECT * FROM enriched_media ORDER BY cachedAt DESC")
    fun getAllHistoryPaged(): PagingSource<Int, EnrichedMediaEntity>

    // Paginated for UI -- returns all sorted by newest first (Instagram-feed style)
    @Query("SELECT * FROM enriched_media ORDER BY cachedAt DESC")
    fun getAllHistory(): Flow<List<EnrichedMediaEntity>>

    @Query("SELECT * FROM enriched_media ORDER BY cachedAt DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<EnrichedMediaEntity>>

    @Query("UPDATE enriched_media SET userStatus = :status, cachedAt = :cachedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, cachedAt: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: EnrichedMediaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<EnrichedMediaEntity>)

    @Query("DELETE FROM enriched_media WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM enriched_media")
    suspend fun clearAll()
}
