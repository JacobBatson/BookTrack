package com.example.booktrack.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val key: String,
    val title: String,
    val author: String,
    val coverId: Int?,
    val description: String?,
    val shelf: String,
    val currentPage: Int = 0
)
