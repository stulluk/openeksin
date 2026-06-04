package com.drejo.openeksin.ui.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drejo.openeksin.data.model.Topic
import com.drejo.openeksin.ui.theme.LocalEkColors
import com.drejo.openeksin.ui.theme.TextSizes

@Composable
fun TopicListScreen(
    state: TopicListUiState,
    onRetry: () -> Unit,
    onVerifyCloudflare: (String) -> Unit,
    onTopicClick: (Topic) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            is TopicListUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is TopicListUiState.Success -> {
                TopicList(topics = state.topics, onTopicClick = onTopicClick)
            }

            is TopicListUiState.Error -> {
                CenteredMessage(
                    message = state.message,
                    actionLabel = "tekrar dene",
                    onAction = onRetry,
                )
            }

            is TopicListUiState.NeedsCloudflare -> {
                CenteredMessage(
                    message = "cloudflare doğrulaması gerekiyor",
                    actionLabel = "doğrula",
                    onAction = { onVerifyCloudflare(state.challengeUrl) },
                )
            }
        }
    }
}

@Composable
private fun TopicList(topics: List<Topic>, onTopicClick: (Topic) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(topics) { topic ->
            TopicRow(topic = topic, onClick = { onTopicClick(topic) })
            HorizontalDivider(color = LocalEkColors.current.divider)
        }
    }
}

@Composable
private fun TopicRow(topic: Topic, onClick: () -> Unit) {
    val ek = LocalEkColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (topic.entryCount.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .widthIn(min = 36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ek.rankBadge)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = topic.entryCount,
                    color = ek.rankBadgeText,
                    fontSize = TextSizes.EntryAuthor,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            text = topic.title,
            fontSize = TextSizes.TopicTitle,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun CenteredMessage(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message)
        Button(onClick = onAction, modifier = Modifier.padding(top = 16.dp)) {
            Text(text = actionLabel)
        }
    }
}
