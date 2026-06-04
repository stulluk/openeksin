package com.drejo.openeksin.data.remote

import android.app.Activity
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * WebView-based login. Loads eksisozluk's /giris page so the user can sign in
 * (and Cloudflare can be cleared). Cookies are stored in the system
 * [CookieManager] and thus shared with okhttp via WebViewCookieJar.
 *
 * Login completion is detected when navigation leaves the /giris path (the site
 * redirects to the home page after a successful sign-in) and the page is not a
 * Cloudflare interstitial. The actual session is verified afterwards by
 * SessionManager.refresh().
 */
class LoginActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                if (url == null) return
                val isEksi = url.contains(Endpoints.HOST)
                val onLoginPath = url.contains("/giris") || url.contains("/login")
                val isChallenge = url.contains("challenge") || url.contains("__cf")
                if (isEksi && !onLoginPath && !isChallenge) {
                    CookieManager.getInstance().flush()
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }

        webView.loadUrl(Endpoints.LOGIN)
    }
}
