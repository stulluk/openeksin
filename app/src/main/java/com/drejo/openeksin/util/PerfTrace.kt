package com.drejo.openeksin.util

import android.os.SystemClock
import android.util.Log

/** Lightweight tap-to-content timing for entry open benchmarks (logcat tag: OpenEksinPerf). */
object PerfTrace {
    private const val TAG = "OpenEksinPerf"

    @Volatile
    private var topicTapMs: Long = 0L

    fun markTopicTap(link: String) {
        topicTapMs = SystemClock.elapsedRealtime()
        Log.i(TAG, "topic_tap link=$link t=$topicTapMs")
    }

    fun markEntryScreenOpen(link: String) {
        Log.i(TAG, "entry_screen_open link=$link dt=${delta()}ms")
    }

    fun markEntryFetchStart(link: String, page: Int) {
        Log.i(TAG, "entry_fetch_start link=$link page=$page dt=${delta()}ms")
    }

    fun markEntryFetchEnd(link: String, page: Int, entryCount: Int) {
        Log.i(TAG, "entry_fetch_end link=$link page=$page entries=$entryCount dt=${delta()}ms")
    }

    fun markEntryListVisible(link: String, page: Int, entryCount: Int) {
        Log.i(TAG, "entry_list_visible link=$link page=$page entries=$entryCount dt=${delta()}ms")
    }

    fun logScroll(event: String, detail: String = "") {
        Log.i(TAG, "scroll $event $detail")
    }

    private fun delta(): Long {
        val start = topicTapMs
        return if (start == 0L) -1L else SystemClock.elapsedRealtime() - start
    }
}
