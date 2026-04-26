package com.example.booktrack.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.booktrack.data.BookRepository
import com.example.booktrack.data.local.BookDatabase
import com.example.booktrack.data.remote.RetrofitInstance
import com.example.booktrack.model.Book
import com.example.booktrack.model.Shelf
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val durationMs: Long
)

data class BookUiState(
    val searchResults: List<Book> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val library: Map<Shelf, List<Book>> = emptyMap(),
    val selectedBook: Book? = null,
    val activeSession: ActiveSession? = null,
    val completedSession: CompletedSession? = null
)

class BookViewModel(application: Application) : AndroidViewModel(application) {

    private val db = BookDatabase.getDatabase(application)
    private val repository = BookRepository(
        dao = db.bookDao(),
        sessionDao = db.readingSessionDao(),
        api = RetrofitInstance.api
    )

    private val _uiState = MutableStateFlow(BookUiState())
    val uiState: StateFlow<BookUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.library.collect { lib ->
                _uiState.update { it.copy(library = lib) }
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
            _uiState.update {
                it.copy(
                    activeSession = null,
                    completedSession = CompletedSession(
                        bookTitle = session.book.title,
                        startPage = session.startPage,
                        endPage = endPage,
                        durationMs = durationMs
                    )
                )
            }
        }
    }

    fun clearCompletedSession() {
        _uiState.update { it.copy(completedSession = null) }
    }
}