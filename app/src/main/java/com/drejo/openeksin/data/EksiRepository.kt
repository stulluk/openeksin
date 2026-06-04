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
}
