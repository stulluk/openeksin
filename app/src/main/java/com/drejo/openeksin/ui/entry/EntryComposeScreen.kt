package com.drejo.openeksin.ui.entry

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.drejo.openeksin.data.EksiRepository
import com.drejo.openeksin.data.SessionManager
import com.drejo.openeksin.data.model.Topic
import com.drejo.openeksin.ui.compose.ComposeFormatBar
import com.drejo.openeksin.ui.compose.ComposeFormatKind
import com.drejo.openeksin.ui.compose.applyComposeFormat
import com.drejo.openeksin.ui.theme.EksiPalette
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryComposeScreen(
    topic: Topic,
    draft: String = "",
    entryId: String? = null,
    onBack: () -> Unit,
    onPosted: () -> Unit,
    onLogin: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { EksiRepository() }
    val scope = rememberCoroutineScope()
    val editing = entryId != null
    var body by remember(draft, entryId) { mutableStateOf(TextFieldValue(draft)) }
    var sending by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    fun sendEntry() {
        val text = body.text.trim()
        if (text.isBlank() || sending) return
        if (!SessionManager.isLoggedIn) {
            onLogin()
            return
        }
        sending = true
        scope.launch {
            val result = if (editing) {
                repository.editEntry(entryId!!, text).map { }
            } else {
                repository.addEntry(topic.link, text).map { }
            }
            result
                .onSuccess {
                    val msg = if (editing) "entry güncellendi" else "entry gönderildi"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    onPosted()
                }
                .onFailure {
                    val msg = when (it.message) {
                        "not_logged_in" -> "giriş yapmanız gerekiyor"
                        "not_allowed" -> "düzenleme izni yok"
                        else -> if (editing) "güncellenemedi" else "entry gönderilemedi"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (it.message == "not_logged_in") onLogin()
                    sending = false
                }
        }
    }

    fun saveDraft() {
        val text = body.text
        if (text.isBlank() || saving || editing) return
        if (!SessionManager.isLoggedIn) {
            onLogin()
            return
        }
        saving = true
        scope.launch {
            val ok = repository.saveDraft(topic.link, topic.title, text)
            saving = false
            val msg = if (ok) "taslak kaydedildi" else "taslak kaydedilemedi"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yaz") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "geri", tint = Color.White)
                    }
                },
                actions = {
                    if (!editing) {
                        IconButton(
                            enabled = !saving && body.text.isNotBlank(),
                            onClick = { saveDraft() },
                        ) {
                            if (saving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(8.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                            } else {
                                Icon(Icons.Filled.Save, "kaydet", tint = Color.White)
                            }
                        }
                    }
                    IconButton(
                        enabled = !sending && body.text.isNotBlank(),
                        onClick = { sendEntry() },
                    ) {
                        if (sending) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(8.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, "gönder", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EksiPalette.Toolbar,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            Text(
                text = topic.title.trimEnd(),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EksiPalette.TabBackground)
                    .padding(10.dp),
                color = EksiPalette.TabText,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
            ComposeFormatBar(
                onFormat = { kind ->
                    body = applyComposeFormat(kind, body)
                },
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(10.dp),
            ) {
                BasicTextField(
                    value = body,
                    onValueChange = { body = it },
                    modifier = Modifier.fillMaxSize(),
                    enabled = !sending && !saving,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    cursorBrush = SolidColor(EksiPalette.Blue),
                )
                if (body.text.isEmpty()) {
                    Text(
                        text = "Entry içeriğini buraya yazın…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        modifier = Modifier.align(Alignment.TopStart),
                    )
                }
            }
        }
    }
}
