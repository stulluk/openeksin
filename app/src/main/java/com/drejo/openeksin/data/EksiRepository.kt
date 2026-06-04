package com.drejo.openeksin.data

import com.drejo.openeksin.data.model.Message
import com.drejo.openeksin.data.model.MessageThread
import com.drejo.openeksin.data.model.Topic
import com.drejo.openeksin.data.model.TopicDetail
import com.drejo.openeksin.data.remote.EksiClient
import com.drejo.openeksin.data.remote.Endpoints
import com.drejo.openeksin.data.scraper.ChannelScraper
import com.drejo.openeksin.data.scraper.EntryScraper
import com.drejo.openeksin.data.scraper.MessageScraper
import com.drejo.openeksin.data.scraper.TopicIndexScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder

/** Read-only data access for topic feeds and entries. */
class EksiRepository {

    /** Loads a topic-index feed (gündem / debe / bugün / a channel) by URL. */
    suspend fun topics(path: String): List<Topic> = withContext(Dispatchers.IO) {
        val html = EksiClient.getHtml(path, ajaxPartial = true)
        TopicIndexScraper.parse(html)
    }

    /** Loads the channel list for the dynamic tabs. */
    suspend fun channels(): List<Feed> = withContext(Dispatchers.IO) {
        ChannelScraper.parse(EksiClient.getHtml(Endpoints.CHANNELS, ajaxPartial = true))
    }

    /**
     * Loads a topic page (entries). [path] is a relative link from a [Topic]
     * (e.g. "/baslik--123" or "/entry/456?debe=true") or an absolute URL.
     */
    suspend fun entries(path: String, page: Int = 1): TopicDetail = withContext(Dispatchers.IO) {
        val base = if (path.startsWith("http")) path else Endpoints.BASE + path
        val url = if (page <= 1) {
            base
        } else if (base.contains("?")) {
            "$base&p=$page"
        } else {
            "$base?p=$page"
        }
        val html = EksiClient.getHtml(url, ajaxPartial = false)
        EntryScraper.parse(html)
    }

    /** Favorites (or un-favorites) an entry. Returns true on success. */
    suspend fun favorite(entryId: String, remove: Boolean): Boolean = withContext(Dispatchers.IO) {
        val url = if (remove) Endpoints.UNFAVORITE else Endpoints.FAVORITE
        val body = EksiClient.postForm(url, mapOf("entryId" to entryId))
        body.contains("\"Success\":true", ignoreCase = true) ||
            body.contains("\"success\":true", ignoreCase = true) ||
            body.contains("Count", ignoreCase = true)
    }

    /** Votes on an entry. [rate] is "1" (artı) or "-1" (eksi). Returns true on success. */
    suspend fun vote(entryId: String, ownerId: String, rate: String): Boolean =
        withContext(Dispatchers.IO) {
            val body = EksiClient.postForm(
                Endpoints.VOTE,
                mapOf("id" to entryId, "rate" to rate, "owner" to ownerId),
            )
            body.contains("true", ignoreCase = true)
        }

    /** Loads the message inbox (requires an authenticated session). */
    suspend fun messages(): List<MessageThread> = withContext(Dispatchers.IO) {
        MessageScraper.parseInbox(EksiClient.getHtml(Endpoints.MESSAGES, ajaxPartial = false))
    }

    /** Loads a single conversation thread by its relative link. */
    suspend fun messageThread(link: String): List<Message> = withContext(Dispatchers.IO) {
        val url = if (link.startsWith("http")) link else Endpoints.BASE + link
        MessageScraper.parseThread(EksiClient.getHtml(url, ajaxPartial = false))
    }

    /**
     * Toggles a user relation. [code] is one of [Endpoints.REL_FOLLOW],
     * [Endpoints.REL_BLOCK], [Endpoints.REL_BLOCK_TITLE]. Returns true on success.
     * A Referer is required or the server rejects the request.
     */
    suspend fun toggleRelation(userId: String, code: String, add: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            if (userId.isBlank()) return@withContext false
            val base = if (add) Endpoints.RELATION_ADD else Endpoints.RELATION_REMOVE
            val url = "$base$userId?r=$code"
            EksiClient.postFormResult(url, mapOf("r" to code), ajax = true, referer = Endpoints.BASE).first
        }

    /** Sends a reply inside an existing thread. [threadLink] e.g. "/mesaj/123". */
    suspend fun sendReply(threadLink: String, message: String): Boolean =
        withContext(Dispatchers.IO) {
            val url = if (threadLink.startsWith("http")) threadLink else Endpoints.BASE + threadLink
            val html = EksiClient.getHtml(url, ajaxPartial = false)
            val (token, to) = MessageScraper.parseReplyForm(html)
            if (token.isNullOrEmpty() || to.isNullOrEmpty()) return@withContext false
            val threadId = threadLink.substringAfterLast("/").substringBefore("?")
            EksiClient.postFormResult(
                Endpoints.MESSAGE_SEND,
                mapOf(
                    "__RequestVerificationToken" to token,
                    "To" to to,
                    "ThreadId" to threadId,
                    "Message" to message,
                ),
                ajax = false,
                referer = url,
            ).first
        }

    /**
     * Runs a conversation action ("delete" or "archive") on a thread. Eksi has no
     * single-message delete — only whole-conversation delete via
     * /mesaj/processthread. Scrapes the form's token + composite threadId first.
     */
    suspend fun processThread(threadLink: String, action: String): Boolean =
        withContext(Dispatchers.IO) {
            val url = if (threadLink.startsWith("http")) threadLink else Endpoints.BASE + threadLink
            val html = EksiClient.getHtml(url, ajaxPartial = false)
            val form = org.jsoup.Jsoup.parse(html).selectFirst("#message-thread-form")
                ?: return@withContext false
            val token = form.selectFirst("input[name=__RequestVerificationToken]")?.attr("value")
            val threadId = form.selectFirst("input[name=threadId]")?.attr("value")
            if (token.isNullOrEmpty() || threadId.isNullOrEmpty()) return@withContext false
            EksiClient.postFormResult(
                "${Endpoints.BASE}/mesaj/processthread",
                mapOf("__RequestVerificationToken" to token, "threadId" to threadId, "action" to action),
                ajax = false,
                referer = url,
            ).first
        }

    /** Returns topic-title suggestions for [query] via the autocomplete endpoint. */
    suspend fun searchTitles(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val url = Endpoints.AUTOCOMPLETE + "?q=" + URLEncoder.encode(query, "UTF-8")
        val body = EksiClient.getHtml(url, ajaxPartial = true)
        runCatching {
            val arr = JSONObject(body).optJSONArray("Titles") ?: return@runCatching emptyList()
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrDefault(emptyList())
    }

    /** Sends a brand-new message to [to], using a token scraped from an entry page. */
    suspend fun sendNewMessage(to: String, message: String, entryId: String): Boolean =
        withContext(Dispatchers.IO) {
            val html = EksiClient.getHtml(Endpoints.ENTRY + entryId, ajaxPartial = false)
            val token = MessageScraper.parseSendToken(html) ?: return@withContext false
            EksiClient.postFormResult(
                Endpoints.MESSAGE_SEND_AJAX,
                mapOf(
                    "__RequestVerificationToken" to token,
                    "To" to to,
                    "Message" to message,
                ),
                ajax = true,
                referer = Endpoints.BASE,
            ).first
        }
}
