package com.drejo.openeksin.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** A locally archived entry (the "kaydet" action / "arşiv" list). */
data class SavedEntry(
    val id: String,
    val author: String,
    val date: String,
    val content: String,
    val topicTitle: String,
)

/** Persists archived entries in SharedPreferences as a JSON array. */
object SavedStore {

    private const val PREFS = "openeksin_saved"
    private const val KEY = "entries"

    private lateinit var prefs: SharedPreferences
    private val _entries = MutableStateFlow<List<SavedEntry>>(emptyList())
    val entries: StateFlow<List<SavedEntry>> = _entries.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _entries.value = load()
    }

    fun isSaved(id: String): Boolean = _entries.value.any { it.id == id }

    /** Adds or removes the entry; returns true if it is saved afterwards. */
    fun toggle(entry: SavedEntry): Boolean {
        val current = _entries.value
        val saved = current.any { it.id == entry.id }
        _entries.value = if (saved) current.filterNot { it.id == entry.id } else listOf(entry) + current
        persist()
        return !saved
    }

    private fun load(): List<SavedEntry> = runCatching {
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            SavedEntry(
                id = o.getString("id"),
                author = o.optString("author"),
                date = o.optString("date"),
                content = o.optString("content"),
                topicTitle = o.optString("topicTitle"),
            )
        }
    }.getOrDefault(emptyList())

    private fun persist() {
        val arr = JSONArray()
        _entries.value.forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("author", e.author)
                    .put("date", e.date)
                    .put("content", e.content)
                    .put("topicTitle", e.topicTitle),
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }
}
