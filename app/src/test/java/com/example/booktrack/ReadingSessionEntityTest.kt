package com.example.booktrack

import com.example.booktrack.data.local.ReadingSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingSessionEntityTest {

    @Test
    fun `ReadingSessionEntity stores all fields correctly`() {
        val session = ReadingSessionEntity(
            id = 1L,
            bookKey = "/works/OL1W",
            bookTitle = "Dune",
            startPage = 10,
            endPage = 50,
            startTimeMs = 1000L,
            endTimeMs = 5000L,
            durationMs = 4000L
        )
        assertEquals(1L, session.id)
        assertEquals("/works/OL1W", session.bookKey)
        assertEquals("Dune", session.bookTitle)
        assertEquals(10, session.startPage)
        assertEquals(50, session.endPage)
        assertEquals(1000L, session.startTimeMs)
        assertEquals(5000L, session.endTimeMs)
        assertEquals(4000L, session.durationMs)
    }

    @Test
    fun `ReadingSessionEntity id defaults to zero`() {
        val session = ReadingSessionEntity(
            bookKey = "/works/OL1W",
            bookTitle = "Dune",
            startPage = 0,
            endPage = 10,
            startTimeMs = 0L,
            endTimeMs = 100L,
            durationMs = 100L
        )
        assertEquals(0L, session.id)
    }

    @Test
    fun `ReadingSessionEntity equality holds for same data`() {
        val a = ReadingSessionEntity(1L, "/works/OL1W", "Dune", 10, 50, 1000L, 5000L, 4000L)
        val b = ReadingSessionEntity(1L, "/works/OL1W", "Dune", 10, 50, 1000L, 5000L, 4000L)
        assertEquals(a, b)
    }

    @Test
    fun `ReadingSessionEntity copy changes only specified fields`() {
        val original = ReadingSessionEntity(1L, "/works/OL1W", "Dune", 10, 50, 1000L, 5000L, 4000L)
        val updated = original.copy(endPage = 100, durationMs = 8000L)
        assertEquals(100, updated.endPage)
        assertEquals(8000L, updated.durationMs)
        assertEquals(original.bookKey, updated.bookKey)
        assertEquals(original.id, updated.id)
    }
}
