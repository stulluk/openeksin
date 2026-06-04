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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
import com.drejo.openeksin.ui.theme.EksiPalette
import com.drejo.openeksin.ui.theme.LocalEkColors
import com.drejo.openeksin.ui.theme.TextSizes
import kotlinx.coroutines.launch

private const val COLLAPSED_LINES = 8
private const val URL_TAG = "URL"

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
    onOpenLink: (href: String, title: String) -> Unit,
) {
    val repository = remember { EksiRepository() }
    var page by remember(topic.link) { mutableIntStateOf(1) }

    val state by produceState<EntryUiState>(EntryUiState.Loading, topic.link, page) {
        value = EntryUiState.Loading
        value = try {
            EntryUiState.Success(repository.entries(topic.link, page))
        } catch (e: CloudflareException) {
            EntryUiState.NeedsCloudflare(e.challengeUrl)
        } catch (e: Exception) {
            EntryUiState.Error(e.message ?: "error")
        }
    }

    val detail = (state as? EntryUiState.Success)?.detail

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = topic.title,
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = EksiPalette.Toolbar,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                    ),
                )
                if (detail != null && detail.pageCount > 1) {
                    PagerBar(
                        current = detail.currentPage,
                        count = detail.pageCount,
                        onPrev = { if (page > 1) page-- },
                        onNext = { if (page < detail.pageCount) page++ },
                        onLast = { page = detail.pageCount },
                    )
                }
            }
        },
    ) { innerPadding ->
        val listBg = if (androidx.compose.foundation.isSystemInDarkTheme()) {
            EksiPalette.DarkBackground
        } else {
            EksiPalette.LightEntryListBackground
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(listBg),
        ) {
            when (val s = state) {
                is EntryUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is EntryUiState.Success ->
                    if (s.detail.entries.isEmpty()) {
                        Text("entry bulunamadı", modifier = Modifier.align(Alignment.Center))
                    } else {
                        EntryList(s.detail.entries, topic.title, onOpenLink)
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
private fun PagerBar(
    current: Int,
    count: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onLast: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EksiPalette.TabBar)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
                    .clickable { /* page picker — swipe / future dialog */ }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        IconButton(
            onClick = onLast,
            enabled = current < count,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                Icons.Filled.LastPage,
                contentDescription = "son sayfa",
                tint = EksiPalette.TabSelected,
            )
        }
    }
}

@Composable
private fun EntryList(entries: List<Entry>, topicTitle: String, onOpenLink: (String, String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 4.dp, bottom = 8.dp),
    ) {
        items(entries, key = { it.id.ifEmpty { it.hashCode().toString() } }) { entry ->
            EntryRow(entry, topicTitle, onOpenLink)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryRow(entry: Entry, topicTitle: String, onOpenLink: (String, String) -> Unit) {
    val ek = LocalEkColors.current
    val dark = isSystemInDarkTheme()
    val cardBg = if (dark) EksiPalette.DarkSurface else EksiPalette.LightBackground
    var expanded by remember(entry.id) { mutableStateOf(false) }
    var overflow by remember(entry.id) { mutableStateOf(false) }
    var fullLines by remember(entry.id) { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }

    val annotated = remember(entry.id) { buildContent(entry) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 5.dp, end = 5.dp, bottom = 5.dp),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (entry.favoriteCount.isNotEmpty() && entry.favoriteCount != "0") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 30.dp)
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        entry.favoriteCount,
                        fontSize = TextSizes.EntryAuthor,
                        color = EksiPalette.LightSecondaryText,
                        fontFamily = FontFamily.SansSerif,
                    )
                    Icon(
                        imageVector = Icons.Filled.Opacity,
                        contentDescription = "favori",
                        tint = EksiPalette.LightSecondaryText,
                        modifier = Modifier.padding(start = 5.dp).size(20.dp),
                    )
                }
            }

            HorizontalDivider(color = ek.divider, thickness = 1.dp)

            // Hidden measure pass to learn the full line count.
            if (fullLines == 0) {
                ClickableText(
                    text = annotated,
                    onClick = {},
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = TextSizes.EntryBody,
                        color = ek.mainText,
                    ),
                    modifier = Modifier.fillMaxWidth().height(0.dp).clipToBounds().alpha(0f),
                    onTextLayout = { fullLines = it.lineCount },
                )
            }

            ClickableText(
                text = annotated,
                onClick = { offset ->
                    annotated.getStringAnnotations(URL_TAG, offset, offset).firstOrNull()?.let {
                        handleLink(it.item, onOpenLink)
                    }
                },
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = TextSizes.EntryBody,
                    color = ek.mainText,
                ),
                maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_LINES,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { result: TextLayoutResult ->
                    overflow = result.hasVisualOverflow
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
            )

            if (!expanded && (overflow || fullLines > COLLAPSED_LINES)) {
                val remaining = (fullLines - COLLAPSED_LINES).coerceAtLeast(1)
                HorizontalDivider(color = ek.divider, thickness = 1.dp)
                Text(
                    text = "devamını okuyayım… ($remaining satır)",
                    fontSize = TextSizes.EntryAuthor,
                    fontWeight = FontWeight.Bold,
                    color = EksiPalette.LightReadMore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true }
                        .padding(horizontal = 5.dp, vertical = 4.dp),
                )
            }

            HorizontalDivider(color = ek.divider, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 5.dp, end = 5.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.author,
                        fontSize = TextSizes.EntryAuthor,
                        fontWeight = FontWeight.Bold,
                        color = ek.mainText,
                    )
                    Text(
                        text = entry.date,
                        fontSize = TextSizes.EntryAuthor,
                        color = ek.mainText,
                    )
                }
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "menü",
                        tint = ek.secondaryText,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }
    }

    if (showMenu) {
        EntryMenuSheet(entry = entry, topicTitle = topicTitle, onDismiss = { showMenu = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryMenuSheet(entry: Entry, topicTitle: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
    val entryUrl = "${Endpoints.BASE}/entry/${entry.id}"

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    if (showCompose) {
        ComposeMessageDialog(
            target = entry.author,
            onDismiss = { showCompose = false },
            onSend = { text ->
                showCompose = false
                onDismiss()
                scope.launch {
                    val ok = runCatching { repository.sendNewMessage(entry.author, text, entry.id) }
                        .getOrDefault(false)
                    toast(if (ok) "mesaj gönderildi" else "gönderilemedi")
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
                            topicTitle = topicTitle,
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

private fun buildContent(entry: Entry): AnnotatedString = buildAnnotatedString {
    for (seg in entry.segments) {
        if (seg.href.isNullOrEmpty()) {
            append(seg.text)
        } else {
            pushStringAnnotation(URL_TAG, seg.href)
            val external = seg.href.startsWith("http")
            withStyle(
                SpanStyle(
                    color = if (external) Color(0xFF559CB4) else Color(0xFF177DB4),
                    fontWeight = if (external) FontWeight.Normal else FontWeight.Bold,
                    textDecoration = TextDecoration.None,
                ),
            ) {
                append(seg.text)
            }
            pop()
        }
    }
}

private fun handleLink(href: String, onOpenLink: (String, String) -> Unit) {
    onOpenLink(href, href.removePrefix("/").substringBefore("--").replace("-", " "))
}
