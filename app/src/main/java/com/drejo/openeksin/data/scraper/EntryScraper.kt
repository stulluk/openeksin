package com.drejo.openeksin.data.scraper

import com.drejo.openeksin.data.model.ContentSegment
import com.drejo.openeksin.data.model.Entry
import com.drejo.openeksin.data.model.TopicDetail
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Parses an eksisozluk topic page into a [TopicDetail], including per-entry
 * content with inline links and pagination state.
 */
object EntryScraper {

    fun parse(html: String): TopicDetail {
        val doc = Jsoup.parse(html)

        val titleEl = doc.selectFirst("#title")
        val title = titleEl?.attr("data-title")?.trim().orEmpty()
        val titlePath = titleEl?.selectFirst("a[itemprop=url]")?.attr("href").orEmpty()

        val entries = doc.select("#entry-item-list > li").mapNotNull { li ->
            val content = li.selectFirst(".content") ?: return@mapNotNull null
            val segments = buildSegments(content)
            Entry(
                id = li.attr("data-id"),
                author = li.attr("data-author"),
                authorId = li.attr("data-author-id"),
                date = li.selectFirst(".entry-date")?.text()?.trim().orEmpty(),
                content = segments.joinToString("") { it.text }.trim(),
                segments = segments,
                favoriteCount = li.attr("data-favorite-count"),
                isFavorite = li.attr("data-isfavorite") == "true",
            )
        }

        val pager = doc.selectFirst("#topic .pager")
        val currentPage = pager?.attr("data-currentpage")?.toIntOrNull() ?: 1
        val pageCount = pager?.attr("data-pagecount")?.toIntOrNull() ?: 1

        return TopicDetail(
            title = title,
            titlePath = titlePath,
            entries = entries,
            currentPage = currentPage,
            pageCount = pageCount,
        )
    }

    /** Flattens entry HTML into text/link runs, preserving line breaks. */
    private fun buildSegments(content: Element): List<ContentSegment> {
        val out = mutableListOf<ContentSegment>()
        walk(content, out)
        // Merge adjacent plain-text runs.
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
                        if (text.isNotEmpty()) {
                            out.add(ContentSegment(text, child.attr("href")))
                        }
                    }
                    else -> walk(child, out)
                }
            }
        }
    }
}
