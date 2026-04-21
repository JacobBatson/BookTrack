package com.example.booktrack.model

data class Book(
    val key: String,
    val title: String,
    val author: String,
    val coverId: Int?,
    val description: String?,
    val shelf: Shelf? = null
)

fun Book.coverUrl(size: String = "M"): String? =
    coverId?.let { "https://covers.openlibrary.org/b/id/$it-$size.jpg" }
