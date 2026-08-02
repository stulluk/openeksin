package com.drejo.openeksin.ui.message

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
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.drejo.openeksin.R
import com.drejo.openeksin.data.model.ContentSegment
import com.drejo.openeksin.ui.entry.buildSegmentsSpannable
import com.drejo.openeksin.ui.entry.handleEntryLink
import com.drejo.openeksin.data.EksiRepository
import com.drejo.openeksin.data.model.Message
import com.drejo.openeksin.data.model.MessageThread
import com.drejo.openeksin.ui.theme.EksiPalette
import com.drejo.openeksin.ui.theme.LocalEkColors

private sealed interface InboxState {
    data object Loading : InboxState
    data class Success(val threads: List<MessageThread>) : InboxState
    data class Error(val message: String) : InboxState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(onBack: () -> Unit, onOpenThread: (MessageThread) -> Unit, reloadKey: Int = 0) {
    val repository = remember { EksiRepository() }
    val state by produceState<InboxState>(InboxState.Loading, reloadKey) {
        value = try {
            InboxState.Success(repository.messages())
        } catch (e: Exception) {
            InboxState.Error(e.message ?: "error")
        }
    }

    Scaffold(topBar = { MessageTopBar(title = "mesajlar", onBack = onBack) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is InboxState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is InboxState.Error ->
                    Text(s.message, modifier = Modifier.align(Alignment.Center).padding(24.dp))

                is InboxState.Success ->
                    if (s.threads.isEmpty()) {
                        Text("mesaj yok", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(s.threads, key = { it.threadId }) { thread ->
                                ThreadRow(thread, onClick = { onOpenThread(thread) })
                                HorizontalDivider(color = LocalEkColors.current.divider)
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun ThreadRow(thread: MessageThread, onClick: () -> Unit) {
    val ek = LocalEkColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (thread.unreadCount.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .widthIn(min = 36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ek.rankBadge)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = thread.unreadCount,
                    color = ek.rankBadgeText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(thread.nick, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ek.mainText)
            if (thread.preview.isNotEmpty()) {
                Text(
                    text = thread.preview,
                    fontSize = 13.sp,
                    color = ek.secondaryText,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (thread.date.isNotEmpty()) {
            Text(
                text = thread.date,
                fontSize = 11.sp,
                color = ek.secondaryText,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageThreadScreen(
    thread: MessageThread,
    onBack: () -> Unit,
    onOpenLink: (String, String) -> Unit,
    onDeleted: () -> Unit = onBack,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { EksiRepository() }
    var refresh by remember { mutableStateOf(0) }
    var sending by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("konuşmayı sil") },
            text = { Text("${thread.nick} ile olan tüm mesajlaşma silinsin mi? bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        val ok = runCatching { repository.processThread(thread.link, "delete") }
                            .getOrDefault(false)
                        if (ok) {
                            Toast.makeText(context, "konuşma silindi", Toast.LENGTH_SHORT).show()
                            onDeleted()
                        } else {
                            Toast.makeText(context, "silinemedi", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("sil") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("vazgeç") } },
        )
    }
    val state by produceState<List<Message>?>(null, thread.link, refresh) {
        value = try {
            repository.messageThread(thread.link)
        } catch (e: Exception) {
            emptyList()
        }
    }

    Scaffold(
        topBar = {
            MessageTopBar(title = thread.nick, onBack = onBack, actions = {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "konuşmayı sil", tint = Color.White)
                }
            })
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val messages = state
                when {
                    messages == null ->
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                    messages.isEmpty() ->
                        Text("mesaj yok", modifier = Modifier.align(Alignment.Center))

                    else ->
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                        ) {
                            items(messages) { message ->
                                MessageBubble(
                                    message = message,
                                    onLinkClick = { href -> handleEntryLink(href, onOpenLink) },
                                )
                            }
                        }
                }
            }
            HorizontalDivider(color = LocalEkColors.current.divider)
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("mesaj yaz") },
                    modifier = Modifier.weight(1f),
                    enabled = !sending,
                    maxLines = 4,
                )
                IconButton(
                    onClick = {
                        val text = draft.trim()
                        if (text.isEmpty()) return@IconButton
                        sending = true
                        scope.launch {
                            val ok = runCatching { repository.sendReply(thread.link, text) }
                                .getOrDefault(false)
                            sending = false
                            if (ok) {
                                draft = ""
                                refresh++
                            } else {
                                Toast.makeText(context, "gönderilemedi", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !sending && draft.isNotBlank(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "gönder")
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message, onLinkClick: (String) -> Unit) {
    val ek = LocalEkColors.current
    val bubbleColor = if (message.incoming) ek.divider else EksiPalette.RankBadge
    val textColor = if (message.incoming) ek.mainText else Color.White
    val segments = message.segments.ifEmpty { listOf(ContentSegment(message.text)) }
    val hasLinks = segments.any { !it.href.isNullOrEmpty() }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.incoming) Arrangement.Start else Arrangement.End,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (hasLinks) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx ->
                        TextView(ctx).apply {
                            textSize = 14f
                            movementMethod = LinkMovementMethod.getInstance()
                            setTextIsSelectable(false)
                        }
                    },
                    update = { tv ->
                        tv.setTextColor(textColor.toArgb())
                        tv.setTag(R.id.tag_entry_link_click, onLinkClick)
                        tv.text = if (message.incoming) {
                            buildSegmentsSpannable(segments)
                        } else {
                            buildSegmentsSpannable(
                                segments,
                                internalLinkColor = Color.White.toArgb(),
                                externalLinkColor = 0xFFE8F4FF.toInt(),
                            )
                        }
                    },
                )
            } else {
                Text(message.text, fontSize = 14.sp, color = textColor)
            }
            if (message.date.isNotEmpty()) {
                Text(
                    text = message.date,
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "geri", tint = Color.White)
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = EksiPalette.Toolbar,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
        ),
    )
}
