package com.example.booktrack.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.booktrack.data.local.BookDatabase
import com.example.booktrack.model.Shelf
import com.example.booktrack.ui.screens.DetailScreen
import com.example.booktrack.ui.theme.BookTrackTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailScreenTest {

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

    private fun insertAndSelectBook(shelf: Shelf = Shelf.READ) {
        runBlocking {
            db.bookDao().insertBook(testBookEntity(shelf = shelf))
        }
        viewModel.selectBook(testBook(shelf = shelf))
        rule.waitUntil(3000) { viewModel.uiState.value.selectedBook?.shelf == shelf }
    }

    @Test
    fun showsBookTitleAndAuthor() {
        insertAndSelectBook()
        rule.setContent {
            BookTrackTheme { DetailScreen(viewModel = viewModel, onBack = {}, onStartSession = {}) }
        }
        rule.onNodeWithTag("detail_book_title").assertIsDisplayed()
        rule.onNodeWithText("Dune").assertIsDisplayed()
        rule.onNodeWithTag("detail_book_author").assertIsDisplayed()
        rule.onNodeWithText("Frank Herbert").assertIsDisplayed()
    }

    @Test
    fun showsCorrectShelfChipSelected() {
        insertAndSelectBook(shelf = Shelf.CURRENTLY_READING)
        rule.setContent {
            BookTrackTheme { DetailScreen(viewModel = viewModel, onBack = {}, onStartSession = {}) }
        }
        rule.onNodeWithTag("shelf_chip_CURRENTLY_READING").assertIsSelected()
    }

    @Test
    fun deleteButton_opensConfirmDialog() {
        insertAndSelectBook()
        rule.setContent {
            BookTrackTheme { DetailScreen(viewModel = viewModel, onBack = {}, onStartSession = {}) }
        }
        rule.onNodeWithTag("detail_delete_button").performClick()
        rule.onNodeWithText("Remove book?").assertIsDisplayed()
        rule.onNodeWithTag("delete_confirm_button").assertIsDisplayed()
    }

    @Test
    fun confirmDelete_removesBook() {
        insertAndSelectBook()
        rule.setContent {
            BookTrackTheme { DetailScreen(viewModel = viewModel, onBack = {}, onStartSession = {}) }
        }
        rule.onNodeWithTag("detail_delete_button").performClick()
        rule.onNodeWithTag("delete_confirm_button").performClick()
        rule.waitUntil(3000) { viewModel.uiState.value.library.isEmpty() }
    }

    @Test
    fun startSessionButton_opensDialog() {
        insertAndSelectBook(shelf = Shelf.CURRENTLY_READING)
        rule.setContent {
            BookTrackTheme { DetailScreen(viewModel = viewModel, onBack = {}, onStartSession = {}) }
        }
        rule.onNodeWithTag("start_session_button").performClick()
        rule.onNodeWithText("Start Reading Session").assertIsDisplayed()
        rule.onNodeWithText("Starting page").assertIsDisplayed()
    }
}
