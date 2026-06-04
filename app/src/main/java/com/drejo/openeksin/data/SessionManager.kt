package com.drejo.openeksin.data

import android.webkit.CookieManager
import com.drejo.openeksin.data.remote.EksiClient
import com.drejo.openeksin.data.remote.Endpoints
import com.drejo.openeksin.data.scraper.LoginScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Tracks the logged-in session. Cookies live in the Android [CookieManager]
 * (shared with okhttp via WebViewCookieJar), so login state is derived by
 * fetching the home page and scraping the user's nick.
 */
object SessionManager {

    private val _nick = MutableStateFlow<String?>(null)
    val nick: StateFlow<String?> = _nick.asStateFlow()

    val isLoggedIn: Boolean get() = _nick.value != null

    /** Fetches the home page and updates [nick]. Returns the nick or null. */
    suspend fun refresh(): String? = withContext(Dispatchers.IO) {
        val nick = try {
            LoginScraper.parseNick(EksiClient.getHtml(Endpoints.BASE, ajaxPartial = false))
        } catch (e: Exception) {
            null
        }
        _nick.value = nick
        nick
    }

    /** Clears cookies and resets the session. */
    fun logout() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        _nick.value = null
    }
}
