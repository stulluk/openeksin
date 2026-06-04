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
    val authorId: String,
    val date: String,
    val content: String,
    val segments: List<ContentSegment>,
    val favoriteCount: String,
    val isFavorite: Boolean,
    /** Space-separated action flags from data-flags (edit, deleteself, vote, msg). */
    val flags: String = "",
) {
    val canEdit: Boolean get() = "edit" in flags.split(' ')
    val canDelete: Boolean get() = "deleteself" in flags.split(' ')
}

/** Fields scraped from the entry edit form at /entry/duzelt/{id}. */
data class EntryEditForm(
    val token: String,
    val title: String,
    val inputStartTime: String,
    val content: String,
)

/** A topic page: title, entries and pagination state. */
data class TopicDetail(
    val title: String,
    val titlePath: String,
    val entries: List<Entry>,
    val currentPage: Int,
    val pageCount: Int,
    val topicId: String = "",
    val isTracked: Boolean = false,
    /** Relative href for "tümünü göster", e.g. /baslik--123?focusto=456 */
    val showAllUrl: String = "",
    /** Saved draft text from the compose form, if any. */
    val draft: String = "",
)

/** Hidden fields scraped from the entry compose form on a topic page. */
data class EntryComposeForm(
    val token: String,
    val title: String,
    val topicId: String,
    val inputStartTime: String,
)
