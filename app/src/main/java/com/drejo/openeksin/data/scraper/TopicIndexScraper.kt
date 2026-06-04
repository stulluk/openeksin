package com.drejo.openeksin.data.scraper

import com.drejo.openeksin.data.model.Topic
import org.jsoup.Jsoup

/**
 * Parses an eksisozluk topic-index page (today / agenda / popular) into a list
 * of [Topic]. Matches the website's public HTML structure:
 *
 *   <ul class="topic-list partial">
 *     <li><a href="/baslik--123">title <small>42</small></a></li>
 *   </ul>
 */
object TopicIndexScraper {

    fun parse(html: String): List<Topic> {
        val document = Jsoup.parse(html)
        val anchors = document.select("ul.topic-list.partial li a")
            .ifEmpty { document.select("ul.topic-list li a") }

        return anchors.mapNotNull { a ->
            val count = a.selectFirst("small")?.text().orEmpty().trim()
            // gündem/bugün: title is the anchor's own text + <small> count.
            // debe: title lives in a nested <span class="caption">.
            val caption = a.selectFirst("span.caption")?.text()?.trim()
            val title = (caption ?: a.ownText().trim())
            val link = a.attr("href").trim()
            if (title.isEmpty() || link.isEmpty()) {
                null
            } else {
                Topic(title = title, link = link, entryCount = count)
            }
        }
    }
}
