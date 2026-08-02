package com.drejo.openeksin.data

import com.drejo.openeksin.data.model.AuthorEntry
import com.drejo.openeksin.data.model.Message
import com.drejo.openeksin.data.model.MessageThread
import com.drejo.openeksin.data.model.Topic
import com.drejo.openeksin.data.model.TopicDetail
import com.drejo.openeksin.data.remote.EksiClient
import com.drejo.openeksin.data.remote.Endpoints
import com.drejo.openeksin.data.scraper.AuthorEntriesScraper
import com.drejo.openeksin.data.scraper.AuthorProfileScraper
import com.drejo.openeksin.data.scraper.ChannelScraper
import com.drejo.openeksin.data.scraper.EntryScraper
import com.drejo.openeksin.data.scraper.EntryMetaScraper
import com.drejo.openeksin.data.scraper.MessageScraper
import com.drejo.openeksin.data.scraper.TopicIndexScraper
import com.drejo.openeksin.data.remote.TopicUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject
import java.net.URLEncoder

/** Read-only data access for topic feeds and entries. */
class EksiRepository {

    private val debeFavoriteCountCache = ConcurrentHashMap<String, String>()
    private val debeEnrichSemaphore = Semaphore(6)

    /** Loads a topic-index feed (gündem / debe / bugün / a channel) by URL. */
    suspend fun topics(path: String, page: Int = 1): List<Topic> = withContext(Dispatchers.IO) {
        val url = topicIndexUrl(path, page)
        val html = EksiClient.getHtml(url, ajaxPartial = true)
        TopicIndexScraper.parse(html)
    }

    fun isDebeFeed(path: String): Boolean =
        path == Endpoints.DEBE || path.endsWith("/debe") || path.contains("/debe?")

    /** Fills missing debe favorite counts from entry partials (list HTML omits them). */
    suspend fun enrichDebeFavoriteCounts(topics: List<Topic>): List<Topic> =
        withContext(Dispatchers.IO) {
            coroutineScope {
                topics.map { topic ->
                    async {
                        if (topic.entryCount.isNotEmpty()) return@async topic
                        val entryId = entryIdFromLink(topic.link) ?: return@async topic
                        debeFavoriteCountCache[entryId]?.let { cached ->
                            return@async topic.copy(entryCount = cached)
                        }
                        val count = debeEnrichSemaphore.withPermit {
                            fetchEntryFavoriteCount(entryId)
                        }
                        if (count.isNotEmpty()) {
                            debeFavoriteCountCache[entryId] = count
                            topic.copy(entryCount = count)
                        } else {
                            topic
                        }
                    }
                }.awaitAll()
            }
        }

    private fun entryIdFromLink(link: String): String? =
        Regex("""/entry/(\d+)""").find(link)?.groupValues?.get(1)

    private fun fetchEntryFavoriteCount(entryId: String): String = runCatching {
        val html = EksiClient.getHtml(Endpoints.ENTRY + entryId, ajaxPartial = true)
        EntryMetaScraper.favoriteCount(html)
    }.getOrDefault("")

    private fun topicIndexUrl(path: String, page: Int): String {
        if (page <= 1) return path
        // "bugün" uses /index/feedrefresh — no paging in the app feed endpoint.
        if (path.contains("/index/feedrefresh")) return path
        // tarihte bugün / bugün-style paths use a /N suffix (matches Ekşin IndexObservable).
        if (path.contains("/basliklar/tarihte-bugun") || path.contains("/basliklar/bugun")) {
            return "$path/$page"
        }
        return if (path.contains("?")) "$path&p=$page" else "$path?p=$page"
    }

    /** Loads the channel list for the dynamic tabs. */
    suspend fun channels(): List<Feed> = withContext(Dispatchers.IO) {
        ChannelScraper.parse(EksiClient.getHtml(Endpoints.CHANNELS, ajaxPartial = true))
    }

    /** Warms the entry cache before navigation so the first paint is faster. */
    fun prefetchEntries(path: String, page: Int = 1) {
        EntryCache.prefetch(path, page) { loadEntriesUncached(path, page) }
    }

