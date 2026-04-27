package com.example.booktrack

import com.example.booktrack.data.remote.SearchDoc
import com.example.booktrack.data.remote.SearchResponse
import com.example.booktrack.data.remote.WorkDetail
import com.example.booktrack.data.remote.extractDescription
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenLibraryModelsTest {

    // --- WorkDetail.extractDescription() ---

    @Test
    fun `extractDescription returns null when description is null`() {
        val detail = WorkDetail(description = null)
        assertNull(detail.extractDescription())
    }

    @Test
    fun `extractDescription returns string when description is JsonPrimitive`() {
        val detail = WorkDetail(description = JsonPrimitive("A great book."))
        assertEquals("A great book.", detail.extractDescription())
    }

    @Test
    fun `extractDescription returns value field when description is JsonObject`() {
        val obj = JsonObject().apply { addProperty("value", "Nested description.") }
        val detail = WorkDetail(description = obj)
        assertEquals("Nested description.", detail.extractDescription())
    }

    @Test
    fun `extractDescription returns null when JsonObject has no value field`() {
        val obj = JsonObject().apply { addProperty("type", "/type/text") }
        val detail = WorkDetail(description = obj)
        assertNull(detail.extractDescription())
    }

    @Test
    fun `extractDescription returns null for JsonArray`() {
        val array = com.google.gson.JsonArray().apply { add("item") }
        val detail = WorkDetail(description = array)
        assertNull(detail.extractDescription())
    }

    // --- SearchDoc ---

    @Test
    fun `SearchDoc fields are accessible`() {
        val doc = SearchDoc(
            key = "/works/OL1W",
            title = "Test Book",
            authorName = listOf("Author One", "Author Two"),
            coverId = 999,
            firstSentence = listOf("Once upon a time.")
        )
        assertEquals("/works/OL1W", doc.key)
        assertEquals("Test Book", doc.title)
        assertEquals("Author One", doc.authorName?.first())
        assertEquals(999, doc.coverId)
        assertEquals("Once upon a time.", doc.firstSentence?.first())
    }

    @Test
    fun `SearchDoc allows null optional fields`() {
        val doc = SearchDoc(
            key = "/works/OL2W",
            title = "No Author",
            authorName = null,
            coverId = null,
            firstSentence = null
        )
        assertNull(doc.authorName)
        assertNull(doc.coverId)
        assertNull(doc.firstSentence)
    }

    // --- SearchResponse ---

    @Test
    fun `SearchResponse holds list of docs`() {
        val docs = listOf(
            SearchDoc("/works/OL1W", "Book A", listOf("Author"), 1, null)
        )
        val response = SearchResponse(docs)
        assertEquals(1, response.docs.size)
        assertEquals("Book A", response.docs.first().title)
    }
}