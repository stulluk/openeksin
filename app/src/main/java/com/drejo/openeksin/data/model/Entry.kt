package com.drejo.openeksin.data.model

/** A single entry inside a topic. */
data class Entry(
    val id: String,
    val author: String,
    val date: String,
    val content: String,
    val favoriteCount: String,
)

/** A topic page: its title plus the entries on the current page. */
data class TopicDetail(
    val title: String,
    val entries: List<Entry>,
)
