package com.example.booktrack.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenLibraryApi {
    @GET("search.json")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20,
        @Query("fields") fields: String = "key,title,author_name,cover_i,first_sentence"
    ): SearchResponse

    @GET("{key}.json")
    suspend fun getWork(
        @Path("key", encoded = true) key: String
    ): WorkDetail
}
