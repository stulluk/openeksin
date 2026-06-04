package com.drejo.openeksin.data

import com.drejo.openeksin.data.model.Topic
import com.drejo.openeksin.data.model.TopicDetail
import com.drejo.openeksin.data.remote.EksiClient
import com.drejo.openeksin.data.remote.Endpoints
import com.drejo.openeksin.data.scraper.EntryScraper
import com.drejo.openeksin.data.scraper.TopicIndexScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Which topic index to load. */
enum class TopicFeed(val url: String) {
    AGENDA(Endpoints.AGENDA),
    DEBE(Endpoints.DEBE),
    TODAY(Endpoints.TODAY),
}

/** Read-only data access for the topic-index feeds. */
class EksiRepository {

    suspend fun topics(feed: TopicFeed): List<Topic> = withContext(Dispatchers.IO) {
        val html = EksiClient.getHtml(feed.url, ajaxPartial = true)
        TopicIndexScraper.parse(html)
    }

    /**
     * Loads a topic page (entries). [path] is a relative link from a [Topic]
     * (e.g. "/baslik--123" or "/entry/456?debe=true") or an absolute URL.
     */
    suspend fun entries(path: String): TopicDetail = withContext(Dispatchers.IO) {
        val url = if (path.startsWith("http")) path else Endpoints.BASE + path
        val html = EksiClient.getHtml(url, ajaxPartial = false)
        EntryScraper.parse(html)
    }
}
