package com.example.booktrack

import com.example.booktrack.model.Book
import com.example.booktrack.model.Shelf
import com.example.booktrack.model.coverUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookTest {

    private val bookWithCover = Book(
        key = "/works/OL123W",
        title = "Dune",
        author = "Frank Herbert",
        coverId = 12345,
        description = "A sci-fi epic.",
        shelf = Shelf.READ,
        currentPage = 50
    )

    private val bookNoCover = bookWithCover.copy(coverId = null)

    @Test
    fun `coverUrl returns null when coverId is null`() {
        assertNull(bookNoCover.coverUrl())
    }

    @Test
    fun `coverUrl returns medium URL by default`() {
        assertEquals(
            "https://covers.openlibrary.org/b/id/12345-M.jpg",
            bookWithCover.coverUrl()
        )
    }

    @Test
    fun `coverUrl respects custom size parameter`() {
        assertEquals(
            "https://covers.openlibrary.org/b/id/12345-L.jpg",
            bookWithCover.coverUrl("L")
        )
        assertEquals(
            "https://covers.openlibrary.org/b/id/12345-S.jpg",
            bookWithCover.coverUrl("S")
        )
    }

    @Test
    fun `Book default currentPage is zero`() {
        val book = Book(
            key = "/works/OL1W",
            title = "Test",
            author = "Author",
            coverId = null,
            description = null
        )
        assertEquals(0, book.currentPage)
    }

    @Test
    fun `Book default shelf is null`() {
        val book = Book(
            key = "/works/OL1W",
            title = "Test",
            author = "Author",
            coverId = null,
            description = null
        )
        assertNull(book.shelf)
    }

    @Test
    fun `Book copy preserves all fields`() {
        val updated = bookWithCover.copy(currentPage = 200)
        assertEquals(200, updated.currentPage)
        assertEquals(bookWithCover.key, updated.key)
        assertEquals(bookWithCover.title, updated.title)
    }
}