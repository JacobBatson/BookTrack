package com.example.booktrack.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.booktrack.data.local.BookDatabase
import com.example.booktrack.data.remote.OpenLibraryApi
import com.example.booktrack.data.remote.SearchDoc
import com.example.booktrack.data.remote.SearchResponse
import com.example.booktrack.ui.screens.SearchScreen
import com.example.booktrack.ui.theme.BookTrackTheme
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class SearchScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var db: BookDatabase
    private lateinit var viewModel: BookViewModel
    private lateinit var mockApi: OpenLibraryApi

    @Before
    fun setUp() {
        mockApi = mockk(relaxed = true)
        val (vm, database) = BookViewModelTestFactory.build(mockApi)
        viewModel = vm
        db = database
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun initialState_showsSearchField() {
        rule.setContent {
            BookTrackTheme { SearchScreen(viewModel = viewModel, onBookClick = {}) }
        }
        rule.onNodeWithTag("search_field").assertIsDisplayed()
    }

    @Test
    fun whileSearching_showsLoadingIndicator() {
        coEvery { mockApi.search(any(), any(), any()) } coAnswers {
            suspendCancellableCoroutine { }
        }
        rule.setContent {
            BookTrackTheme { SearchScreen(viewModel = viewModel, onBookClick = {}) }
        }
        viewModel.search("kotlin")
        rule.waitUntil(3000) { viewModel.uiState.value.isSearching }
        rule.onNodeWithTag("search_loading").assertIsDisplayed()
    }

    @Test
    fun onSearchError_showsErrorMessage() {
        coEvery { mockApi.search(any(), any(), any()) } throws IOException("no network")
        rule.setContent {
            BookTrackTheme { SearchScreen(viewModel = viewModel, onBookClick = {}) }
        }
        viewModel.search("kotlin")
        rule.waitUntil(3000) { viewModel.uiState.value.searchError != null }
        rule.onNodeWithTag("search_error").assertIsDisplayed()
        rule.onNodeWithText("Search failed. Check your connection.").assertIsDisplayed()
    }

    @Test
    fun onSearchSuccess_showsResultCards() {
        coEvery { mockApi.search(any(), any(), any()) } returns SearchResponse(
            docs = listOf(
                SearchDoc(key = "/works/OL1W", title = "Dune", authorName = listOf("Frank Herbert"), coverId = null, firstSentence = null)
            )
        )
        rule.setContent {
            BookTrackTheme { SearchScreen(viewModel = viewModel, onBookClick = {}) }
        }
        viewModel.search("dune")
        rule.waitUntil(3000) { viewModel.uiState.value.searchResults.isNotEmpty() }
        rule.onNodeWithTag("search_book_card_/works/OL1W").assertIsDisplayed()
        rule.onNodeWithText("Dune").assertIsDisplayed()
    }

    @Test
    fun onEmptyResults_showsNoResultsText() {
        rule.setContent {
            BookTrackTheme { SearchScreen(viewModel = viewModel, onBookClick = {}) }
        }
        rule.onNodeWithTag("search_field").performTextInput("kotlin")
        rule.onNodeWithTag("search_no_results").assertIsDisplayed()
    }
}
