package com.example.booktrack.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_sessions")
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookKey: String,
    val bookTitle: String,
    val startPage: Int,
    val endPage: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long
)