package com.forgis.ironmetrics.data

import kotlinx.coroutines.flow.Flow

class WodRepository(database: WodDatabase) {
    private val dao = database.wodDao()

    val wods: Flow<List<WodEntity>> = dao.getAllWods()

    /** Inserta (id = 0) o sobrescribe el WOD con el último tiempo y notas. */
    suspend fun saveWod(wod: WodEntity): Long = dao.upsertWod(wod)

    suspend fun deleteWod(id: Long) = dao.deleteWodById(id)
}