    /**
     * Loads a topic page (entries). [path] is a relative link from a [Topic]
     * (e.g. "/baslik--123" or "/entry/456?debe=true") or an absolute URL.
     */
    suspend fun entries(path: String, page: Int = 1): TopicDetail = withContext(Dispatchers.IO) {
        EntryCache.getOrLoad(path, page) { loadEntriesUncached(path, page) }
    }

    private suspend fun loadEntriesUncached(path: String, page: Int): TopicDetail {
        val base = if (path.startsWith("http")) path else Endpoints.BASE + path
        val url = if (page <= 1) {
            base
        } else if (base.contains("?")) {
            "$base&p=$page"
        } else {
            "$base?p=$page"
        }
        val html = EksiClient.getHtml(url, ajaxPartial = false)
        return EntryScraper.parse(html)
    }

    private object EntryCache {
        private val prefetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val completed = ConcurrentHashMap<String, TopicDetail>()
        private val inflight = ConcurrentHashMap<String, Deferred<TopicDetail>>()

        private fun key(path: String, page: Int) = "$path|$page"

        fun prefetch(path: String, page: Int, loader: suspend () -> TopicDetail) {
            val cacheKey = key(path, page)
            if (completed.containsKey(cacheKey) || inflight.containsKey(cacheKey)) return
            inflight[cacheKey] = prefetchScope.async {
                try {
                    loader().also { completed[cacheKey] = it }
                } finally {
                    inflight.remove(cacheKey)
                }
            }
        }

        suspend fun getOrLoad(path: String, page: Int, loader: suspend () -> TopicDetail): TopicDetail {
            val cacheKey = key(path, page)
            completed[cacheKey]?.let { return it }
            inflight[cacheKey]?.let { return it.await() }
            return coroutineScope {
                val job = async {
                    loader().also { completed[cacheKey] = it }
                }
                inflight[cacheKey] = job
                try {
                    job.await()
                } finally {
                    inflight.remove(cacheKey)
                }
            }
        }

        fun peek(path: String, page: Int): TopicDetail? = completed[key(path, page)]
    }

    /** Returns cached topic page data if prefetch or a prior load already completed. */
    fun peekEntries(path: String, page: Int = 1): TopicDetail? = EntryCache.peek(path, page)

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

    /** Featured entry from the profile intro block, if present. */
    suspend fun authorProfileHighlight(nick: String): AuthorProfileScraper.Highlight? =
        withContext(Dispatchers.IO) {
            val html = EksiClient.getHtml(Endpoints.authorProfile(nick), ajaxPartial = false)
            AuthorProfileScraper.parseHighlight(html)
        }

    /** Loads one page of an author feed (son-entryleri, en-cok-favorilenen-entryleri, …). */
    suspend fun authorFeed(relativePath: String, nick: String, page: Int = 1): Pair<List<AuthorEntry>, Boolean> =
        withContext(Dispatchers.IO) {
            val url = Endpoints.authorFeedPage(relativePath, page)
            val html = EksiClient.getHtml(url, ajaxPartial = true, referer = Endpoints.authorProfile(nick))
            AuthorEntriesScraper.parse(html) to AuthorEntriesScraper.hasMore(html)
        }

    /** Loads one page of a user's recent entries (requires login for own profile). */
    suspend fun authorEntries(nick: String, page: Int = 1): Pair<List<AuthorEntry>, Boolean> =
        authorFeed(Endpoints.authorEntriesPath(nick), nick, page)

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

    /** Saves a draft entry on the topic page (/savedraft), matching Ekşin. */
    suspend fun saveDraft(topicPath: String, topicTitle: String, content: String): Boolean =
        withContext(Dispatchers.IO) {
            if (content.isBlank() || topicTitle.isBlank()) return@withContext false
            val topicUrl = if (topicPath.startsWith("http")) topicPath else Endpoints.BASE + topicPath
            val cleanUrl = topicUrl.substringBefore("?")
            val body = buildString {
                append("title=")
                append(URLEncoder.encode(topicTitle, "UTF-8"))
                append("&content=")
                append(URLEncoder.encode(content, "UTF-8"))
            }
            EksiClient.postRawBody(
                "$cleanUrl/savedraft",
                body,
                ajax = true,
                referer = topicUrl,
            ).first
        }

