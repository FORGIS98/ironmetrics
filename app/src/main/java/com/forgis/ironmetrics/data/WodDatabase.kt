package com.forgis.ironmetrics.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WodEntity::class], version = 1, exportSchema = false)
abstract class WodDatabase : RoomDatabase() {

    abstract fun wodDao(): WodDao

    companion object {
        @Volatile
        private var instance: WodDatabase? = null

        fun getInstance(context: Context): WodDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WodDatabase::class.java,
                    "ironmetrics_wods.db"
                ).build().also { instance = it }
            }
    }
}
