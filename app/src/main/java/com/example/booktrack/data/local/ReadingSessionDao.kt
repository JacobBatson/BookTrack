package com.example.booktrack.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingSessionDao {
    @Insert
    suspend fun insertSession(session: ReadingSessionEntity): Long

    @Query("SELECT * FROM reading_sessions WHERE bookKey = :bookKey ORDER BY startTimeMs DESC")
    fun getSessionsForBook(bookKey: String): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions ORDER BY startTimeMs DESC")
    fun getAllSessions(): Flow<List<ReadingSessionEntity>>

    @Delete
    suspend fun deleteSession(session: ReadingSessionEntity)
}