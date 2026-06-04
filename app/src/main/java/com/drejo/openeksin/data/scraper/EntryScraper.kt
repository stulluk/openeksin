package com.drejo.openeksin.data.scraper

import com.drejo.openeksin.data.model.Entry
import com.drejo.openeksin.data.model.TopicDetail
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Parses an eksisozluk topic page into a [TopicDetail]. Matches the website's
 * HTML structure:
 *
 *   <h1 id="title" data-title="...">...</h1>
 *   <ul id="entry-item-list">
 *     <li data-id="..." data-author="..." data-favorite-count="...">
 *       <div class="content">...</div>
 *       <footer> ... <a class="entry-date ...">12.06.2024</a> ... </footer>
 *     </li>
 *   </ul>
 */
object EntryScraper {

    private const val BR_MARK = "\u0001"

    fun parse(html: String): TopicDetail {
        val doc = Jsoup.parse(html)

        val title = doc.selectFirst("#title")?.attr("data-title")?.trim().orEmpty()

        val entries = doc.select("#entry-item-list > li").mapNotNull { li ->
            val content = li.selectFirst(".content") ?: return@mapNotNull null
            Entry(
                id = li.attr("data-id"),
                author = li.attr("data-author"),
                date = li.selectFirst(".entry-date")?.text()?.trim().orEmpty(),
                content = contentToText(content),
                favoriteCount = li.attr("data-favorite-count"),
            )
        }

        return TopicDetail(title = title, entries = entries)
    }

    /** Flattens entry HTML to text while preserving line breaks. */
    private fun contentToText(content: Element): String {
        val clone = content.clone()
        clone.select("br").before(BR_MARK)
        return clone.text()
            .replace(" $BR_MARK ", "\n")
            .replace(BR_MARK, "\n")
            .trim()
    }
}
