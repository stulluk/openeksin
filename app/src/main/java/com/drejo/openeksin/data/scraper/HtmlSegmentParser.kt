package com.drejo.openeksin.data.scraper

import com.drejo.openeksin.data.model.ContentSegment
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/** Flattens eksisozluk HTML into plain-text and link runs. */
object HtmlSegmentParser {

    fun parse(content: Element): List<ContentSegment> {
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
        while (merged.isNotEmpty() && merged.first().href == null) {
            val trimmed = merged.first().text.trimStart()
            if (trimmed.isEmpty()) merged.removeAt(0)
            else {
                merged[0] = merged.first().copy(text = trimmed)
                break
            }
        }
        while (merged.isNotEmpty() && merged.last().href == null) {
            val trimmed = merged.last().text.trimEnd()
            if (trimmed.isEmpty()) merged.removeAt(merged.lastIndex)
            else {
                merged[merged.lastIndex] = merged.last().copy(text = trimmed)
                break
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
