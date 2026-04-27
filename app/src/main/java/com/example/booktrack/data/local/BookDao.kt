package com.example.booktrack.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("SELECT * FROM books WHERE `key` = :key LIMIT 1")
    suspend fun getBookByKey(key: String): BookEntity?

    @Query("UPDATE books SET shelf = :shelf WHERE `key` = :key")
    suspend fun updateShelf(key: String, shelf: String)

    @Query("UPDATE books SET description = :description WHERE `key` = :key")
    suspend fun updateDescription(key: String, description: String)

    @Query("UPDATE books SET currentPage = :currentPage WHERE `key` = :key")
    suspend fun updateCurrentPage(key: String, currentPage: Int)
}
