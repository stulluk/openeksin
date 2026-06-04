package com.drejo.openeksin.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Author nicks the current user follows / blocks. */
data class RelationSets(
    val buddies: Set<String> = emptySet(),
    val blocked: Set<String> = emptySet(),
    val blockedTitles: Set<String> = emptySet(),
)

/**
 * Tracks the logged-in user's relations performed inside the app, persisted
 * locally. Eksi loads the authoritative list via a JS-only AJAX call we can't
 * reliably scrape, so we keep an optimistic local cache instead. This keeps the
 * follow/block menu labels (takip et ↔ takibi bırak) correct and reversible.
 */
object RelationStore {

    private const val PREFS = "openeksin_relations"

    private lateinit var prefs: SharedPreferences
    private val _state = MutableStateFlow(RelationSets())
    val state: StateFlow<RelationSets> = _state.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _state.value = RelationSets(
            buddies = prefs.getStringSet("b", emptySet())!!.toSet(),
            blocked = prefs.getStringSet("m", emptySet())!!.toSet(),
            blockedTitles = prefs.getStringSet("i", emptySet())!!.toSet(),
        )
    }

    fun clear() {
        _state.value = RelationSets()
        if (::prefs.isInitialized) prefs.edit().clear().apply()
    }

    fun update(nick: String, code: String, add: Boolean) {
        val s = _state.value
        fun Set<String>.toggle() = if (add) this + nick else this - nick
        _state.value = when (code) {
            "b" -> s.copy(buddies = s.buddies.toggle())
            "m" -> s.copy(blocked = s.blocked.toggle())
            "i" -> s.copy(blockedTitles = s.blockedTitles.toggle())
            else -> s
        }
        persist()
    }

    private fun persist() {
        if (!::prefs.isInitialized) return
        val s = _state.value
        prefs.edit()
            .putStringSet("b", s.buddies)
            .putStringSet("m", s.blocked)
            .putStringSet("i", s.blockedTitles)
            .apply()
    }
}
