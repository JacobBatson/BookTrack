package com.example.booktrack.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.booktrack.data.local.BookDatabase
import com.example.booktrack.model.Shelf
import com.example.booktrack.ui.screens.SessionSummaryScreen
import com.example.booktrack.ui.theme.BookTrackTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionSummaryScreenTest {

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

    private fun setupCompletedSession() {
        runBlocking {
            db.bookDao().insertBook(testBookEntity(shelf = Shelf.CURRENTLY_READING))
        }
        viewModel.selectBook(testBook(shelf = Shelf.CURRENTLY_READING))
        rule.waitUntil(3000) { viewModel.uiState.value.selectedBook != null }
        viewModel.startReadingSession(testBook(shelf = Shelf.CURRENTLY_READING), startPage = 10)
        viewModel.endReadingSession(endPage = 50)
        rule.waitUntil(3000) { viewModel.uiState.value.completedSession != null }
    }

    @Test
    fun showsBookTitle() {
        setupCompletedSession()
        rule.setContent {
            BookTrackTheme { SessionSummaryScreen(viewModel = viewModel, onDone = {}) }
        }
        rule.onNodeWithTag("summary_book_title").assertIsDisplayed()
        rule.onNodeWithText("Dune").assertIsDisplayed()
    }

    @Test
    fun showsStatsCard() {
        setupCompletedSession()
        rule.setContent {
            BookTrackTheme { SessionSummaryScreen(viewModel = viewModel, onDone = {}) }
        }
        rule.onNodeWithTag("summary_stats_card").assertIsDisplayed()
        rule.onNodeWithText("40 pages").assertIsDisplayed()
    }

    @Test
    fun doneButton_callsCallbackAndClearsSession() {
        setupCompletedSession()
        var doneCalled = false
        rule.setContent {
            BookTrackTheme { SessionSummaryScreen(viewModel = viewModel, onDone = { doneCalled = true }) }
        }
        rule.onNodeWithTag("summary_done_button").performClick()
        rule.waitUntil(2000) { doneCalled }
        assertNull(viewModel.uiState.value.completedSession)
    }
}
