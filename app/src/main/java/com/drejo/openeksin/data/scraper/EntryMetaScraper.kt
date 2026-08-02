package com.drejo.openeksin.data.scraper

import org.jsoup.Jsoup

/** Extracts lightweight metadata from entry HTML partials. */
object EntryMetaScraper {

    fun favoriteCount(html: String): String =
        Jsoup.parse(html)
            .selectFirst("li#entry-item[data-favorite-count]")
            ?.attr("data-favorite-count")
            ?.trim()
            .orEmpty()
}
