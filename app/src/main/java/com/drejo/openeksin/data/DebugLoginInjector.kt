package com.drejo.openeksin.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.webkit.WebView
import org.json.JSONObject
import java.io.File

/**
 * Debug-only helper: fills the eksisozluk login WebView after Turnstile succeeds.
 * Credentials may be pushed to files/debug_login.json on debug builds for local testing.
 */
object DebugLoginInjector {

    private const val LOGIN_FILE = "debug_login.json"

    internal data class Creds(val email: String, val password: String)
    internal fun readCredentials(context: Context): Creds? {
        val debug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!debug) return null
        val file = File(context.filesDir, LOGIN_FILE)
        if (!file.exists()) return null
        return runCatching {
            val root = JSONObject(file.readText())
            Creds(
                email = root.getString("email"),
                password = root.getString("password"),
            )
        }.getOrNull()
    }

    /** Deletes the credential file after a login attempt. */
    fun clearCredentials(context: Context) {
        File(context.filesDir, LOGIN_FILE).delete()
    }

    internal fun tryAutoLogin(webView: WebView, creds: Creds, onResult: (Boolean) -> Unit = {}) {
        val email = JSONObject.quote(creds.email)
        val password = JSONObject.quote(creds.password)
        val script = """
            (function() {
              var token = document.querySelector('[name="cf-turnstile-response"]');
              if (!token || !token.value) return 'wait';
              var user = document.getElementById('username');
              var pass = document.getElementById('password');
              if (!user || !pass) return 'noform';
              user.value = $email;
              pass.value = $password;
              user.dispatchEvent(new Event('input', { bubbles: true }));
              pass.dispatchEvent(new Event('input', { bubbles: true }));
              var btn = document.querySelector('button[type="submit"]');
              if (!btn) return 'nobutton';
              btn.click();
              return 'submit';
            })();
        """.trimIndent()
        webView.evaluateJavascript(script) { result ->
            onResult(result?.contains("submit") == true)
        }
    }
}
