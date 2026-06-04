package com.drejo.openeksin.data.scraper

import com.drejo.openeksin.data.Feed
import com.drejo.openeksin.data.remote.Endpoints
import org.jsoup.Jsoup

/**
 * Parses eksisozluk's /kanallar page into a list of channel [Feed]s. Channels
 * are the `a.index-link` anchors, e.g. `<a class="index-link"
 * href="/basliklar/kanal/spor">#spor</a>`.
 */
object ChannelScraper {

    fun parse(html: String): List<Feed> {
        return Jsoup.parse(html).select("a.index-link").mapNotNull { a ->
            val title = a.text().trim()
            val href = a.attr("href").trim()
            if (title.isEmpty() || href.isEmpty()) null
            else Feed(title = title, path = Endpoints.BASE + href)
        }
    }
}
