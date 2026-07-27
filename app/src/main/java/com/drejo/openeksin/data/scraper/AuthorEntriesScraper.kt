package com.drejo.openeksin.data.scraper

import com.drejo.openeksin.data.model.AuthorEntry
import com.drejo.openeksin.data.model.ContentSegment
import com.drejo.openeksin.data.model.Entry
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Parses the AJAX fragment returned by `/son-entryleri?nick=…&p=…`.
 * Each `.topic-item` block contains a topic title and one entry row.
 */
object AuthorEntriesScraper {

    fun parse(html: String): List<AuthorEntry> {
        val doc = Jsoup.parse(html)
        return doc.select(".topic-item").mapNotNull { item -> parseTopicItem(item) }
    }

    /** True when the server signals there are no further pages. */
    fun hasMore(html: String): Boolean =
        !Jsoup.parse(html).selectFirst("#no-more-data").let { it != null }

    private fun parseTopicItem(item: Element): AuthorEntry? {
        val titleEl = item.selectFirst("#title") ?: return null
        val topicTitle = titleEl.attr("data-title").trim().ifEmpty { titleEl.text().trim() }
        val topicLink = titleEl.selectFirst("a[itemprop=url]")?.attr("href")?.trim().orEmpty()
        val li = item.selectFirst("#entry-item-list > li") ?: return null
        val entry = parseEntryLi(li) ?: return null
        if (topicTitle.isEmpty() || topicLink.isEmpty()) return null
        return AuthorEntry(topicTitle = topicTitle, topicLink = topicLink, entry = entry)
    }

    private fun parseEntryLi(li: Element): Entry? {
        val content = li.selectFirst(".content") ?: return null
        val segments = buildSegments(content)
        return Entry(
            id = li.attr("data-id"),
            author = li.attr("data-author"),
            authorId = li.attr("data-author-id"),
            date = li.selectFirst(".entry-date")?.text()?.trim().orEmpty(),
            content = segments.joinToString("") { it.text }.trim(),
            segments = segments,
            favoriteCount = li.attr("data-favorite-count"),
            isFavorite = li.attr("data-isfavorite") == "true",
            flags = li.attr("data-flags"),
        )
    }

    private fun buildSegments(content: Element): List<ContentSegment> {
        val out = mutableListOf<ContentSegment>()
        walk(content, out)
        val merged = mutableListOf<ContentSegment>()
        for (seg in out) {
            val last = merged.lastOrNull()
            if (seg.href == null && last != null && last.href == null) {
                merged[merged.lastIndex] = last.copy(text = last.text + seg.text)
            } else {
                merged.add(seg)
            }
        }
        return merged
    }

    private fun walk(node: Node, out: MutableList<ContentSegment>) {
        for (child in node.childNodes()) {
            when (child) {
                is TextNode -> out.add(ContentSegment(child.wholeText, null))
                is Element -> when (child.tagName()) {
                    "br" -> out.add(ContentSegment("\n", null))
                    "a" -> {
                        val text = child.text()
                        if (text.isNotEmpty()) out.add(ContentSegment(text, child.attr("href")))
                    }
                    else -> walk(child, out)
                }
            }
        }
    }
}
