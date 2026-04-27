package com.example.booktrack

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.booktrack.data.local.BookDao
import com.example.booktrack.data.local.BookDatabase
import com.example.booktrack.data.local.BookEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDaoTest {

    private lateinit var db: BookDatabase
    private lateinit var dao: BookDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, BookDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.bookDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entity(
        key: String = "/works/OL1W",
        title: String = "Test Book",
        author: String = "Test Author",
        coverId: Int? = null,
        description: String? = null,
        shelf: String = "READ",
        currentPage: Int = 0
    ) = BookEntity(key, title, author, coverId, description, shelf, currentPage)

    // --- getAllBooks ---

    @Test
    fun getAllBooks_emptyOnStart() = runTest {
        val books = dao.getAllBooks().first()
        assertEquals(emptyList<BookEntity>(), books)
    }

    @Test
    fun getAllBooks_returnsInsertedBooks() = runTest {
        dao.insertBook(entity("/works/OL1W", "Book A"))
        dao.insertBook(entity("/works/OL2W", "Book B"))

        val books = dao.getAllBooks().first()
        assertEquals(2, books.size)
    }

    // --- insertBook ---

    @Test
    fun insertBook_storesAllFields() = runTest {
        val original = entity("/works/OL1W", "Dune", "Frank Herbert", 42, "Desc", "CURRENTLY_READING", 10)
        dao.insertBook(original)

        val books = dao.getAllBooks().first()
        val stored = books.first()
        assertEquals(original, stored)
    }

    @Test
    fun insertBook_replacesOnConflict() = runTest {
        dao.insertBook(entity("/works/OL1W", "Old Title", shelf = "READ"))
        dao.insertBook(entity("/works/OL1W", "New Title", shelf = "WANT_TO_READ"))

        val books = dao.getAllBooks().first()
        assertEquals(1, books.size)
        assertEquals("New Title", books[0].title)
        assertEquals("WANT_TO_READ", books[0].shelf)
    }

    // --- deleteBook ---

    @Test
    fun deleteBook_removesFromDatabase() = runTest {
        val e = entity("/works/OL1W")
        dao.insertBook(e)
        dao.deleteBook(e)

        val books = dao.getAllBooks().first()
        assertEquals(emptyList<BookEntity>(), books)
    }

    @Test
    fun deleteBook_onlyRemovesMatchedBook() = runTest {
        val keep = entity("/works/OL1W", "Keep")
        val remove = entity("/works/OL2W", "Remove")
        dao.insertBook(keep)
        dao.insertBook(remove)
        dao.deleteBook(remove)

        val books = dao.getAllBooks().first()
        assertEquals(1, books.size)
        assertEquals("Keep", books[0].title)
    }

    // --- getBookByKey ---

    @Test
    fun getBookByKey_returnsEntityWhenFound() = runTest {
        val e = entity("/works/OL1W", "Dune")
        dao.insertBook(e)

        val result = dao.getBookByKey("/works/OL1W")
        assertEquals(e, result)
    }

    @Test
    fun getBookByKey_returnsNullWhenNotFound() = runTest {
        val result = dao.getBookByKey("/works/missing")
        assertNull(result)
    }

    // --- updateShelf ---

    @Test
    fun updateShelf_changesShelfValue() = runTest {
        dao.insertBook(entity("/works/OL1W", shelf = "READ"))
        dao.updateShelf("/works/OL1W", "CURRENTLY_READING")

        val result = dao.getBookByKey("/works/OL1W")
        assertEquals("CURRENTLY_READING", result?.shelf)
    }

    @Test
    fun updateShelf_doesNotAffectOtherBooks() = runTest {
        dao.insertBook(entity("/works/OL1W", shelf = "READ"))
        dao.insertBook(entity("/works/OL2W", shelf = "READ"))
        dao.updateShelf("/works/OL1W", "WANT_TO_READ")

        val other = dao.getBookByKey("/works/OL2W")
        assertEquals("READ", other?.shelf)
    }

    // --- updateDescription ---

    @Test
    fun updateDescription_setsDescription() = runTest {
        dao.insertBook(entity("/works/OL1W", description = null))
        dao.updateDescription("/works/OL1W", "Updated description.")

        val result = dao.getBookByKey("/works/OL1W")
        assertEquals("Updated description.", result?.description)
    }

    // --- updateCurrentPage ---

    @Test
    fun updateCurrentPage_setsPage() = runTest {
        dao.insertBook(entity("/works/OL1W", currentPage = 0))
        dao.updateCurrentPage("/works/OL1W", 75)

        val result = dao.getBookByKey("/works/OL1W")
        assertEquals(75, result?.currentPage)
    }

    @Test
    fun updateCurrentPage_doesNotAffectOtherFields() = runTest {
        val original = entity("/works/OL1W", "Dune", "Frank Herbert", 42, "Desc", "READ", 0)
        dao.insertBook(original)
        dao.updateCurrentPage("/works/OL1W", 100)

        val result = dao.getBookByKey("/works/OL1W")
        assertEquals("Dune", result?.title)
        assertEquals("Frank Herbert", result?.author)
        assertEquals(42, result?.coverId)
        assertEquals("READ", result?.shelf)
        assertEquals(100, result?.currentPage)
    }
}
