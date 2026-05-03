package com.example.booktrack.ui

import android.app.Application
import android.content.ContentValues
import android.provider.CalendarContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.booktrack.data.BookRepository
import com.example.booktrack.data.local.BookDatabase
import com.example.booktrack.data.local.ReadingSessionEntity
import com.example.booktrack.data.remote.RetrofitInstance
import com.example.booktrack.model.Book
import com.example.booktrack.model.Shelf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

data class ActiveSession(
    val book: Book,
    val startPage: Int,
    val startTimeMs: Long,
    val elapsedSeconds: Long = 0
)

data class CompletedSession(
    val bookTitle: String,
    val startPage: Int,
    val endPage: Int,
    val durationMs: Long,
    val startTimeMs: Long,
    val endTimeMs: Long
)

data class BookUiState(
    val searchResults: List<Book> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val library: Map<Shelf, List<Book>> = emptyMap(),
    val selectedBook: Book? = null,
    val activeSession: ActiveSession? = null,
    val completedSession: CompletedSession? = null,
    val sessionsForSelectedBook: List<ReadingSessionEntity> = emptyList(),
    val allSessions: List<ReadingSessionEntity> = emptyList(),
    val readingStreak: Int = 0,
)

class BookViewModel(
    application: Application,
    private val repository: BookRepository = BookRepository(
        dao = BookDatabase.getDatabase(application).bookDao(),
        sessionDao = BookDatabase.getDatabase(application).readingSessionDao(),
        api = RetrofitInstance.api
    )
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(BookUiState())
    val uiState: StateFlow<BookUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var sessionsJob: Job? = null

    init {
        viewModelScope.launch {
            repository.library.collect { lib ->
                _uiState.update { it.copy(library = lib) }
            }
        }
        viewModelScope.launch {
            repository.getAllSessions().collect { sessions ->
                _uiState.update { state ->
                    state.copy(
                        allSessions = sessions,
                        readingStreak = computeStreak(sessions)
                    )
                }
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchError = null) }
            try {
                val results = repository.search(query)
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSearching = false, searchError = "Search failed. Check your connection.")
                }
            }
        }
    }

    fun selectBook(book: Book) {
        viewModelScope.launch {
            val inLibrary = repository.getBookByKey(book.key)
            _uiState.update { it.copy(selectedBook = inLibrary ?: book) }
        }
        sessionsJob?.cancel()
        sessionsJob = viewModelScope.launch {
            repository.getSessionsForBook(book.key).collect { sessions ->
                _uiState.update { it.copy(sessionsForSelectedBook = sessions) }
            }
        }
    }

    fun addToShelf(book: Book, shelf: Shelf) {
        viewModelScope.launch {
            repository.addToShelf(book, shelf)
            val updated = repository.getBookByKey(book.key)
            _uiState.update { it.copy(selectedBook = updated ?: book.copy(shelf = shelf)) }
        }
    }

    fun moveToShelf(key: String, shelf: Shelf) {
        viewModelScope.launch {
            repository.moveToShelf(key, shelf)
            val updated = repository.getBookByKey(key)
            _uiState.update { state ->
                state.copy(selectedBook = updated ?: state.selectedBook?.copy(shelf = shelf))
            }
        }
    }

    fun removeFromLibrary(key: String) {
        viewModelScope.launch {
            repository.removeFromLibrary(key)
            _uiState.update { state ->
                state.copy(selectedBook = state.selectedBook?.copy(shelf = null))
            }
        }
    }

    fun fetchDescription(book: Book) {
        if (!book.description.isNullOrBlank()) return
        viewModelScope.launch {
            val description = repository.fetchDescription(book) ?: return@launch
            _uiState.update { state ->
                val updated = state.selectedBook?.takeIf { it.key == book.key }
                    ?.copy(description = description)
                state.copy(selectedBook = updated ?: state.selectedBook)
            }
        }
    }

    fun clearSearchResults() {
        _uiState.update { it.copy(searchResults = emptyList(), searchError = null) }
    }

    fun getShelfForBook(key: String): Shelf? =
        _uiState.value.library.entries
            .firstOrNull { (_, books) -> books.any { it.key == key } }
            ?.key

    fun startReadingSession(book: Book, startPage: Int) {
        val startTimeMs = System.currentTimeMillis()
        _uiState.update {
            it.copy(activeSession = ActiveSession(book, startPage, startTimeMs))
        }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { state ->
                    val session = state.activeSession ?: return@update state
                    val elapsed = (System.currentTimeMillis() - session.startTimeMs) / 1000
                    state.copy(activeSession = session.copy(elapsedSeconds = elapsed))
                }
            }
        }
    }

    fun endReadingSession(endPage: Int) {
        timerJob?.cancel()
        val session = _uiState.value.activeSession ?: return
        val endTimeMs = System.currentTimeMillis()
        val durationMs = endTimeMs - session.startTimeMs
        viewModelScope.launch {
            repository.insertSession(
                bookKey = session.book.key,
                bookTitle = session.book.title,
                startPage = session.startPage,
                endPage = endPage,
                startTimeMs = session.startTimeMs,
                endTimeMs = endTimeMs,
                durationMs = durationMs
            )
            repository.updateCurrentPage(session.book.key, endPage)
            val updated = repository.getBookByKey(session.book.key)
            _uiState.update {
                it.copy(
                    activeSession = null,
                    selectedBook = updated ?: it.selectedBook,
                    completedSession = CompletedSession(
                        bookTitle = session.book.title,
                        startPage = session.startPage,
                        endPage = endPage,
                        durationMs = durationMs,
                        startTimeMs = session.startTimeMs,
                        endTimeMs = endTimeMs
                    )
                )
            }
        }
    }

    fun clearCompletedSession() {
        _uiState.update { it.copy(completedSession = null) }
    }

    fun insertCalendarEvent(session: CompletedSession) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val calendarId = getDefaultCalendarId() ?: return@launch
                val pagesRead = (session.endPage - session.startPage).coerceAtLeast(0)
                val values = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.TITLE, "Reading: ${session.bookTitle}")
                    put(CalendarContract.Events.DESCRIPTION,
                        "Pages ${session.startPage} → ${session.endPage} ($pagesRead pages read)")
                    put(CalendarContract.Events.DTSTART, session.startTimeMs)
                    put(CalendarContract.Events.DTEND, session.endTimeMs)
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                }
                app.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            } catch (_: Exception) {}
        }
    }

    private fun getDefaultCalendarId(): Long? {
        val app = getApplication<Application>()
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val selection = "${CalendarContract.Calendars.VISIBLE} = 1"
        return app.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection, selection, null,
            "${CalendarContract.Calendars.IS_PRIMARY} DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
    }

    private fun computeStreak(sessions: List<ReadingSessionEntity>): Int {
        if (sessions.isEmpty()) return 0
        val cal = Calendar.getInstance()
        fun normalizeDay(ms: Long): Long {
            cal.timeInMillis = ms
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
        val today = normalizeDay(System.currentTimeMillis())
        val oneDayMs = 24 * 60 * 60 * 1000L
        val readDays = sessions.map { normalizeDay(it.startTimeMs) }.toHashSet()
        var checkDay = if (today in readDays) today else today - oneDayMs
        var streak = 0
        while (checkDay in readDays) {
            streak++
            checkDay -= oneDayMs
        }
        return streak
    }
}
