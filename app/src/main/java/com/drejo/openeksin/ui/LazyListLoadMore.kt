package com.drejo.openeksin.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal const val LOAD_MORE_THRESHOLD = 3

/** Emits total item count when the viewport is near the list end; -1 otherwise. */
internal fun LazyListState.nearEndSignal(threshold: Int = LOAD_MORE_THRESHOLD): Int {
    val info = layoutInfo
    val total = info.totalItemsCount
    if (total == 0) return -1
    val last = info.visibleItemsInfo.lastOrNull()?.index ?: return -1
    return if (last >= total - threshold) total else -1
}

internal suspend fun LazyListState.watchNearEnd(
    threshold: Int = LOAD_MORE_THRESHOLD,
    onNearEnd: suspend () -> Unit,
) {
    snapshotFlow { nearEndSignal(threshold) }
        .distinctUntilChanged()
        .collect { signal ->
            if (signal >= 0) onNearEnd()
        }
}
