package com.example.booktrack.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.booktrack.data.local.BookDatabase
import com.example.booktrack.ui.screens.CalendarScreen
import com.example.booktrack.ui.theme.BookTrackTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class CalendarScreenTest {

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
    fun showsCurrentMonthLabel() {
        rule.setContent {
            BookTrackTheme { CalendarScreen(viewModel = viewModel) }
        }
        val expected = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
        rule.onNodeWithTag("calendar_month_label").assertIsDisplayed()
        rule.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun prevMonth_updatesLabel() {
        rule.setContent {
            BookTrackTheme { CalendarScreen(viewModel = viewModel) }
        }
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val expectedPrev = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
        rule.onNodeWithTag("calendar_prev_month").performClick()
        rule.onNodeWithText(expectedPrev).assertIsDisplayed()
    }

    @Test
    fun nextMonth_updatesLabel() {
        rule.setContent {
            BookTrackTheme { CalendarScreen(viewModel = viewModel) }
        }
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
        val expectedNext = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
        rule.onNodeWithTag("calendar_next_month").performClick()
        rule.onNodeWithText(expectedNext).assertIsDisplayed()
    }

    @Test
    fun dayWithSession_opensBottomSheet() {
        val todayMs = System.currentTimeMillis()
        runBlocking {
            db.readingSessionDao().insertSession(testSession(startTimeMs = todayMs))
        }
        rule.setContent {
            BookTrackTheme { CalendarScreen(viewModel = viewModel) }
        }
        rule.waitUntil(3000) { viewModel.uiState.value.allSessions.isNotEmpty() }

        val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        rule.onNodeWithTag("day_cell_$today").performClick()
        rule.onNodeWithText("Dune").assertIsDisplayed()
    }
}
