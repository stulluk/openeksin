package com.drejo.openeksin.data.model

/** A single title in a topic-index list (today / agenda / popular). */
data class Topic(
    val title: String,
    val link: String,
    val entryCount: String,
)
