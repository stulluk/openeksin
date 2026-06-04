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

    /** A modern, consistent Chrome-on-Android UA so Cloudflare does not flag us. */
    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/131.0.0.0 Mobile Safari/537.36"
}
