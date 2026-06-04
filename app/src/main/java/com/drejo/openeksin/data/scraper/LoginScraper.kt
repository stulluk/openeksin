package com.drejo.openeksin.data.scraper

import org.jsoup.Jsoup

/**
 * Detects the logged-in user's nick from a full eksisozluk page. When logged
 * in, the top navigation contains a link to the user's own profile
 * (`/biri/<nick>`); its `title` attribute is the nick.
 */
object LoginScraper {

    fun parseNick(html: String): String? {
        val doc = Jsoup.parse(html)

        doc.selectFirst("#top-navigation a[href^=/biri/]")?.let { el ->
            val title = el.attr("title").trim()
            if (title.isNotEmpty()) return title
            val text = el.text().trim()
            if (text.isNotEmpty()) return text
        }

        // Mobile/alternative markups.
        doc.selectFirst("a.mobile-only-nick")?.text()?.trim()?.let {
            if (it.isNotEmpty()) return it
        }
        doc.selectFirst("#owner-profile a[href^=/biri/]")?.text()?.trim()?.let {
            if (it.isNotEmpty()) return it
        }

        return null
    }
}
