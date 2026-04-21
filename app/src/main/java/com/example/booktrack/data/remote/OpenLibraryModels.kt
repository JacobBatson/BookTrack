package com.example.booktrack.data.remote

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class SearchResponse(
    @SerializedName("docs") val docs: List<SearchDoc>
)

data class SearchDoc(
    @SerializedName("key") val key: String,
    @SerializedName("title") val title: String,
    @SerializedName("author_name") val authorName: List<String>?,
    @SerializedName("cover_i") val coverId: Int?,
    @SerializedName("first_sentence") val firstSentence: List<String>?
)

data class WorkDetail(
    @SerializedName("description") val description: JsonElement?
)

fun WorkDetail.extractDescription(): String? =
    when {
        description == null -> null
        description.isJsonPrimitive -> description.asString
        description.isJsonObject -> description.asJsonObject.get("value")?.asString
        else -> null
    }
