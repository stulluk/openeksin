package com.drejo.openeksin.ui.entry

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import com.drejo.openeksin.R
import com.drejo.openeksin.data.model.ContentSegment
import com.drejo.openeksin.data.model.Entry
import java.util.concurrent.ConcurrentHashMap

private val ExternalLinkColor = 0xFF559CB4.toInt()
private val InternalLinkColor = 0xFF177DB4.toInt()

internal object EntrySpannableCache {
    private val cache = ConcurrentHashMap<String, SpannableStringBuilder>()

    fun warm(entries: List<Entry>) {
        for (entry in entries) {
            cache.getOrPut(entry.id) { buildEntrySpannable(entry) }
        }
    }

    fun get(entry: Entry): SpannableStringBuilder =
        cache.getOrPut(entry.id) { buildEntrySpannable(entry) }

    fun clear() {
        cache.clear()
    }
}

internal fun buildEntrySpannable(entry: Entry): SpannableStringBuilder =
    buildSegmentsSpannable(entry.segments)

internal fun buildSegmentsSpannable(
    segments: List<ContentSegment>,
    internalLinkColor: Int = InternalLinkColor,
    externalLinkColor: Int = ExternalLinkColor,
): SpannableStringBuilder {
    val builder = SpannableStringBuilder()
    for (seg in segments) {
        val start = builder.length
        builder.append(seg.text)
        val href = seg.href
        if (!href.isNullOrEmpty()) {
            val end = builder.length
            val external = href.startsWith("http")
            builder.setSpan(
                ForegroundColorSpan(if (external) externalLinkColor else internalLinkColor),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            builder.setSpan(
                StyleSpan(if (external) Typeface.NORMAL else Typeface.BOLD),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            builder.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        @Suppress("UNCHECKED_CAST")
                        (widget.getTag(R.id.tag_entry_link_click) as? (String) -> Unit)?.invoke(href)
                    }
                },
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }
    return builder
}

internal fun TextView.bindEntryBody(
    entry: Entry,
    expanded: Boolean,
    collapsedLines: Int,
    onLinkClick: (String) -> Unit,
    onOverflow: () -> Unit,
) {
    setTag(R.id.tag_entry_link_click, onLinkClick)
    text = EntrySpannableCache.get(entry)
    maxLines = if (expanded) Int.MAX_VALUE else collapsedLines
    if (!expanded && entry.content.length <= 280) {
        post {
            val layout = layout ?: return@post
            if (lineCount >= collapsedLines && layout.getEllipsisCount(collapsedLines - 1) > 0) {
                onOverflow()
            }
        }
    }
}

internal fun handleEntryLink(href: String, onOpenLink: (String, String) -> Unit) {
    onOpenLink(href, href.removePrefix("/").substringBefore("--").replace("-", " "))
}
