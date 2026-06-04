package com.drejo.openeksin.data

import com.drejo.openeksin.data.model.Topic
import com.drejo.openeksin.data.model.TopicDetail
import com.drejo.openeksin.data.remote.EksiClient
import com.drejo.openeksin.data.remote.Endpoints
import com.drejo.openeksin.data.scraper.ChannelScraper
import com.drejo.openeksin.data.scraper.EntryScraper
import com.drejo.openeksin.data.scraper.TopicIndexScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
}
