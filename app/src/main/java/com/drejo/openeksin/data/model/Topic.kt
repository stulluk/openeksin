package com.drejo.openeksin.data.model

import androidx.compose.runtime.Immutable

/** A single title in a topic-index list (today / agenda / popular). */
@Immutable
data class Topic(
    val title: String,
    val link: String,
    val entryCount: String,
)
