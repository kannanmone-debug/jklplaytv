package com.example.movies.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MovieMetadataEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BnjMovieDatabase : RoomDatabase() {
    abstract fun movieMetadataDao(): MovieMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: BnjMovieDatabase? = null

        fun getInstance(context: Context): BnjMovieDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BnjMovieDatabase::class.java,
                    "bnj_movies_metadata.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
