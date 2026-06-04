package com.drejo.openeksin.data.remote

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * An okhttp [CookieJar] backed by the Android system [CookieManager]. This lets
 * the okhttp client and the login/Cloudflare WebView share the same cookies
 * (most importantly the `cf_clearance` cookie produced when the WebView solves
 * a Cloudflare challenge).
 */
class WebViewCookieJar : CookieJar {

    private val cookieManager: CookieManager = CookieManager.getInstance()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val urlString = url.toString()
        for (cookie in cookies) {
            cookieManager.setCookie(urlString, cookie.toString())
        }
        cookieManager.flush()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val raw = cookieManager.getCookie(url.toString()) ?: return emptyList()
        if (raw.isEmpty()) return emptyList()
        return raw.split(';').mapNotNull { pair ->
            Cookie.parse(url, pair.trim())
        }
    }
}
