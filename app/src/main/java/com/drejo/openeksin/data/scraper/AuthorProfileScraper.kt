package com.drejo.openeksin.data.scraper

import org.jsoup.Jsoup

/** Parses static parts of a `/biri/{nick}` profile page. */
object AuthorProfileScraper {

    /** Featured entry shown in the profile intro (org.eksin "Künye" tab). */
    data class Highlight(
        val title: String,
        val entryLink: String,
    )

    fun parseHighlight(html: String): Highlight? {
        val intro = Jsoup.parse(html).selectFirst("#profile-intro") ?: return null
        val link = intro.selectFirst("footer a")?.attr("href")?.trim().orEmpty()
        val title = intro.selectFirst("h2")?.text()?.trim().orEmpty()
        if (link.isEmpty() || !link.startsWith("/entry/")) return null
        return Highlight(title = title.ifEmpty { "künye" }, entryLink = link)
    }
}
