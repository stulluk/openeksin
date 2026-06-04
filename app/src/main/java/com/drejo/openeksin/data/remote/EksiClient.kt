package com.drejo.openeksin.data.remote

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

/** Raised when a response is a Cloudflare challenge page rather than content. */
class CloudflareException(val challengeUrl: String) : Exception("Cloudflare challenge required")

/**
 * Single shared okhttp client. It always sends a modern Chrome User-Agent and
 * shares cookies with the WebView via [WebViewCookieJar].
 */
object EksiClient {

    val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(WebViewCookieJar())
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", Endpoints.USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "tr-TR,tr;q=0.9")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    /**
     * GET [url] and return the body as a string. Throws [CloudflareException] if
     * the response looks like a Cloudflare challenge so the caller can open the
     * WebView to solve it.
     *
     * @param ajaxPartial when true, sends `X-Requested-With: XMLHttpRequest`.
     *   eksisozluk only returns the topic-list fragment with this header; full
     *   pages (e.g. for login detection) must be fetched without it.
     */
    fun getHtml(url: String, ajaxPartial: Boolean = false): String {
        val builder = Request.Builder().url(url).get()
        if (ajaxPartial) {
            builder.header("X-Requested-With", "XMLHttpRequest")
        }
        okHttp.newCall(builder.build()).execute().use { response: Response ->
            val body = response.body?.string().orEmpty()
            if (isCloudflareChallenge(response.code, body)) {
                throw CloudflareException(url)
            }
            return body
        }
    }

    /**
     * POST a form-urlencoded body to [url] (with `X-Requested-With:
     * XMLHttpRequest`) using the shared session cookies. Returns the response
     * body; used for authenticated actions like favorite and vote.
     */
    fun postForm(url: String, params: Map<String, String>): String {
        val form = FormBody.Builder()
        params.forEach { (key, value) -> form.add(key, value) }
        val request = Request.Builder()
            .url(url)
            .header("X-Requested-With", "XMLHttpRequest")
            .post(form.build())
            .build()
        okHttp.newCall(request).execute().use { response: Response ->
            return response.body?.string().orEmpty()
        }
    }

    private fun isCloudflareChallenge(code: Int, body: String): Boolean {
        if (code != 403 && code != 503) {
            // Some challenges still return 200; detect by markers below.
        }
        val markers = listOf(
            "challenge-platform",
            "cf-challenge",
            "id=\"challenge-form\"",
            "class=\"challenge-form\"",
            "Just a moment",
            "cf_chl_opt",
        )
        return markers.any { body.contains(it, ignoreCase = true) }
    }
}
