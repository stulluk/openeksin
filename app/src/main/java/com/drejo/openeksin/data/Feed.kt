package com.drejo.openeksin.data

import com.drejo.openeksin.data.remote.Endpoints

/** A selectable topic feed (built-in index or a channel). */
data class Feed(
    val title: String,
    val path: String,
)

/** Built-in feeds shown before the dynamic channel list. */
object Feeds {
    val BUILTIN = listOf(
        Feed("gündem", Endpoints.AGENDA),
        Feed("debe", Endpoints.DEBE),
        Feed("bugün", Endpoints.TODAY),
        Feed("tarihte bugün", "${Endpoints.BASE}/basliklar/tarihte-bugun"),
    )
}
