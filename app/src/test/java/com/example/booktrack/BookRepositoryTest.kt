package com.example.booktrack

import com.example.booktrack.data.BookRepository
import com.example.booktrack.data.local.BookDao
import com.example.booktrack.data.local.BookEntity
import com.example.booktrack.data.local.ReadingSessionDao
import com.example.booktrack.data.local.ReadingSessionEntity
import com.example.booktrack.data.remote.OpenLibraryApi
import com.example.booktrack.data.remote.SearchDoc
import com.example.booktrack.data.remote.SearchResponse
import com.example.booktrack.data.remote.WorkDetail
import com.example.booktrack.model.Book
import com.example.booktrack.model.Shelf
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BookRepositoryTest {

    private val mockDao: BookDao = mockk(relaxed = true)
    private val mockSessionDao: ReadingSessionDao = mockk(relaxed = true)
    private val mockApi: OpenLibraryApi = mockk()

    private lateinit var repository: BookRepository

    @Before
    fun setUp() {
        every { mockDao.getAllBooks() } returns flowOf(emptyList())
        every { mockSessionDao.getSessionsForBook(any()) } returns flowOf(emptyList())
        repository = BookRepository(mockDao, mockSessionDao, mockApi)
    }

    // --- library flow ---

    @Test
    fun `library emits empty map when no books in DB`() = runTest {
        val result = repository.library.first()
        assertEquals(emptyMap<Shelf, List<Book>>(), result)
    }

    @Test
    fun `library groups books by shelf`() = runTest {
        val entities = listOf(
            BookEntity("/works/OL1W", "Book A", "Author", null, null, "READ", 0),
            BookEntity("/works/OL2W", "Book B", "Author", null, null, "READ", 0),
            BookEntity("/works/OL3W", "Book C", "Author", null, null, "CURRENTLY_READING", 10)
        )
        every { mockDao.getAllBooks() } returns flowOf(entities)
        val repo = BookRepository(mockDao, mockSessionDao, mockApi)

        val library = repo.library.first()

        assertEquals(2, library[Shelf.READ]?.size)
        assertEquals(1, library[Shelf.CURRENTLY_READING]?.size)
        assertEquals("Book C", library[Shelf.CURRENTLY_READING]?.first()?.title)
    }

    @Test
    fun `library maps entity fields to domain Book`() = runTest {
        val entity = BookEntity("/works/OL1W", "Dune", "Frank Herbert", 12345, "Desc", "WANT_TO_READ", 42)
        every { mockDao.getAllBooks() } returns flowOf(listOf(entity))
        val repo = BookRepository(mockDao, mockSessionDao, mockApi)

        val book = repo.library.first()[Shelf.WANT_TO_READ]!!.first()

        assertEquals("/works/OL1W", book.key)
        assertEquals("Dune", book.title)
        assertEquals("Frank Herbert", book.author)
        assertEquals(12345, book.coverId)
        assertEquals("Desc", book.description)
        assertEquals(Shelf.WANT_TO_READ, book.shelf)
        assertEquals(42, book.currentPage)
    }

    // --- search ---

    @Test
    fun `search maps API docs to Book list`() = runTest {
        val docs = listOf(
            SearchDoc("/works/OL1W", "Book A", listOf("Author A"), 100, listOf("First sentence."))
        )
        coEvery { mockApi.search(any(), any(), any()) } returns SearchResponse(docs)

        val results = repository.search("query")

        assertEquals(1, results.size)
        assertEquals("/works/OL1W", results[0].key)
        assertEquals("Book A", results[0].title)
        assertEquals("Author A", results[0].author)
        assertEquals(100, results[0].coverId)
        assertEquals("First sentence.", results[0].description)
    }

    @Test
    fun `search uses Unknown Author when authorName is null`() = runTest {
        val docs = listOf(SearchDoc("/works/OL1W", "No Author Book", null, null, null))
        coEvery { mockApi.search(any(), any(), any()) } returns SearchResponse(docs)

        val results = repository.search("query")

        assertEquals("Unknown Author", results[0].author)
    }

    @Test
    fun `search uses Unknown Author when authorName list is empty`() = runTest {
        val docs = listOf(SearchDoc("/works/OL1W", "Empty Authors", emptyList(), null, null))
        coEvery { mockApi.search(any(), any(), any()) } returns SearchResponse(docs)

        val results = repository.search("query")

        assertEquals("Unknown Author", results[0].author)
    }

    @Test
    fun `search returns null description when firstSentence is null`() = runTest {
        val docs = listOf(SearchDoc("/works/OL1W", "Title", listOf("Author"), null, null))
        coEvery { mockApi.search(any(), any(), any()) } returns SearchResponse(docs)

        val results = repository.search("query")

        assertNull(results[0].description)
    }

    @Test
    fun `search returns empty list when docs is empty`() = runTest {
        coEvery { mockApi.search(any(), any(), any()) } returns SearchResponse(emptyList())

        val results = repository.search("query")

        assertEquals(emptyList<Book>(), results)
    }

    // --- addToShelf ---

    @Test
    fun `addToShelf inserts correct entity into dao`() = runTest {
        val book = Book("/works/OL1W", "Dune", "Frank Herbert", 12345, "Desc", currentPage = 5)
        coJustRun { mockDao.insertBook(any()) }

        repository.addToShelf(book, Shelf.READ)

        coVerify {
            mockDao.insertBook(
                BookEntity("/works/OL1W", "Dune", "Frank Herbert", 12345, "Desc", "READ", 5)
            )
        }
    }

    // --- moveToShelf ---

    @Test
    fun `moveToShelf calls dao updateShelf with shelf name`() = runTest {
        coJustRun { mockDao.updateShelf(any(), any()) }

        repository.moveToShelf("/works/OL1W", Shelf.CURRENTLY_READING)

        coVerify { mockDao.updateShelf("/works/OL1W", "CURRENTLY_READING") }
    }

    // --- removeFromLibrary ---

    @Test
    fun `removeFromLibrary deletes entity when found`() = runTest {
        val entity = BookEntity("/works/OL1W", "Dune", "Author", null, null, "READ")
        coEvery { mockDao.getBookByKey("/works/OL1W") } returns entity
        coJustRun { mockDao.deleteBook(any()) }

        repository.removeFromLibrary("/works/OL1W")

        coVerify { mockDao.deleteBook(entity) }
    }

    @Test
    fun `removeFromLibrary does nothing when entity not found`() = runTest {
        coEvery { mockDao.getBookByKey(any()) } returns null

        repository.removeFromLibrary("/works/OL99W")

        coVerify(exactly = 0) { mockDao.deleteBook(any()) }
    }

    // --- getBookByKey ---

    @Test
    fun `getBookByKey returns mapped domain Book when entity exists`() = runTest {
        val entity = BookEntity("/works/OL1W", "Dune", "Frank Herbert", 42, "Desc", "READ", 10)
        coEvery { mockDao.getBookByKey("/works/OL1W") } returns entity

        val book = repository.getBookByKey("/works/OL1W")

        assertEquals("/works/OL1W", book?.key)
        assertEquals("Dune", book?.title)
        assertEquals(Shelf.READ, book?.shelf)
        assertEquals(10, book?.currentPage)
    }

    @Test
    fun `getBookByKey returns null when entity not found`() = runTest {
        coEvery { mockDao.getBookByKey(any()) } returns null

        val result = repository.getBookByKey("/works/missing")

        assertNull(result)
    }

    // --- insertSession ---

    @Test
    fun `insertSession delegates to sessionDao with correct entity`() = runTest {
        coEvery { mockSessionDao.insertSession(any()) } returns 1L

        repository.insertSession(
            bookKey = "/works/OL1W",
            bookTitle = "Dune",
            startPage = 10,
            endPage = 50,
            startTimeMs = 1000L,
            endTimeMs = 5000L,
            durationMs = 4000L
        )

        coVerify {
            mockSessionDao.insertSession(
                ReadingSessionEntity(
                    bookKey = "/works/OL1W",
                    bookTitle = "Dune",
                    startPage = 10,
                    endPage = 50,
                    startTimeMs = 1000L,
                    endTimeMs = 5000L,
                    durationMs = 4000L
                )
            )
        }
    }

    // --- updateCurrentPage ---

    @Test
    fun `updateCurrentPage delegates to dao`() = runTest {
        coJustRun { mockDao.updateCurrentPage(any(), any()) }

        repository.updateCurrentPage("/works/OL1W", 42)

        coVerify { mockDao.updateCurrentPage("/works/OL1W", 42) }
    }

    // --- getSessionsForBook ---

    @Test
    fun `getSessionsForBook returns flow from sessionDao`() = runTest {
        val sessions = listOf(
            ReadingSessionEntity(1L, "/works/OL1W", "Dune", 10, 50, 1000L, 5000L, 4000L)
        )
        every { mockSessionDao.getSessionsForBook("/works/OL1W") } returns flowOf(sessions)

        val result = repository.getSessionsForBook("/works/OL1W").first()

        assertEquals(1, result.size)
        assertEquals("Dune", result[0].bookTitle)
    }

    // --- fetchDescription ---

    @Test
    fun `fetchDescription returns string and updates DB when book exists`() = runTest {
        val book = Book("/works/OL1W", "Dune", "Author", null, null)
        val detail = WorkDetail(JsonPrimitive("A great description."))
        coEvery { mockApi.getWork("/works/OL1W") } returns detail
        coEvery { mockDao.getBookByKey("/works/OL1W") } returns
                BookEntity("/works/OL1W", "Dune", "Author", null, null, "READ")
        coJustRun { mockDao.updateDescription(any(), any()) }

        val result = repository.fetchDescription(book)

        assertEquals("A great description.", result)
        coVerify { mockDao.updateDescription("/works/OL1W", "A great description.") }
    }

    @Test
    fun `fetchDescription skips DB update when book not in library`() = runTest {
        val book = Book("/works/OL1W", "Dune", "Author", null, null)
        val detail = WorkDetail(JsonPrimitive("Some description."))
        coEvery { mockApi.getWork("/works/OL1W") } returns detail
        coEvery { mockDao.getBookByKey("/works/OL1W") } returns null

        val result = repository.fetchDescription(book)

        assertEquals("Some description.", result)
        coVerify(exactly = 0) { mockDao.updateDescription(any(), any()) }
    }

    @Test
    fun `fetchDescription returns null when description extraction yields null`() = runTest {
        val book = Book("/works/OL1W", "Dune", "Author", null, null)
        val detail = WorkDetail(JsonObject())
        coEvery { mockApi.getWork("/works/OL1W") } returns detail

        val result = repository.fetchDescription(book)

        assertNull(result)
    }

    @Test
    fun `fetchDescription returns null on API exception`() = runTest {
        val book = Book("/works/OL1W", "Dune", "Author", null, null)
        coEvery { mockApi.getWork(any()) } throws RuntimeException("Network error")

        val result = repository.fetchDescription(book)

        assertNull(result)
    }
}
