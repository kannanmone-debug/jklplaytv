package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MediaEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        VideoBookmarkEntity::class,
        VaultMediaEntity::class,
        ExcludedFolderEntity::class,
        FolderShortcutEntity::class,
        EqualizerPresetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BnjMediaDatabase : RoomDatabase() {
    abstract fun mediaDao(): BnjMediaDao

    companion object {
        @Volatile
        private var INSTANCE: BnjMediaDatabase? = null

        fun getInstance(context: Context): BnjMediaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BnjMediaDatabase::class.java,
                    "bnj_media_pro_local.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
