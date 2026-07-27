package com.drejo.openeksin.data.remote

import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
    fun getHtml(url: String, ajaxPartial: Boolean = false, referer: String? = null): String {
        val builder = Request.Builder().url(url).get()
        if (ajaxPartial) {
            builder.header("X-Requested-With", "XMLHttpRequest")
        }
        if (referer != null) {
            builder.header("Referer", referer)
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
     * POST a form-urlencoded body to [url] using the shared session cookies.
     * Returns the response body; used for authenticated actions like favorite
     * and vote. [ajax] toggles the `X-Requested-With: XMLHttpRequest` header.
     */
    fun postForm(
        url: String,
        params: Map<String, String>,
        ajax: Boolean = true,
        referer: String? = null,
    ): String = postFormResult(url, params, ajax, referer).second

    /** POST a raw application/x-www-form-urlencoded body. */
    fun postRawBody(
        url: String,
        body: String,
        ajax: Boolean = true,
        referer: String? = null,
    ): Pair<Boolean, String> {
        val mediaType = "application/x-www-form-urlencoded".toMediaType()
        val builder = Request.Builder()
            .url(url)
            .post(body.toRequestBody(mediaType))
        if (ajax) builder.header("X-Requested-With", "XMLHttpRequest")
        if (referer != null) builder.header("Referer", referer)
        okHttp.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            return (response.isSuccessful && text.contains("true", ignoreCase = true)) to text
        }
    }

    /** Like [postForm] but also reports whether the response was successful. */
    fun postFormResult(
        url: String,
        params: Map<String, String>,
        ajax: Boolean = true,
        referer: String? = null,
    ): Pair<Boolean, String> {
        val form = FormBody.Builder()
        params.forEach { (key, value) -> form.add(key, value) }
        val builder = Request.Builder().url(url).post(form.build())
        if (ajax) builder.header("X-Requested-With", "XMLHttpRequest")
        if (referer != null) builder.header("Referer", referer)
        okHttp.newCall(builder.build()).execute().use { response: Response ->
            val body = response.body?.string().orEmpty()
            return response.isSuccessful to body
        }
    }

    /**
     * Like [postFormResult] but treats any HTTP status below 400 as success, matching
     * the original Ekşin client (e.g. entry delete via [RemoveEntryObservable]).
     */
    fun postFormResultAcceptingRedirect(
        url: String,
        params: Map<String, String>,
        ajax: Boolean = true,
        referer: String? = null,
    ): Boolean {
        val form = FormBody.Builder()
        params.forEach { (key, value) -> form.add(key, value) }
        val builder = Request.Builder().url(url).post(form.build())
        if (ajax) builder.header("X-Requested-With", "XMLHttpRequest")
        if (referer != null) builder.header("Referer", referer)
        okHttp.newCall(builder.build()).execute().use { response ->
            return response.code < 400
        }
    }

    /**
     * POST a form and return the final request URL after redirects. Used for entry
     * submission where success is a redirect to /entry/{id}.
     */
    fun postFormFinalUrl(
        url: String,
        params: Map<String, String>,
        referer: String,
    ): String? {
        val form = FormBody.Builder()
        params.forEach { (key, value) -> form.add(key, value) }
        val request = Request.Builder()
            .url(url)
            .post(form.build())
            .header("Referer", referer)
            .build()
        okHttp.newCall(request).execute().use { response ->
            val finalUrl = response.request.url.toString()
            if (finalUrl.contains("/entry/")) return finalUrl
            if (response.isSuccessful) return finalUrl
            return null
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
