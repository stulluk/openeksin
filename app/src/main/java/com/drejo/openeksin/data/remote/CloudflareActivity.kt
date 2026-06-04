package com.drejo.openeksin.data.remote

import android.app.Activity
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Opens a WebView that loads eksisozluk.com so the user (or Cloudflare's
 * automatic JS challenge) can pass the bot check. Once a `cf_clearance` cookie
 * is present it finishes with [Activity.RESULT_OK]; the cookie is then shared
 * with okhttp through [WebViewCookieJar].
 */
class CloudflareActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetUrl = intent.getStringExtra(EXTRA_URL) ?: Endpoints.BASE

        val webView = WebView(this)
        setContentView(webView)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = Endpoints.USER_AGENT
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val cookie = CookieManager.getInstance().getCookie(Endpoints.BASE)
                if (cookie != null && cookie.contains("cf_clearance")) {
                    CookieManager.getInstance().flush()
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }

        webView.loadUrl(targetUrl)
    }

    companion object {
        const val EXTRA_URL = "extra_url"
    }
}
