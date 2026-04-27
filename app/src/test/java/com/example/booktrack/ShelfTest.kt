package com.example.booktrack

import com.example.booktrack.model.Shelf
import org.junit.Assert.assertEquals
import org.junit.Test

class ShelfTest {

    @Test
    fun `CURRENTLY_READING has correct label`() {
        assertEquals("Currently Reading", Shelf.CURRENTLY_READING.label)
    }

    @Test
    fun `READ has correct label`() {
        assertEquals("Read", Shelf.READ.label)
    }

    @Test
    fun `WANT_TO_READ has correct label`() {
        assertEquals("Want to Read", Shelf.WANT_TO_READ.label)
    }

    @Test
    fun `enum has exactly three values`() {
        assertEquals(3, Shelf.entries.size)
    }

    @Test
    fun `valueOf returns correct enum constant`() {
        assertEquals(Shelf.READ, Shelf.valueOf("READ"))
        assertEquals(Shelf.CURRENTLY_READING, Shelf.valueOf("CURRENTLY_READING"))
        assertEquals(Shelf.WANT_TO_READ, Shelf.valueOf("WANT_TO_READ"))
    }
}