package com.drejo.openeksin.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drejo.openeksin.data.EksiRepository
import com.drejo.openeksin.data.Feed
import com.drejo.openeksin.data.model.AuthorEntry
import com.drejo.openeksin.data.model.Topic
import com.drejo.openeksin.data.remote.Endpoints
import com.drejo.openeksin.data.scraper.AuthorProfileScraper
import com.drejo.openeksin.ui.theme.EksiPalette
import com.drejo.openeksin.ui.theme.LocalEkColors
import com.drejo.openeksin.ui.topic.FeedTabRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorEntriesScreen(
    nick: String,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    onBack: () -> Unit,
    onOpenTopic: (Topic) -> Unit,
) {
    val ek = LocalEkColors.current
    val repository = remember { EksiRepository() }
    val tabs = remember(nick) {
        listOf(
            Feed("entry'ler", Endpoints.authorEntriesPath(nick)),
            Feed("en çok favorilenenler", Endpoints.authorTopFavoritedPath(nick)),
        )
    }
    val entries = remember { mutableStateListOf<AuthorEntry>() }
    var highlight by remember { mutableStateOf<AuthorProfileScraper.Highlight?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableIntStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    suspend fun loadPage(nextPage: Int, append: Boolean) {
        val feedPath = tabs[selectedTab].path
        val (batch, more) = repository.authorFeed(feedPath, nick, nextPage)
        if (append) entries.addAll(batch) else {
            entries.clear()
            entries.addAll(batch)
        }
        hasMore = more && batch.isNotEmpty()
        page = nextPage
    }

    LaunchedEffect(nick) {
        highlight = try {
            repository.authorProfileHighlight(nick)
        } catch (_: Exception) {
            null
        }
    }

    LaunchedEffect(nick, selectedTab) {
        loading = true
        error = null
        page = 1
        hasMore = true
        entries.clear()
        try {
            loadPage(1, append = false)
        } catch (e: Exception) {
            error = e.message ?: "yüklenemedi"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(listState, hasMore, selectedTab) {
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last to info.totalItemsCount
        }.collect { (lastVisible, total) ->
            if (!hasMore || loadingMore || loading || total == 0) return@collect
            if (lastVisible >= total - 3) {
                loadingMore = true
                try {
                    loadPage(page + 1, append = true)
                } catch (_: Exception) {
                    hasMore = false
                } finally {
                    loadingMore = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(nick) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "geri", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = EksiPalette.Toolbar,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                    ),
                )
                FeedTabRow(
                    feeds = tabs,
                    selectedIndex = selectedTab,
                    onTabSelected = onTabChange,
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                error != null -> Text(error!!, modifier = Modifier.align(Alignment.Center).padding(24.dp))
                entries.isEmpty() && highlight == null -> Text("entry yok", modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    if (selectedTab == 0) {
                        highlight?.let { h ->
                            item(key = "highlight-${h.entryLink}") {
                                HighlightRow(h) {
                                    onOpenTopic(Topic(title = h.title, link = h.entryLink, entryCount = ""))
                                }
                                HorizontalDivider(color = ek.divider)
                            }
                        }
                    }
                    items(entries, key = { "${selectedTab}-${it.entry.id}" }) { row ->
                        AuthorEntryRow(row) {
                            onOpenTopic(
                                Topic(
                                    title = row.topicTitle,
                                    link = "/entry/${row.entry.id}",
                                    entryCount = "",
                                ),
                            )
                        }
                        HorizontalDivider(color = ek.divider)
                    }
                    if (loadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightRow(highlight: AuthorProfileScraper.Highlight, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            "en beğenilen entry",
            color = EksiPalette.LightSecondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            highlight.title,
            color = EksiPalette.Blue,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun AuthorEntryRow(row: AuthorEntry, onClick: () -> Unit) {
    val ek = LocalEkColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(row.topicTitle, color = EksiPalette.Blue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(
            row.entry.content.lineSequence().first().take(280),
            color = ek.mainText,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            "${row.entry.date}  ♥ ${row.entry.favoriteCount}",
            color = EksiPalette.LightSecondaryText,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
