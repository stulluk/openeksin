package com.drejo.openeksin.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.webkit.CookieManager
import org.json.JSONObject
import java.io.File

/**
 * Debug-only helper: applies eksisozluk cookies pushed from a logged-in phone
 * via [scripts/sync_session_phone_to_emulator.sh]. The raw WebView Cookies DB
 * cannot be copied across devices reliably; [CookieManager.setCookie] is required.
 */
object SessionCookieImporter {

    private const val IMPORT_FILE = "import_session.json"

    /** Reads and applies a one-shot cookie bundle, then deletes the file. */
    fun applyIfPresent(context: Context) {
        val debug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!debug) return
        val file = File(context.filesDir, IMPORT_FILE)
        if (!file.exists()) return
        runCatching {
            val root = JSONObject(file.readText())
            val cookies = root.getJSONArray("cookies")
            val manager = CookieManager.getInstance()
            manager.setAcceptCookie(true)
            for (i in 0 until cookies.length()) {
                val entry = cookies.getJSONObject(i)
                val host = entry.getString("host")
                val name = entry.getString("name")
                val value = entry.getString("value")
                val domain = if (host.startsWith(".")) host else ".$host"
                manager.setCookie(
                    "https://eksisozluk.com",
                    "$name=$value; domain=$domain; path=/",
                )
            }
            manager.flush()
        }
        file.delete()
    }
}
