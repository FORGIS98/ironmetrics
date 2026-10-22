package com.forgis.ironmetrics.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WodDao {

    @Query("SELECT * FROM wods ORDER BY name COLLATE NOCASE")
    fun getAllWods(): Flow<List<WodEntity>>

    @Query("SELECT * FROM wods")
    suspend fun getAllWodsOnce(): List<WodEntity>

    @Upsert
    suspend fun upsertWod(wod: WodEntity): Long

    @Upsert
    suspend fun upsertWods(wods: List<WodEntity>)

    @Query("DELETE FROM wods WHERE id = :id")
    suspend fun deleteWodById(id: Long)

    @Query("DELETE FROM wods")
    suspend fun deleteAllWods()
}
