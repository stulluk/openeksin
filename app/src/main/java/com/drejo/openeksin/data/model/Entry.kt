package com.drejo.openeksin.data.model

/** A run of entry content: plain text, or a link with an href. */
data class ContentSegment(
    val text: String,
    val href: String? = null,
)

/** A single entry inside a topic. */
data class Entry(
    val id: String,
    val author: String,
    val date: String,
    val content: String,
    val segments: List<ContentSegment>,
    val favoriteCount: String,
)

/** A topic page: title, entries and pagination state. */
data class TopicDetail(
    val title: String,
    val titlePath: String,
    val entries: List<Entry>,
    val currentPage: Int,
    val pageCount: Int,
)
