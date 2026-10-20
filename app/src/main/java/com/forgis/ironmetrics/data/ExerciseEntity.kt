package com.forgis.ironmetrics.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val realOneRm: Double? = null,
    val theoreticalOneRm: Double = 0.0
)
