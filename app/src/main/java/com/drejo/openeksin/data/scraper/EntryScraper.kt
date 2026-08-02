package com.drejo.openeksin.data.scraper

import com.drejo.openeksin.data.model.ContentSegment
import com.drejo.openeksin.data.model.Entry
import com.drejo.openeksin.data.model.EntryComposeForm
import com.drejo.openeksin.data.model.EntryEditForm
import com.drejo.openeksin.data.model.TopicDetail
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

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
            val segments = HtmlSegmentParser.parse(content)
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
}
