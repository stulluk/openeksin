package com.drejo.openeksin.data.model

/** One conversation row in the message inbox. */
data class MessageThread(
    val threadId: String,
    val link: String,
    val nick: String,
    val unreadCount: String,
    val preview: String,
    val date: String,
)

/** A single message inside a conversation thread. */
data class Message(
    val incoming: Boolean,
    val text: String,
    val date: String,
)