    /**
     * Posts a new entry to the topic at [topicPath]. Scrapes the compose form from
     * the topic page first. Returns the final entry URL on success.
     */
    suspend fun addEntry(topicPath: String, content: String): Result<String> =
        withContext(Dispatchers.IO) {
            if (content.isBlank()) return@withContext Result.failure(IllegalArgumentException("empty"))
            val topicUrl = if (topicPath.startsWith("http")) topicPath else Endpoints.BASE + topicPath
            val html = EksiClient.getHtml(topicUrl, ajaxPartial = false)
            val form = EntryScraper.parseComposeForm(html)
                ?: return@withContext Result.failure(IllegalStateException("not_logged_in"))
            val detail = EntryScraper.parse(html)
            val canonicalPath = detail.titlePath.takeIf { it.isNotBlank() } ?: topicPath
            val referer = if (canonicalPath.startsWith("http")) {
                canonicalPath.substringBefore("?")
            } else {
                Endpoints.BASE + canonicalPath.substringBefore("?")
            }
            val (_, body) = EksiClient.postFormResult(
                Endpoints.ENTRY_ADD,
                mapOf(
                    "__RequestVerificationToken" to form.token,
                    "Title" to form.title,
                    "Id" to form.topicId,
                    "ReturnUrl" to "",
                    "Content" to content,
                    "InputStartTime" to form.inputStartTime,
                    "AddAsHidden" to "false",
                ),
                ajax = false,
                referer = referer,
            )
            val redirectEntry = Regex("""/entry/(\d+)""").find(body)?.groupValues?.get(1)
            if (redirectEntry != null) {
                return@withContext Result.success(Endpoints.ENTRY + redirectEntry)
            }
            // Site often returns 200 on /entry/ekle even on success; verify on last page.
            val verifyBase = if (canonicalPath.startsWith("http")) {
                canonicalPath.substringBefore("?")
            } else {
                Endpoints.BASE + canonicalPath.substringBefore("?")
            }
            val verifyUrl = if (detail.pageCount > 1) {
                "$verifyBase?p=${detail.pageCount}"
            } else {
                verifyBase
            }
            val verifyHtml = EksiClient.getHtml(verifyUrl, ajaxPartial = false)
            if (verifyHtml.contains(content)) {
                Result.success(verifyUrl)
            } else {
                Result.failure(IllegalStateException("post_failed"))
            }
        }

    /** Follows or unfollows a topic by numeric id. */
    suspend fun watchTopic(topicId: String, watch: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (topicId.isBlank()) return@withContext false
        val url = if (watch) Endpoints.watchTopic(topicId) else Endpoints.unwatchTopic(topicId)
        EksiClient.postFormResult(url, emptyMap(), ajax = true, referer = Endpoints.BASE).first
    }

    /** Deletes an entry owned by the logged-in user. */
    suspend fun deleteEntry(entryId: String): Boolean = withContext(Dispatchers.IO) {
        if (entryId.isBlank()) return@withContext false
        EksiClient.postFormResultAcceptingRedirect(
            Endpoints.ENTRY_DELETE,
            mapOf("id" to entryId),
            ajax = true,
            referer = Endpoints.ENTRY + entryId,
        )
    }

    /** Posts an edited entry body to /entry/duzelt/{id}. */
    suspend fun editEntry(entryId: String, content: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (content.isBlank()) return@withContext Result.failure(IllegalArgumentException("empty"))
            val url = Endpoints.entryEdit(entryId)
            val html = EksiClient.getHtml(url, ajaxPartial = false)
            val form = EntryScraper.parseEditForm(html, entryId)
                ?: return@withContext Result.failure(IllegalStateException("not_allowed"))
            val ok = EksiClient.postFormResult(
                url,
                mapOf(
                    "__RequestVerificationToken" to form.token,
                    "Title" to form.title,
                    "ReturnUrl" to "",
                    "InputStartTime" to form.inputStartTime,
                    "Content" to content,
                ),
                ajax = false,
                referer = url,
            ).first
            if (ok) Result.success(Unit) else Result.failure(IllegalStateException("post_failed"))
        }
}
