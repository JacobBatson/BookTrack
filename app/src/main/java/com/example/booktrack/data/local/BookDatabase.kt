package com.example.booktrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BookEntity::class, ReadingSessionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class BookDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun readingSessionDao(): ReadingSessionDao

    companion object {
        @Volatile
        private var INSTANCE: BookDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reading_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bookKey TEXT NOT NULL,
                        bookTitle TEXT NOT NULL,
                        startPage INTEGER NOT NULL,
                        endPage INTEGER NOT NULL,
                        startTimeMs INTEGER NOT NULL,
                        endTimeMs INTEGER NOT NULL,
                        durationMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): BookDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    BookDatabase::class.java,
                    "book_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}