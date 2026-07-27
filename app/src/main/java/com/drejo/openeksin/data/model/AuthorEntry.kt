package com.drejo.openeksin.data.model

import androidx.compose.runtime.Immutable

/** One entry shown on a user's profile feed, with its topic context. */
@Immutable
data class AuthorEntry(
    val topicTitle: String,
    val topicLink: String,
    val entry: Entry,
)
