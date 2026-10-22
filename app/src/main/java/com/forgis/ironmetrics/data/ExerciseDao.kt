package com.forgis.ironmetrics.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM set_logs WHERE exerciseId = :exerciseId ORDER BY date DESC")
    fun getHistory(exerciseId: Long): Flow<List<SetLogEntity>>

    // REPLACE sobre una fila existente con ID borraría en cascada sus series; Room hace UPDATE en @Upsert
    @androidx.room.Upsert
    suspend fun upsertExercise(exercise: ExerciseEntity): Long

    @Insert
    suspend fun insertSetLog(setLog: SetLogEntity): Long

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercises")
    suspend fun getAllExercisesOnce(): List<ExerciseEntity>

    @Query("SELECT * FROM set_logs")
    suspend fun getAllSetLogsOnce(): List<SetLogEntity>

    @Insert
    suspend fun insertSetLogs(setLogs: List<SetLogEntity>)

    @Query("DELETE FROM exercises")
    suspend fun deleteAllExercises()

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExerciseById(id: Long)
}
