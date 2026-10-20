package com.forgis.ironmetrics.data

import androidx.room.withTransaction
import com.forgis.ironmetrics.util.BrzyckiUtils
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(
    private val database: AppDatabase
) {
    private val dao = database.exerciseDao()

    val exercises: Flow<List<ExerciseEntity>> = dao.getAllExercises()

    fun history(exerciseId: Long): Flow<List<SetLogEntity>> = dao.getHistory(exerciseId)

    suspend fun saveExercise(exercise: ExerciseEntity): Long = dao.upsertExercise(exercise)

    /**
     * Registra una serie y actualiza los RM del ejercicio:
     * - 1 rep: actualiza el RM real si supera al anterior.
     * - 2..10 reps: actualiza el RM teórico (Brzycki) si supera al anterior.
     * La serie se marca como PR si ha mejorado alguno de los dos.
     */
    suspend fun logSet(
        exerciseId: Long,
        weight: Double,
        reps: Int,
        date: Long = System.currentTimeMillis()
    ): SetLogEntity = database.withTransaction {
        require(weight > 0.0) { "weight must be > 0" }
        require(reps >= 1) { "reps must be >= 1" }
        val exercise = dao.getExerciseById(exerciseId)
            ?: throw IllegalArgumentException("Exercise $exerciseId not found")

        var updated = exercise
        var isPr = false
        when {
            reps == 1 -> {
                if (exercise.realOneRm == null || weight > exercise.realOneRm) {
                    updated = exercise.copy(realOneRm = weight)
                    isPr = true
                }
            }
            reps in 2..10 -> {
                val theoretical = BrzyckiUtils.calculateTheoreticalOneRm(weight, reps)
                if (theoretical > exercise.theoreticalOneRm) {
                    updated = exercise.copy(theoreticalOneRm = theoretical)
                    isPr = true
                }
            }
        }
        if (updated != exercise) dao.upsertExercise(updated)

        val log = SetLogEntity(
            exerciseId = exerciseId,
            date = date,
            weight = weight,
            reps = reps,
            isPr = isPr
        )
        log.copy(id = dao.insertSetLog(log))
    }
}
