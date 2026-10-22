package com.forgis.ironmetrics.data

import androidx.room.withTransaction
import com.google.gson.Gson

data class BackupData(
    val version: Int = 1,
    val exercises: List<ExerciseEntity> = emptyList(),
    val setLogs: List<SetLogEntity> = emptyList(),
    // null en backups anteriores a los WODs
    val wods: List<WodEntity>? = null
)

class BackupManager(
    private val database: AppDatabase,
    private val wodDatabase: WodDatabase,
    private val gson: Gson = Gson()
) {
    private val dao = database.exerciseDao()
    private val wodDao = wodDatabase.wodDao()

    suspend fun exportToJson(): String {
        val wods = wodDao.getAllWodsOnce()
        return database.withTransaction {
            gson.toJson(
                BackupData(
                    exercises = dao.getAllExercisesOnce(),
                    setLogs = dao.getAllSetLogsOnce(),
                    wods = wods
                )
            )
        }
    }

    /**
     * @param replace true: borra todo y restaura el backup tal cual.
     * false: fusiona; ejercicios por nombre (conserva el mayor RM) y series sin duplicar.
     */
    suspend fun importFromJson(json: String, replace: Boolean = false) {
        val backup = gson.fromJson(json, BackupData::class.java)
            ?: throw IllegalArgumentException("Empty backup")
        database.withTransaction {
            if (replace) restore(backup) else merge(backup)
        }
        backup.wods?.let { incoming ->
            wodDatabase.withTransaction {
                if (replace) restoreWods(incoming) else mergeWods(incoming)
            }
        }
    }

    private suspend fun restore(backup: BackupData) {
        dao.deleteAllExercises()
        backup.exercises.forEach { dao.upsertExercise(it) }
        dao.insertSetLogs(backup.setLogs.filter { log -> backup.exercises.any { it.id == log.exerciseId } })
    }

    private suspend fun merge(backup: BackupData) {
        val existing = dao.getAllExercisesOnce().associateBy { it.name.lowercase() }.toMutableMap()
        val idMap = mutableMapOf<Long, Long>()

        for (incoming in backup.exercises) {
            val key = incoming.name.lowercase()
            val current = existing[key]
            if (current == null) {
                val newId = dao.upsertExercise(incoming.copy(id = 0))
                idMap[incoming.id] = newId
                existing[key] = incoming.copy(id = newId)
            } else {
                val merged = current.copy(
                    realOneRm = listOfNotNull(current.realOneRm, incoming.realOneRm).maxOrNull(),
                    theoreticalOneRm = maxOf(current.theoreticalOneRm, incoming.theoreticalOneRm)
                )
                if (merged != current) dao.upsertExercise(merged)
                existing[key] = merged
                idMap[incoming.id] = current.id
            }
        }

        val knownLogs = dao.getAllSetLogsOnce()
            .map { listOf(it.exerciseId, it.date, it.weight, it.reps) }
            .toMutableSet()
        val newLogs = backup.setLogs.mapNotNull { log ->
            val exerciseId = idMap[log.exerciseId] ?: return@mapNotNull null
            val signature = listOf(exerciseId, log.date, log.weight, log.reps)
            if (!knownLogs.add(signature)) null else log.copy(id = 0, exerciseId = exerciseId)
        }
        dao.insertSetLogs(newLogs)
    }

    private suspend fun restoreWods(incoming: List<WodEntity>) {
        wodDao.deleteAllWods()
        wodDao.upsertWods(incoming)
    }

    // Los WODs ya existentes (por nombre) conservan su último valor; solo se añaden los nuevos
    private suspend fun mergeWods(incoming: List<WodEntity>) {
        val knownNames = wodDao.getAllWodsOnce().map { it.name.lowercase() }.toMutableSet()
        incoming.filter { knownNames.add(it.name.lowercase()) }
            .forEach { wodDao.upsertWod(it.copy(id = 0)) }
    }
}
