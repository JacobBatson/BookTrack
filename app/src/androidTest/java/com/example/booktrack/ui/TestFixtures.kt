package com.example.booktrack.ui

import com.example.booktrack.data.local.BookEntity
import com.example.booktrack.data.local.ReadingSessionEntity
import com.example.booktrack.model.Book
import com.example.booktrack.model.Shelf

fun testBook(
    key: String = "/works/OL1W",
    title: String = "Dune",
    author: String = "Frank Herbert",
    shelf: Shelf? = null,
    currentPage: Int = 0
) = Book(key = key, title = title, author = author, coverId = null, description = null, shelf = shelf, currentPage = currentPage)

fun testBookEntity(
    key: String = "/works/OL1W",
    title: String = "Dune",
    author: String = "Frank Herbert",
    shelf: Shelf = Shelf.READ
) = BookEntity(key = key, title = title, author = author, coverId = null, description = null, shelf = shelf.name)

fun testSession(
    bookKey: String = "/works/OL1W",
    bookTitle: String = "Dune",
    startTimeMs: Long = System.currentTimeMillis(),
    durationMs: Long = 3_600_000L
) = ReadingSessionEntity(
    bookKey = bookKey,
    bookTitle = bookTitle,
    startPage = 10,
    endPage = 50,
    startTimeMs = startTimeMs,
    endTimeMs = startTimeMs + durationMs,
    durationMs = durationMs
)
