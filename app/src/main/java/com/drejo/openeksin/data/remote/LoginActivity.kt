package com.drejo.openeksin.data.remote

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.drejo.openeksin.data.DebugLoginInjector
import com.drejo.openeksin.data.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * WebView-based login. Loads eksisozluk's /giris page so the user can sign in
 * (and Cloudflare can be cleared). Cookies are stored in the system
 * [CookieManager] and thus shared with okhttp via WebViewCookieJar.
 *
 * Login completion is detected when navigation leaves the /giris path (the site
 * redirects to the home page after a successful sign-in) and the page is not a
 * Cloudflare interstitial. The actual session is verified afterwards by
 * SessionManager.refresh().
 *
 * Debug builds may use files/debug_login.json for automated login during development.
 * to auto-fill the form once Turnstile succeeds.
 */
class LoginActivity : Activity() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private var autoLoginCreds: DebugLoginInjector.Creds? = null
    private var autoLoginAttempts = 0
    private var autoLoginSubmitted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        autoLoginCreds = DebugLoginInjector.readCredentials(this)

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
                if (isEksi && onLoginPath && !isChallenge) {
                    scheduleAutoLogin(webView)
                }
                if (isEksi && !onLoginPath && !isChallenge) {
                    scope.launch {
                        val nick = SessionManager.refresh()
                        if (nick != null) {
                            DebugLoginInjector.clearCredentials(this@LoginActivity)
                            CookieManager.getInstance().flush()
                            setResult(RESULT_OK)
                            finish()
                        }
                    }
                }
            }
        }

        webView.loadUrl(Endpoints.LOGIN)
    }

    private fun scheduleAutoLogin(webView: WebView) {
        val creds = autoLoginCreds ?: return
        if (autoLoginSubmitted || autoLoginAttempts >= 30) {
            if (autoLoginAttempts >= 30 && !autoLoginSubmitted) {
                DebugLoginInjector.clearCredentials(this)
                autoLoginCreds = null
            }
            return
        }
        autoLoginAttempts++
        handler.postDelayed({
            DebugLoginInjector.tryAutoLogin(webView, creds) { submitted ->
                if (submitted) autoLoginSubmitted = true
            }
            if (!autoLoginSubmitted) scheduleAutoLogin(webView)
        }, 1000L)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        super.onDestroy()
    }
}
