package com.drejo.openeksin.data.scraper

import com.drejo.openeksin.data.model.Message
import com.drejo.openeksin.data.model.MessageThread
import org.jsoup.Jsoup

/**
 * Parses eksisozluk message pages. Mirrors the selectors used by the original
 * app: the inbox is `#threads > article`, a conversation is
 * `#message-thread > article` (with `incoming`/`outgoing` classes).
 */
object MessageScraper {

    fun parseInbox(html: String): List<MessageThread> {
        return Jsoup.parse(html).select("#threads article").mapNotNull { article ->
            val threadId = article.selectFirst("input[name=threadId]")?.attr("value")
                ?: return@mapNotNull null
            val a = article.selectFirst("a[href^=/mesaj/]") ?: article.selectFirst("a")
            val h2 = a?.selectFirst("h2")
            val nick = h2?.ownText()?.trim().orEmpty()
            val badge = h2?.selectFirst("small")?.text()?.trim().orEmpty()
            val preview = a?.selectFirst("p")?.text()?.trim().orEmpty()
            val time = article.selectFirst("time")
            val date = (time?.text()?.takeIf { it.isNotBlank() } ?: time?.attr("datetime")).orEmpty()
            MessageThread(
                threadId = threadId,
                link = a?.attr("href").orEmpty(),
                nick = nick,
                unreadCount = badge,
                preview = preview,
                date = date,
            )
        }
    }

    fun parseThread(html: String): List<Message> {
        return Jsoup.parse(html).select("#message-thread article").map { article ->
            val time = article.selectFirst("time")
            Message(
                incoming = article.hasClass("incoming"),
                text = article.selectFirst("p")?.text()?.trim().orEmpty(),
                date = (time?.text()?.takeIf { it.isNotBlank() } ?: time?.attr("datetime")).orEmpty(),
            )
        }
    }
}
