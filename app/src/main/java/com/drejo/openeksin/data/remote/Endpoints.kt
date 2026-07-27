package com.drejo.openeksin.data.remote

/**
 * Public endpoints of eksisozluk.com. These are facts about the website (URL
 * paths), used to scrape its public HTML.
 */
object Endpoints {
    const val HOST = "eksisozluk.com"
    const val BASE = "https://$HOST"

    // "bugün" feed. The web /basliklar/bugun is 404 for anonymous users, but the
    // app feed endpoint /index/feedrefresh returns today's topics without login.
    const val TODAY = "$BASE/index/feedrefresh"
    const val AGENDA = "$BASE/basliklar/gundem"
    const val DEBE = "$BASE/debe"

    const val LOGIN = "$BASE/giris?returnurl=%2f"
    const val LOGOUT = "$BASE/cikis"
    const val CHANNELS = "$BASE/kanallar"
    const val FAVORITE = "$BASE/entry/favla"
    const val UNFAVORITE = "$BASE/entry/favlama"
    const val VOTE = "$BASE/entry/vote"
    const val MESSAGES = "$BASE/mesaj"
    const val EVENTS = "$BASE/basliklar/olay"
    const val MESSAGE_SEND = "$BASE/mesaj/yolla"
    const val MESSAGE_SEND_AJAX = "$BASE/mesaj/sendajax"
    const val RELATION_ADD = "$BASE/userrelation/addrelation/"
    const val RELATION_REMOVE = "$BASE/userrelation/removerelation/"
    const val BUDDY_INFO = "$BASE/takip-engellenmis"
    const val ENTRY = "$BASE/entry/"
    const val AUTOCOMPLETE = "$BASE/autocomplete/query"

    fun authorProfile(nick: String): String =
        "$BASE/biri/${nick.trim().replace(' ', '-')}"

    fun authorEntriesPath(nick: String): String =
        "/son-entryleri?nick=${java.net.URLEncoder.encode(nick.trim(), "UTF-8")}"

    fun authorTopFavoritedPath(nick: String): String =
        "/en-cok-favorilenen-entryleri?nick=${java.net.URLEncoder.encode(nick.trim(), "UTF-8")}"

    /** Builds a paginated author feed URL from a relative path (with or without query). */
    fun authorFeedPage(relativePath: String, page: Int): String {
        val base = if (relativePath.startsWith("http")) relativePath else BASE + relativePath
        if (page <= 1) return base
        return if (base.contains("?")) "$base&p=$page" else "$base?p=$page"
    }

    @Deprecated("Use authorFeedPage(authorEntriesPath(nick), page)")
    fun authorEntries(nick: String, page: Int): String =
        authorFeedPage(authorEntriesPath(nick), page)
    const val ENTRY_ADD = "$BASE/entry/ekle"
    const val ENTRY_DELETE = "$BASE/entry/sil"

    fun entryEdit(entryId: String) = "$BASE/entry/duzelt/$entryId"

    fun watchTopic(topicId: String) = "$BASE/baslik/takip-et/$topicId"
    fun unwatchTopic(topicId: String) = "$BASE/takip-etme/$topicId"

    // user-relation type codes (see decompiled ResponseProcessor)
    const val REL_FOLLOW = "b"
    const val REL_BLOCK = "m"
    const val REL_BLOCK_TITLE = "i"

    /** A modern, consistent Chrome-on-Android UA so Cloudflare does not flag us. */
    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/131.0.0.0 Mobile Safari/537.36"
}
