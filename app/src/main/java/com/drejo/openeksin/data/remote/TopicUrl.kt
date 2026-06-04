package com.drejo.openeksin.data.remote

import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Builds topic entry-list URLs with eksi query parameters (find, şükela, …). */
object TopicUrl {

    /** Canonical topic path without query string (relative or absolute). */
    fun clean(path: String): String = path.substringBefore("?")

    fun withParams(path: String, params: Map<String, String>): String {
        val base = clean(path)
        val query = params.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }
        return if (query.isBlank()) base else "$base?$query"
    }

    fun sukelaAll(path: String): String = withParams(path, mapOf("a" to "nice"))

    fun sukelaToday(path: String): String = withParams(path, mapOf("a" to "dailynice"))

    fun findKeywords(path: String, keywords: String): String =
        withParams(path, mapOf("a" to "find", "keywords" to keywords))

    fun findAuthor(path: String, author: String): String =
        withParams(path, mapOf("a" to "search", "author" to author))

    /** Resolves user input: `@nick` → author search, otherwise keyword find. */
    fun findQuery(path: String, raw: String): String {
        val q = raw.trim()
        if (q.isBlank()) return clean(path)
        return if (q.startsWith("@")) findAuthor(path, q.removePrefix("@").trim()) else findKeywords(path, q)
    }

    fun todayInTopic(path: String): String {
        val day = SimpleDateFormat("MM/dd/yyyy 00:00:00", Locale.US).format(Date())
        return withParams(path, mapOf("day" to day))
    }

    fun niceInTopic(path: String): String = sukelaAll(path)

    fun linksInTopic(path: String): String = findKeywords(path, "http://")

    fun buddyEntriesInTopic(path: String): String = withParams(path, mapOf("a" to "buddy"))
}
