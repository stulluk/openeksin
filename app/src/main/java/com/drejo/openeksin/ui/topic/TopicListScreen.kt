package com.drejo.openeksin.ui.topic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drejo.openeksin.data.EksiRepository
import com.drejo.openeksin.data.Feed
import com.drejo.openeksin.data.model.Topic
import com.drejo.openeksin.data.remote.CloudflareException

sealed interface TopicListUiState {
    data object Loading : TopicListUiState
    data class Success(val topics: List<Topic>) : TopicListUiState
    data class Error(val message: String) : TopicListUiState
    data class NeedsCloudflare(val challengeUrl: String) : TopicListUiState
}

private class LoadGate {
    private var active = false
    fun tryBegin(): Boolean {
        if (active) return false
        active = true
        return true
    }
    fun end() { active = false }
    fun reset() { active = false }
}

/** One pager page: loads and renders a single feed's topic list with infinite scroll. */
@Composable
fun FeedPage(
    feed: Feed,
    reloadKey: Int,
    onVerifyCloudflare: (String) -> Unit,
    onTopicClick: (Topic) -> Unit,
    modifier: Modifier = Modifier,
) {
    val repository = remember { EksiRepository() }
    val topics = remember { mutableStateListOf<Topic>() }
    var initialLoading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var nextPage by remember(feed.path) { mutableIntStateOf(2) }
    var cloudflareUrl by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val loadGate = remember { LoadGate() }
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }

    suspend fun enrichIfDebe(items: List<Topic>): List<Topic> =
        if (repository.isDebeFeed(feed.path)) repository.enrichDebeFavoriteCounts(items) else items

    suspend fun loadFirstPage() {
        hasMore = true
        nextPage = 2
        cloudflareUrl = null
        errorMessage = null
        loadGate.reset()
        val first = repository.topics(feed.path, 1)
        topics.clear()
        topics.addAll(first)
        hasMore = first.isNotEmpty()
    }

    LaunchedEffect(feed.path, reloadKey) {
        initialLoading = true
        loadingMore = false
        refreshing = false
        topics.clear()
        try {
            loadFirstPage()
            initialLoading = false
            if (repository.isDebeFeed(feed.path)) {
                val enriched = enrichIfDebe(topics.toList())
                topics.clear()
                topics.addAll(enriched)
            }
        } catch (e: CloudflareException) {
            cloudflareUrl = e.challengeUrl
        } catch (e: Exception) {
            errorMessage = e.message ?: "error"
        } finally {
            initialLoading = false
        }
    }

    val state = when {
        cloudflareUrl != null -> TopicListUiState.NeedsCloudflare(cloudflareUrl!!)
        errorMessage != null && topics.isEmpty() -> TopicListUiState.Error(errorMessage!!)
        initialLoading && topics.isEmpty() -> TopicListUiState.Loading
        else -> TopicListUiState.Success(topics)
    }

    TopicListScreen(
        state = state,
        loadingMore = loadingMore,
        isRefreshing = refreshing,
        onRefresh = {
            if (refreshing || initialLoading) return@TopicListScreen
            refreshing = true
            scope.launch {
                try {
                    loadFirstPage()
                    if (repository.isDebeFeed(feed.path)) {
                        val enriched = enrichIfDebe(topics.toList())
                        topics.clear()
                        topics.addAll(enriched)
                    }
                } catch (e: CloudflareException) {
                    cloudflareUrl = e.challengeUrl
                } catch (e: Exception) {
                    errorMessage = e.message ?: "error"
                } finally {
                    refreshing = false
                }
            }
        },
        onNearEnd = {
            if (hasMore && !loadingMore && loadGate.tryBegin()) {
                loadingMore = true
                scope.launch {
                    try {
                        val more = repository.topics(feed.path, nextPage)
                        if (more.isEmpty()) {
                            hasMore = false
                        } else {
                            topics.addAll(enrichIfDebe(more))
                            nextPage++
                        }
                    } catch (_: Exception) {
                        hasMore = false
                    } finally {
                        loadingMore = false
                        loadGate.end()
                    }
                }
            }
        },
        onVerifyCloudflare = onVerifyCloudflare,
        onTopicClick = onTopicClick,
        modifier = modifier,
    )
}

@Composable
fun TopicListScreen(
    state: TopicListUiState,
    loadingMore: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onNearEnd: () -> Unit = {},
    onVerifyCloudflare: (String) -> Unit,
    onTopicClick: (Topic) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            is TopicListUiState.Loading ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

            is TopicListUiState.Success ->
                Box(modifier = Modifier.fillMaxSize()) {
                    NativeTopicList(
                        topics = state.topics,
                        onTopicClick = onTopicClick,
                        onNearEnd = onNearEnd,
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                    )
                    if (loadingMore) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                        )
                    }
                }

            is TopicListUiState.Error ->
                CenteredMessage(message = state.message)

            is TopicListUiState.NeedsCloudflare ->
                CenteredMessage(
                    message = "cloudflare doğrulaması gerekiyor",
                    actionLabel = "doğrula",
                    onAction = { onVerifyCloudflare(state.challengeUrl) },
                )
        }
    }
}

@Composable
private fun CenteredMessage(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message)
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction, modifier = Modifier.padding(top = 16.dp)) {
                Text(text = actionLabel)
            }
        }
    }
}


