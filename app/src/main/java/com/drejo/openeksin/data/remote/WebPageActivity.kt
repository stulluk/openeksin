package com.drejo.openeksin.data.remote

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

/**
 * A plain in-app browser that loads an eksisozluk page using the shared session
 * cookies (so authenticated pages like /mesaj and /basliklar/olay render while
 * logged in). Unlike [CloudflareActivity] it does not auto-finish.
 */
class WebPageActivity : ComponentActivity() {

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
        webView.webViewClient = WebViewClient()
        webView.loadUrl(targetUrl)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) webView.goBack() else finish()
                }
            },
        )
    }

    companion object {
        const val EXTRA_URL = "extra_url"
    }
}
