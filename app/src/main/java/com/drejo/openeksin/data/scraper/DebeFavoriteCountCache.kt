package com.drejo.openeksin.data.scraper

import java.util.concurrent.ConcurrentHashMap

/** Process-wide cache for debe entry favorite counts (survives tab/pager recreation). */
object DebeFavoriteCountCache {
    private val counts = ConcurrentHashMap<String, String>()

    fun get(entryId: String): String? = counts[entryId]

    fun put(entryId: String, count: String) {
        if (count.isNotEmpty()) counts[entryId] = count
    }

    fun apply(entryId: String, count: String): String =
        if (count.isNotEmpty()) {
            counts[entryId] = count
            count
        } else {
            counts[entryId].orEmpty()
        }
}
