package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ChatMessageDao
import com.example.data.local.dao.PendingQueueDao
import com.example.data.local.entity.LocalChatMessageEntity
import com.example.data.local.entity.PendingQueueEntity

/**
 * AppRoomDatabase manages the SQLite database via Room for on-device persistence
 * of pending offline actions and local chat message caching.
 */
@Database(
    entities = [
        PendingQueueEntity::class,
        LocalChatMessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppRoomDatabase : RoomDatabase() {

    abstract fun pendingQueueDao(): PendingQueueDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppRoomDatabase? = null

        fun getInstance(context: Context): AppRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppRoomDatabase::class.java,
                    "echotalk_room.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun createInMemory(context: Context): AppRoomDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppRoomDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
