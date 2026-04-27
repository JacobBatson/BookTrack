package com.example.booktrack

import com.example.booktrack.data.local.BookEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookEntityTest {

    @Test
    fun `BookEntity fields are stored correctly`() {
        val entity = BookEntity(
            key = "/works/OL1W",
            title = "Test Title",
            author = "Test Author",
            coverId = 42,
            description = "A description.",
            shelf = "READ",
            currentPage = 10
        )
        assertEquals("/works/OL1W", entity.key)
        assertEquals("Test Title", entity.title)
        assertEquals("Test Author", entity.author)
        assertEquals(42, entity.coverId)
        assertEquals("A description.", entity.description)
        assertEquals("READ", entity.shelf)
        assertEquals(10, entity.currentPage)
    }

    @Test
    fun `BookEntity currentPage defaults to zero`() {
        val entity = BookEntity(
            key = "/works/OL2W",
            title = "No Page",
            author = "Author",
            coverId = null,
            description = null,
            shelf = "WANT_TO_READ"
        )
        assertEquals(0, entity.currentPage)
    }

    @Test
    fun `BookEntity coverId and description can be null`() {
        val entity = BookEntity(
            key = "/works/OL3W",
            title = "Nulls",
            author = "Author",
            coverId = null,
            description = null,
            shelf = "READ"
        )
        assertNull(entity.coverId)
        assertNull(entity.description)
    }

    @Test
    fun `BookEntity equality holds for same data`() {
        val a = BookEntity("/works/OL1W", "Title", "Author", 1, "Desc", "READ", 5)
        val b = BookEntity("/works/OL1W", "Title", "Author", 1, "Desc", "READ", 5)
        assertEquals(a, b)
    }

    @Test
    fun `BookEntity copy modifies only specified fields`() {
        val original = BookEntity("/works/OL1W", "Title", "Author", 1, "Desc", "READ", 5)
        val updated = original.copy(shelf = "CURRENTLY_READING", currentPage = 20)
        assertEquals("CURRENTLY_READING", updated.shelf)
        assertEquals(20, updated.currentPage)
        assertEquals(original.key, updated.key)
    }
}
