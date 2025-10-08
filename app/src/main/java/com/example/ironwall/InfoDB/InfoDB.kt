package com.example.ironwall.InfoDB

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UserTable::class], version = 3, exportSchema = false) // <-- incremented version
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    // Singleton pattern to ensure only one instance of the database is created.
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "user_database.db"
                )
                    .fallbackToDestructiveMigration() // <-- allow destructive migration for dev
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
