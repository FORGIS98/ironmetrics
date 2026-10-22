package com.forgis.ironmetrics.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Un WOD guarda siempre solo el último tiempo y las últimas notas. */
@Entity(tableName = "wods")
data class WodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val timeSeconds: Int = 0,
    val notes: String = ""
)
