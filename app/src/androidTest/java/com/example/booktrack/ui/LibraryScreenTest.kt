package com.example.booktrack.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.booktrack.data.local.BookDatabase
import com.example.booktrack.model.Shelf
import com.example.booktrack.ui.screens.LibraryScreen
import com.example.booktrack.ui.theme.BookTrackTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var db: BookDatabase
    private lateinit var viewModel: BookViewModel

    @Before
    fun setUp() {
        val (vm, database) = BookViewModelTestFactory.build()
        viewModel = vm
        db = database
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun emptyLibrary_showsEmptyState() {
        rule.setContent {
            BookTrackTheme { LibraryScreen(viewModel = viewModel, onBookClick = {}) }
        }
        rule.onNodeWithTag("library_empty_state").assertIsDisplayed()
        rule.onNodeWithText("Your library is empty").assertIsDisplayed()
    }

    @Test
    fun withBooks_showsBookCard() {
        runBlocking { db.bookDao().insertBook(testBookEntity(shelf = Shelf.READ)) }
        rule.setContent {
            BookTrackTheme { LibraryScreen(viewModel = viewModel, onBookClick = {}) }
        }
        rule.waitUntil(3000) { viewModel.uiState.value.library.isNotEmpty() }
        rule.onNodeWithTag("book_card_/works/OL1W").assertIsDisplayed()
    }

    @Test
    fun withBooks_showsShelfHeaders() {
        runBlocking {
            db.bookDao().insertBook(testBookEntity(key = "/works/OL1W", shelf = Shelf.CURRENTLY_READING))
            db.bookDao().insertBook(testBookEntity(key = "/works/OL2W", shelf = Shelf.READ))
        }
        rule.setContent {
            BookTrackTheme { LibraryScreen(viewModel = viewModel, onBookClick = {}) }
        }
        rule.waitUntil(3000) { viewModel.uiState.value.library.size >= 2 }
        rule.onNodeWithTag("shelf_header_CURRENTLY_READING").assertIsDisplayed()
        rule.onNodeWithTag("shelf_header_READ").assertIsDisplayed()
    }

    @Test
    fun withSessions_showsStreakBanner() {
        runBlocking {
            db.bookDao().insertBook(testBookEntity())
            db.readingSessionDao().insertSession(testSession(startTimeMs = System.currentTimeMillis()))
        }
        rule.setContent {
            BookTrackTheme { LibraryScreen(viewModel = viewModel, onBookClick = {}) }
        }
        rule.waitUntil(3000) { viewModel.uiState.value.readingStreak > 0 }
        rule.onNodeWithTag("streak_banner").assertIsDisplayed()
    }

    @Test
    fun withNoSessions_hidesStreakBanner() {
        runBlocking { db.bookDao().insertBook(testBookEntity()) }
        rule.setContent {
            BookTrackTheme { LibraryScreen(viewModel = viewModel, onBookClick = {}) }
        }
        rule.waitUntil(3000) { viewModel.uiState.value.library.isNotEmpty() }
        rule.onNodeWithTag("streak_banner").assertDoesNotExist()
    }
}
