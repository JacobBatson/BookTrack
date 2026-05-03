package com.example.booktrack.ui

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.booktrack.data.BookRepository
import com.example.booktrack.data.local.BookDatabase
import com.example.booktrack.data.remote.OpenLibraryApi
import io.mockk.mockk

object BookViewModelTestFactory {
    fun build(
        api: OpenLibraryApi = mockk(relaxed = true)
    ): Pair<BookViewModel, BookDatabase> {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(app, BookDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = BookRepository(
            dao = db.bookDao(),
            sessionDao = db.readingSessionDao(),
            api = api
        )
        return BookViewModel(app, repo) to db
    }
}
