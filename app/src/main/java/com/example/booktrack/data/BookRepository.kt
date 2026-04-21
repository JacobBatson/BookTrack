package com.example.booktrack.data

import com.example.booktrack.data.local.BookDao
import com.example.booktrack.data.local.BookEntity
import com.example.booktrack.data.remote.OpenLibraryApi
import com.example.booktrack.data.remote.extractDescription
import com.example.booktrack.model.Book
import com.example.booktrack.model.Shelf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookRepository(
    private val dao: BookDao,
    private val api: OpenLibraryApi
) {
    val library: Flow<Map<Shelf, List<Book>>> = dao.getAllBooks().map { entities ->
        entities
            .map { it.toDomain() }
            .groupBy { it.shelf!! }
    }

    suspend fun search(query: String): List<Book> {
        val response = api.search(query)
        return response.docs.map { doc ->
            Book(
                key = doc.key,
                title = doc.title,
                author = doc.authorName?.firstOrNull() ?: "Unknown Author",
                coverId = doc.coverId,
                description = doc.firstSentence?.firstOrNull()
            )
        }
    }

    suspend fun addToShelf(book: Book, shelf: Shelf) {
        dao.insertBook(book.toEntity(shelf))
    }

    suspend fun moveToShelf(key: String, shelf: Shelf) {
        dao.updateShelf(key, shelf.name)
    }

    suspend fun removeFromLibrary(key: String) {
        val entity = dao.getBookByKey(key) ?: return
        dao.deleteBook(entity)
    }

    suspend fun getBookByKey(key: String): Book? =
        dao.getBookByKey(key)?.toDomain()

    suspend fun fetchDescription(book: Book): String? {
        return try {
            val detail = api.getWork(book.key)
            val description = detail.extractDescription() ?: return null
            if (dao.getBookByKey(book.key) != null) {
                dao.updateDescription(book.key, description)
            }
            description
        } catch (e: Exception) {
            null
        }
    }
}

private fun BookEntity.toDomain(): Book = Book(
    key = key,
    title = title,
    author = author,
    coverId = coverId,
    description = description,
    shelf = Shelf.valueOf(shelf)
)

private fun Book.toEntity(shelf: Shelf): BookEntity = BookEntity(
    key = key,
    title = title,
    author = author,
    coverId = coverId,
    description = description,
    shelf = shelf.name
)
