package com.biomedixai.ricefieldsamplelogger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SampleEntity::class], version = 1, exportSchema = false)
abstract class SampleDatabase : RoomDatabase() {

    abstract fun sampleDao(): SampleDao

    companion object {
        @Volatile
        private var INSTANCE: SampleDatabase? = null

        fun getDatabase(context: Context): SampleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SampleDatabase::class.java,
                    "rice_sample_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
