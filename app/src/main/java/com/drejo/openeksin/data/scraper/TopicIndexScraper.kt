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
            val link = a.attr("href").trim()
            if (link.startsWith("http") || a.hasClass("sponsored")) {
                return@mapNotNull null
            }
            val childElements = a.children()
            // debe: title in span.caption; favorite count may be in span.value or fetched later.
            val caption = a.selectFirst("span.caption")?.text()?.trim()
            val ownText = a.ownText().trim()
            val title = caption ?: ownText
            if (title.isEmpty() || link.isEmpty()) {
                null
            } else {
                val count = if (caption != null) {
                    a.selectFirst("span.value")?.text()?.trim().orEmpty()
                } else if (childElements.isEmpty()) {
                    // Index feeds omit <small> when the topic has a single entry.
                    "1"
                } else {
                    childElements.first().text().trim()
                }
                Topic(title = title, link = link, entryCount = count)
            }
        }
    }
}
