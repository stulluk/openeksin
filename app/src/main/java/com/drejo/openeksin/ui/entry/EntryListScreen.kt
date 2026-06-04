package com.drejo.openeksin.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.drejo.openeksin.data.EksiRepository
import com.drejo.openeksin.data.model.Entry
import com.drejo.openeksin.data.model.Topic
import com.drejo.openeksin.data.model.TopicDetail
import com.drejo.openeksin.data.remote.CloudflareException
import com.drejo.openeksin.ui.theme.EksiPalette
import com.drejo.openeksin.ui.theme.LocalEkColors
import com.drejo.openeksin.ui.theme.TextSizes

private sealed interface EntryUiState {
    data object Loading : EntryUiState
    data class Success(val detail: TopicDetail) : EntryUiState
    data class Error(val message: String) : EntryUiState
    data class NeedsCloudflare(val challengeUrl: String) : EntryUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryListScreen(
    topic: Topic,
    onBack: () -> Unit,
    onVerifyCloudflare: (String) -> Unit,
) {
    val repository = remember { EksiRepository() }
    val state by produceState<EntryUiState>(EntryUiState.Loading, topic.link) {
        value = EntryUiState.Loading
        value = try {
            EntryUiState.Success(repository.entries(topic.link))
        } catch (e: CloudflareException) {
            EntryUiState.NeedsCloudflare(e.challengeUrl)
        } catch (e: Exception) {
            EntryUiState.Error(e.message ?: "error")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topic.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "geri",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EksiPalette.Toolbar,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val s = state) {
                is EntryUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is EntryUiState.Success ->
                    if (s.detail.entries.isEmpty()) {
                        Text(
                            text = "entry bulunamadı",
                            modifier = Modifier.align(Alignment.Center),
                        )
                    } else {
                        EntryList(s.detail.entries)
                    }

                is EntryUiState.Error ->
                    Text(
                        text = s.message,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )

                is EntryUiState.NeedsCloudflare ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("cloudflare doğrulaması gerekiyor")
                        Button(
                            onClick = { onVerifyCloudflare(s.challengeUrl) },
                            modifier = Modifier.padding(top = 16.dp),
                        ) { Text("doğrula") }
                    }
            }
        }
    }
}

@Composable
private fun EntryList(entries: List<Entry>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(entries) { entry ->
            EntryRow(entry)
            HorizontalDivider(color = LocalEkColors.current.divider)
        }
    }
}

@Composable
private fun EntryRow(entry: Entry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = entry.content,
            fontSize = TextSizes.EntryBody,
        )
        Spacer(modifier = Modifier.padding(top = 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.author,
                fontSize = TextSizes.EntryAuthor,
                fontWeight = FontWeight.Bold,
                color = EksiPalette.Blue,
            )
            Text(
                text = "  ${entry.date}",
                fontSize = TextSizes.EntryDate,
                color = LocalEkColors.current.secondaryText,
            )
        }
    }
}
