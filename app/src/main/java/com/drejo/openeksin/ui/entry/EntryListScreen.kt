package com.drejo.openeksin.ui.entry

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drejo.openeksin.data.EksiRepository
import com.drejo.openeksin.data.SessionManager
import com.drejo.openeksin.data.local.RelationStore
import com.drejo.openeksin.data.local.SavedEntry
import com.drejo.openeksin.data.local.SavedStore
import com.drejo.openeksin.data.model.Entry
import com.drejo.openeksin.data.model.Topic
import com.drejo.openeksin.data.model.TopicDetail
import com.drejo.openeksin.data.remote.CloudflareException
import com.drejo.openeksin.data.remote.Endpoints
import com.drejo.openeksin.data.remote.TopicUrl
import com.drejo.openeksin.ui.theme.EksiPalette
import com.drejo.openeksin.ui.theme.LocalEkColors
import com.drejo.openeksin.util.PerfTrace
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface EntryUiState {
    data object Loading : EntryUiState
    data class Success(val detail: TopicDetail) : EntryUiState
    data class Error(val message: String) : EntryUiState
    data class NeedsCloudflare(val challengeUrl: String) : EntryUiState
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EntryListScreen(
    topic: Topic,
    reloadKey: Int = 0,
    onBack: () -> Unit,
    onVerifyCloudflare: (String) -> Unit,
    onOpenLink: (href: String, title: String) -> Unit,
    onOpenAuthor: (String) -> Unit,
    onCompose: (Topic, String, String?) -> Unit,
    onReload: () -> Unit,
    onOpenSearch: () -> Unit,
    onLogin: () -> Unit,
) {
    val repository = remember { EksiRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isLoggedIn = SessionManager.isLoggedIn

    LaunchedEffect(topic.link) {
        PerfTrace.markEntryScreenOpen(topic.link)
    }

    var activeLink by remember(topic.link) { mutableStateOf(topic.link) }
    var topicMeta by remember { mutableStateOf<TopicDetail?>(null) }
    var isTracked by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var findOpen by remember { mutableStateOf(false) }

    var pageCount by remember(activeLink, reloadKey) {
        mutableIntStateOf(
            repository.peekEntries(activeLink, 1)?.pageCount?.coerceAtLeast(1) ?: 1,
        )
    }
    val pagerState = rememberPagerState(pageCount = { pageCount.coerceAtLeast(1) })

    fun navigateTopicLink(link: String) {
        activeLink = link
        scope.launch { pagerState.scrollToPage(0) }
    }

    val topicBasePath = remember(topicMeta, activeLink) {
        val path = topicMeta?.titlePath?.takeIf { it.isNotBlank() } ?: activeLink
        TopicUrl.clean(if (path.startsWith("http")) path.removePrefix(Endpoints.BASE) else path)
    }

    val currentPage = pagerState.currentPage + 1
    val displayTitle = topicMeta?.title?.ifBlank { topic.title } ?: topic.title
    val shareUrl = remember(topicMeta, activeLink) {
        val path = topicMeta?.titlePath?.takeIf { it.isNotBlank() } ?: activeLink
        if (path.startsWith("http")) path else Endpoints.BASE + path.substringBefore("?")
    }

    if (menuOpen) {
        EntryTopicMenuSheet(
            title = displayTitle,
            isLoggedIn = isLoggedIn,
            isTracked = isTracked,
            hasDraft = !topicMeta?.draft.isNullOrBlank(),
            showAllVisible = !topicMeta?.showAllUrl.isNullOrBlank(),
            onDismiss = { menuOpen = false },
            onShare = {
                menuOpen = false
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareUrl)
                }
                context.startActivity(Intent.createChooser(intent, "paylaş"))
            },
            onWatch = {
                menuOpen = false
                val id = topicMeta?.topicId.orEmpty()
                if (id.isBlank()) return@EntryTopicMenuSheet
                scope.launch {
                    val ok = repository.watchTopic(id, watch = !isTracked)
                    if (ok) {
                        isTracked = !isTracked
                        Toast.makeText(
                            context,
                            if (isTracked) "takip ediliyor" else "takip bırakıldı",
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        Toast.makeText(context, "işlem başarısız", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onWrite = {
                menuOpen = false
                if (!isLoggedIn) {
                    onLogin()
                } else {
                    onCompose(
                        topic.copy(
                            title = displayTitle,
                            link = topicMeta?.titlePath?.takeIf { it.isNotBlank() } ?: activeLink,
                        ),
                        topicMeta?.draft.orEmpty(),
                        null,
                    )
                }
            },
            onFindInTopic = {
                menuOpen = false
                findOpen = true
            },
            onShowAll = {
                menuOpen = false
                topicMeta?.showAllUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    navigateTopicLink(url)
                }
            },
            onSukelaAll = {
                menuOpen = false
                navigateTopicLink(TopicUrl.sukelaAll(topicBasePath))
            },
            onSukelaToday = {
                menuOpen = false
                navigateTopicLink(TopicUrl.sukelaToday(topicBasePath))
            },
        )
    }

    if (findOpen) {
        FindInTopicDialog(
            onDismiss = { findOpen = false },
            onSearch = { query ->
                findOpen = false
                navigateTopicLink(TopicUrl.findQuery(topicBasePath, query))
            },
            onQuickFilter = { filter ->
                findOpen = false
                val link = when (filter) {
                    QuickFindFilter.BuddyEntries -> TopicUrl.buddyEntriesInTopic(topicBasePath)
                    QuickFindFilter.Today -> TopicUrl.todayInTopic(topicBasePath)
                    QuickFindFilter.Nice -> TopicUrl.niceInTopic(topicBasePath)
                    QuickFindFilter.Links -> TopicUrl.linksInTopic(topicBasePath)
                }
                navigateTopicLink(link)
            },
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = displayTitle,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 16.sp,
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
                    actions = {
                        IconButton(onClick = onOpenSearch) {
                            Icon(Icons.Filled.Search, "ara", tint = Color.White)
                        }
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.ShortText, "menü", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = EksiPalette.Toolbar,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    ),
                )
                if (pageCount > 1) {
                    PagerBar(
                        current = currentPage,
                        count = pageCount,
                        onFirst = { scope.launch { pagerState.scrollToPage(0) } },
                        onLast = { scope.launch { pagerState.scrollToPage(pageCount - 1) } },
                    )
                }
            }
        },
    ) { innerPadding ->
        val listBg = LocalEkColors.current.entryListBg
        val topicSessionKey = remember(activeLink) { TopicUrl.clean(activeLink) }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(listBg),
            beyondViewportPageCount = 0,
            userScrollEnabled = pageCount > 1,
        ) { pageIndex ->
            EntryPage(
                sessionKey = topicSessionKey,
                topic = topic.copy(title = displayTitle, link = activeLink),
                page = pageIndex + 1,
                topicPageCount = pageCount,
                pagerState = pagerState,
                reloadKey = reloadKey,
                listBg = listBg,
                repository = repository,
                onVerifyCloudflare = onVerifyCloudflare,
                onOpenLink = onOpenLink,
                onOpenAuthor = onOpenAuthor,
                onPageCount = { count -> if (count > 0) pageCount = count },
                onMeta = { detail ->
                    topicMeta = detail
                    isTracked = detail.isTracked
                },
                onCompose = onCompose,
                onReload = onReload,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun EntryPage(
    sessionKey: String,
    topic: Topic,
    page: Int,
    topicPageCount: Int,
    pagerState: PagerState,
    reloadKey: Int,
    listBg: Color,
    repository: EksiRepository,
    onVerifyCloudflare: (String) -> Unit,
    onOpenLink: (String, String) -> Unit,
    onOpenAuthor: (String) -> Unit,
    onPageCount: (Int) -> Unit,
    onMeta: (TopicDetail) -> Unit,
    onCompose: (Topic, String, String?) -> Unit,
    onReload: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val fetchPath = topic.link
    val prefetched = remember(sessionKey, page, topic.link) {
        repository.peekEntries(topic.link, page)
    }
    var baseDetail by remember(sessionKey, page) { mutableStateOf(prefetched) }
    var initialLoading by remember(sessionKey, page) { mutableStateOf(prefetched == null) }
    var loadError by remember { mutableStateOf<EntryUiState?>(null) }
    val entries = remember(sessionKey, page) {
        mutableStateListOf<Entry>().apply {
            prefetched?.entries?.let { addAll(it) }
        }
    }
    var nextPageToLoad by remember(sessionKey, page) { mutableIntStateOf(page + 1) }
    var loadingMore by remember { mutableStateOf(false) }
    var menuEntry by remember { mutableStateOf<Entry?>(null) }
    val loadGate = remember { LoadGate() }

    LaunchedEffect(sessionKey, page, topic.link, reloadKey) {
        if (reloadKey == 0) {
            repository.peekEntries(topic.link, page)?.let { cached ->
                EntrySpannableCache.warm(cached.entries)
                baseDetail = cached
                if (entries.isEmpty()) {
                    entries.addAll(cached.entries)
                }
                initialLoading = false
                onPageCount(cached.pageCount)
                onMeta(cached)
                loadGate.reset()
                return@LaunchedEffect
            }
        }
        initialLoading = true
        loadError = null
        entries.clear()
        nextPageToLoad = page + 1
        loadingMore = false
        loadGate.reset()
        EntrySpannableCache.clear()
        try {
            PerfTrace.markEntryFetchStart(topic.link, page)
            val detail = repository.entries(fetchPath, page)
            withContext(Dispatchers.Default) {
                EntrySpannableCache.warm(detail.entries)
            }
            baseDetail = detail
            entries.addAll(detail.entries)
            PerfTrace.markEntryFetchEnd(topic.link, page, detail.entries.size)
            PerfTrace.logScroll("fetch_meta", "pageCount=${detail.pageCount} entries=${detail.entries.size}")
        } catch (e: CloudflareException) {
            loadError = EntryUiState.NeedsCloudflare(e.challengeUrl)
        } catch (e: Exception) {
            loadError = EntryUiState.Error(e.message ?: "error")
        } finally {
            initialLoading = false
        }
    }

    LaunchedEffect(baseDetail?.pageCount) {
        baseDetail?.pageCount?.let { onPageCount(it) }
        loadGate.reset()
    }
    LaunchedEffect(baseDetail) {
        baseDetail?.let { onMeta(it) }
    }

    val totalPages = baseDetail?.pageCount?.coerceAtLeast(1) ?: topicPageCount.coerceAtLeast(1)
    val hasMoreRemotePages = nextPageToLoad <= totalPages

    fun loadNextRemotePage() {
        val total = baseDetail?.pageCount?.coerceAtLeast(1) ?: topicPageCount.coerceAtLeast(1)
        if (nextPageToLoad > total || loadingMore || !loadGate.tryBegin()) {
            PerfTrace.logScroll(
                "load_more_skip",
                "loading=$loadingMore next=$nextPageToLoad total=$total base=${baseDetail?.pageCount} listSize=${entries.size}",
            )
            return
        }
        val pageToLoad = nextPageToLoad
        PerfTrace.logScroll("load_more_start", "apiPage=$pageToLoad path=$fetchPath listSize=${entries.size}")
        loadingMore = true
        scope.launch {
            try {
                val more = repository.entries(fetchPath, pageToLoad)
                withContext(Dispatchers.Default) {
                    EntrySpannableCache.warm(more.entries)
                }
                if (more.entries.isNotEmpty()) {
                    entries.addAll(more.entries)
                }
                nextPageToLoad = pageToLoad + 1
                PerfTrace.logScroll(
                    "load_more_done",
                    "apiPage=$pageToLoad added=${more.entries.size} listSize=${entries.size} next=$nextPageToLoad",
                )
            } catch (e: Exception) {
                PerfTrace.logScroll("load_more_error", "apiPage=$pageToLoad err=${e.message}")
            } finally {
                loadingMore = false
                loadGate.end()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(listBg)) {
        when {
            loadError is EntryUiState.NeedsCloudflare -> {
                val url = (loadError as EntryUiState.NeedsCloudflare).challengeUrl
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("cloudflare doğrulaması gerekiyor")
                    Button(
                        onClick = { onVerifyCloudflare(url) },
                        modifier = Modifier.padding(top = 16.dp),
                    ) { Text("doğrula") }
                }
            }

            loadError is EntryUiState.Error && entries.isEmpty() ->
                Text(
                    text = (loadError as EntryUiState.Error).message,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )

            initialLoading && entries.isEmpty() && loadError == null ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

            !initialLoading && entries.isEmpty() && loadError == null ->
                Text("entry bulunamadı", modifier = Modifier.align(Alignment.Center))

            else -> {
                NativeEntryList(
                    entries = entries,
                    topicLink = topic.link,
                    scrollPage = page,
                    canGoPrev = page > 1,
                    canGoNext = page < totalPages && !hasMoreRemotePages,
                    onNearEnd = { loadNextRemotePage() },
                    onRequestPrevPage = {
                        PerfTrace.logScroll("pager_prev", "from=$page")
                        scope.launch {
                            pagerState.scrollToPage((page - 2).coerceAtLeast(0))
                        }
                    },
                    onRequestNextPage = {
                        PerfTrace.logScroll("pager_next", "from=$page totalPages=$totalPages")
                        scope.launch {
                            pagerState.scrollToPage(page.coerceAtMost(totalPages - 1))
                        }
                    },
                    onLinkClick = { href -> handleEntryLink(href, onOpenLink) },
                    onAuthorClick = onOpenAuthor,
                    onMenuClick = { menuEntry = it },
                    modifier = Modifier.fillMaxSize(),
                )
                if (loadingMore) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                    )
                }
            }
        }
    }

    menuEntry?.let { entry ->
        EntryMenuSheet(
            entry = entry,
            topic = topic,
            scope = scope,
            onDismiss = { menuEntry = null },
            onCompose = onCompose,
            onReload = onReload,
        )
    }
}

@Composable
private fun PagerBar(
    current: Int,
    count: Int,
    onFirst: () -> Unit,
    onLast: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EksiPalette.TabBar)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (current > 1) {
            IconButton(onClick = onFirst, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Filled.FirstPage,
                    contentDescription = "ilk sayfa",
                    tint = EksiPalette.TabSelected,
                )
            }
        } else {
            Box(modifier = Modifier.size(48.dp))
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$current / $count",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = EksiPalette.TabSelected,
                modifier = Modifier
                    .border(1.dp, Color(0xFF666666), RoundedCornerShape(2.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        if (current < count) {
            IconButton(onClick = onLast, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Filled.LastPage,
                    contentDescription = "son sayfa",
                    tint = EksiPalette.TabSelected,
                )
            }
        } else {
            Box(modifier = Modifier.size(48.dp))
        }
    }
}

private class LoadGate {
    private var active = false

    fun tryBegin(): Boolean {
        if (active) return false
        active = true
        return true
    }

    fun end() {
        active = false
    }

    fun reset() {
        active = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryMenuSheet(
    entry: Entry,
    topic: Topic,
    scope: CoroutineScope,
    onDismiss: () -> Unit,
    onCompose: (Topic, String, String?) -> Unit,
    onReload: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { EksiRepository() }
    val nick by SessionManager.nick.collectAsState()
    val savedEntries by SavedStore.entries.collectAsState()
    val relations by RelationStore.state.collectAsState()
    val loggedIn = nick != null
    var isFav by remember(entry.id) { mutableStateOf(entry.isFavorite) }
    val isSaved = savedEntries.any { it.id == entry.id }
    val isFollowed = entry.author in relations.buddies
    val isBlocked = entry.author in relations.blocked
    val isTitleBlocked = entry.author in relations.blockedTitles
    var showCompose by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val entryUrl = "${Endpoints.BASE}/entry/${entry.id}"

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("entry sil") },
            text = { Text("#${entry.id} silinsin mi?") },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirm = false
                    scope.launch {
                        val ok = runCatching { repository.deleteEntry(entry.id) }.getOrDefault(false)
                        toast(if (ok) "entry silindi" else "silinemedi")
                        if (ok) onReload()
                        onDismiss()
                    }
                }) { Text("sil") }
            },
            dismissButton = { Button(onClick = { showDeleteConfirm = false }) { Text("vazgeç") } },
        )
    }

    if (showCompose) {
        ComposeMessageDialog(
            target = entry.author,
            onDismiss = { showCompose = false },
            onSend = { text ->
                showCompose = false
                scope.launch {
                    val ok = runCatching { repository.sendNewMessage(entry.author, text, entry.id) }
                        .getOrDefault(false)
                    toast(if (ok) "mesaj gönderildi" else "gönderilemedi")
                    onDismiss()
                }
            },
        )
    }
    fun share() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, entryUrl)
        }
        context.startActivity(Intent.createChooser(send, null))
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "#${entry.id}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            HorizontalDivider()
            if (loggedIn && entry.canEdit) {
                SheetItem(Icons.Filled.Edit, "düzenle") {
                    onDismiss()
                    onCompose(topic, entry.content, entry.id)
                }
            }
            if (loggedIn && entry.canDelete) {
                SheetItem(Icons.Filled.Delete, "sil") { showDeleteConfirm = true }
            }
            if (loggedIn) {
                SheetItem(Icons.Filled.Opacity, if (isFav) "favorilerden çıkar" else "favori") {
                    val remove = isFav
                    scope.launch {
                        val ok = runCatching { repository.favorite(entry.id, remove) }.getOrDefault(false)
                        if (ok) isFav = !remove
                        toast(if (ok) (if (remove) "favoriden çıkarıldı" else "favorilendi") else "başarısız")
                    }
                    onDismiss()
                }
                SheetItem(Icons.Filled.KeyboardArrowUp, "artı oy") {
                    scope.launch {
                        val ok = runCatching { repository.vote(entry.id, entry.authorId, "1") }.getOrDefault(false)
                        toast(if (ok) "artı oy verildi" else "başarısız")
                    }
                    onDismiss()
                }
                SheetItem(Icons.Filled.KeyboardArrowDown, "eksi oy") {
                    scope.launch {
                        val ok = runCatching { repository.vote(entry.id, entry.authorId, "-1") }.getOrDefault(false)
                        toast(if (ok) "eksi oy verildi" else "başarısız")
                    }
                    onDismiss()
                }
                SheetItem(Icons.Filled.MailOutline, "mesaj yolla") { showCompose = true }
                SheetItem(Icons.Filled.Share, "paylaş") { share(); onDismiss() }
                SheetItem(Icons.Filled.AddCircle, if (isFollowed) "takibi bırak" else "takip et") {
                    val add = !isFollowed
                    scope.launch {
                        val ok = runCatching {
                            repository.toggleRelation(entry.authorId, Endpoints.REL_FOLLOW, add)
                        }.getOrDefault(false)
                        if (ok) RelationStore.update(entry.author, Endpoints.REL_FOLLOW, add)
                        toast(if (!ok) "başarısız" else if (add) "${entry.author} takip ediliyor" else "takip bırakıldı")
                    }
                    onDismiss()
                }
                SheetItem(Icons.Filled.Block, if (isBlocked) "engeli kaldır" else "engelle") {
                    val add = !isBlocked
                    scope.launch {
                        val ok = runCatching {
                            repository.toggleRelation(entry.authorId, Endpoints.REL_BLOCK, add)
                        }.getOrDefault(false)
                        if (ok) RelationStore.update(entry.author, Endpoints.REL_BLOCK, add)
                        toast(if (!ok) "başarısız" else if (add) "${entry.author} engellendi" else "engel kaldırıldı")
                    }
                    onDismiss()
                }
                SheetItem(Icons.Filled.Block, if (isTitleBlocked) "başlık engelini kaldır" else "başlıklarını engelle") {
                    val add = !isTitleBlocked
                    scope.launch {
                        val ok = runCatching {
                            repository.toggleRelation(entry.authorId, Endpoints.REL_BLOCK_TITLE, add)
                        }.getOrDefault(false)
                        if (ok) RelationStore.update(entry.author, Endpoints.REL_BLOCK_TITLE, add)
                        toast(if (!ok) "başarısız" else if (add) "başlıkları engellendi" else "başlık engeli kaldırıldı")
                    }
                    onDismiss()
                }
                SheetItem(Icons.Filled.Save, if (isSaved) "kayıttan çıkar" else "kaydet") {
                    val nowSaved = SavedStore.toggle(
                        SavedEntry(
                            id = entry.id,
                            author = entry.author,
                            date = entry.date,
                            content = entry.content,
                            topicTitle = topic.title,
                        ),
                    )
                    toast(if (nowSaved) "kaydedildi" else "kayıttan çıkarıldı")
                    onDismiss()
                }
            } else {
                SheetItem(Icons.Filled.Share, "paylaş") { share(); onDismiss() }
                SheetItem(Icons.Filled.OpenInBrowser, "tarayıcıda aç") {
                    context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(entryUrl)))
                    onDismiss()
                }
            }
        }
    }
}

@Composable
fun ComposeMessageDialog(
    target: String,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
    title: String = "mesaj yolla",
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$title: $target") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("mesajınız") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
        },
        confirmButton = {
            Button(
                onClick = { onSend(text.trim()) },
                enabled = text.isNotBlank(),
            ) { Text("gönder") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("vazgeç") }
        },
    )
}

@Composable
private fun SheetItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            modifier = Modifier.padding(start = 20.dp),
        )
    }
}
