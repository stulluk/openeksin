package com.drejo.openeksin.data.scraper

import com.drejo.openeksin.data.model.ContentSegment
import com.drejo.openeksin.data.model.Entry
import com.drejo.openeksin.data.model.EntryComposeForm
import com.drejo.openeksin.data.model.EntryEditForm
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
                flags = li.attr("data-flags"),
            )
        }

        val pager = doc.selectFirst("#topic .pager")
        val currentPage = pager?.attr("data-currentpage")?.toIntOrNull() ?: 1
        val pageCount = pager?.attr("data-pagecount")?.toIntOrNull() ?: 1

        val topicId = titleEl?.attr("data-id").orEmpty()
        val trackLink = doc.getElementById("track-topic-link")
        val isTracked = trackLink?.attr("data-tracked") == "1"
        val showAllUrl = doc.selectFirst(".showall")?.attr("href").orEmpty()
        val draft = doc.selectFirst("form[action=/entry/ekle] #editbox")?.ownText().orEmpty()

        return TopicDetail(
            title = title,
            titlePath = titlePath,
            entries = entries,
            currentPage = currentPage,
            pageCount = pageCount,
            topicId = topicId,
            isTracked = isTracked,
            showAllUrl = showAllUrl,
            draft = draft,
        )
    }

    /** Parses the hidden compose form on a logged-in topic page. */
    fun parseComposeForm(html: String): EntryComposeForm? {
        val doc = Jsoup.parse(html)
        val form = doc.selectFirst("form[action=/entry/ekle]") ?: return null
        val token = form.selectFirst("input[name=__RequestVerificationToken]")?.attr("value")
        val title = form.selectFirst("input[name=Title]")?.attr("value")
        val id = form.selectFirst("input[name=Id]")?.attr("value")
        val startTime = form.selectFirst("input[name=InputStartTime]")?.attr("value")
        if (token.isNullOrBlank() || title.isNullOrBlank() || id.isNullOrBlank()) return null
        return EntryComposeForm(
            token = token,
            title = title,
            topicId = id,
            inputStartTime = startTime.orEmpty(),
        )
    }

    /** Parses the edit form at /entry/duzelt/{id}. */
    fun parseEditForm(html: String, entryId: String): EntryEditForm? {
        val doc = Jsoup.parse(html)
        val token = doc.selectFirst("form[action=/entry/duzelt/$entryId] input[name=__RequestVerificationToken]")
            ?.attr("value")
            ?: doc.selectFirst("form[action^=/entry/duzelt] input[name=__RequestVerificationToken]")
                ?.attr("value")
        val title = doc.selectFirst("input[name=Title]")?.attr("value")
        val startTime = doc.selectFirst("input[name=InputStartTime]")?.attr("value")
        val content = doc.getElementById("editbox")?.text()?.trim().orEmpty()
        if (token.isNullOrBlank() || title.isNullOrBlank()) return null
        return EntryEditForm(
            token = token,
            title = title,
            inputStartTime = startTime.orEmpty(),
            content = content,
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
        // Trim leading/trailing whitespace so entries don't render with the
        // source HTML's indentation (eksi wraps content with newlines + spaces).
        while (merged.isNotEmpty() && merged.first().href == null) {
            val trimmed = merged.first().text.trimStart()
            if (trimmed.isEmpty()) merged.removeAt(0)
            else { merged[0] = merged.first().copy(text = trimmed); break }
        }
        while (merged.isNotEmpty() && merged.last().href == null) {
            val trimmed = merged.last().text.trimEnd()
            if (trimmed.isEmpty()) merged.removeAt(merged.lastIndex)
            else { merged[merged.lastIndex] = merged.last().copy(text = trimmed); break }
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
