package com.drejo.openeksin.ui.misc

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drejo.openeksin.data.EksiRepository
import com.drejo.openeksin.data.local.SavedEntry
import com.drejo.openeksin.data.local.SavedStore
import com.drejo.openeksin.data.local.SettingsStore
import com.drejo.openeksin.data.local.ThemeMode
import com.drejo.openeksin.data.model.Topic
import com.drejo.openeksin.ui.theme.EksiPalette
import com.drejo.openeksin.ui.theme.LocalEkColors
import kotlinx.coroutines.delay
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtraTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onBack: () -> Unit, onOpenTopic: (Topic) -> Unit) {
    val ek = LocalEkColors.current
    val repository = remember { EksiRepository() }
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            suggestions = emptyList()
        } else {
            delay(250)
            suggestions = runCatching { repository.searchTitles(query) }.getOrDefault(emptyList())
        }
    }

    fun open(title: String) {
        val link = "/?q=" + URLEncoder.encode(title, "UTF-8")
        onOpenTopic(Topic(title = title, link = link, entryCount = ""))
    }

    Scaffold(topBar = { ExtraTopBar("ara", onBack) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("başlık ara") },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                singleLine = true,
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (query.isNotBlank()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { open(query.trim()) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            Text("\"${query.trim()}\" başlığına git", color = EksiPalette.Blue, fontSize = 15.sp)
                        }
                        HorizontalDivider(color = ek.divider)
                    }
                }
                items(suggestions) { title ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { open(title) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Text(title, color = ek.mainText, fontSize = 15.sp)
                    }
                    HorizontalDivider(color = ek.divider)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ek = LocalEkColors.current
    val mode by SettingsStore.themeMode.collectAsState()

    Scaffold(topBar = { ExtraTopBar("ayarlar", onBack) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                text = "tema",
                color = ek.secondaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
            )
            ThemeOption("sistem varsayılanı", ThemeMode.SYSTEM, mode)
            ThemeOption("açık", ThemeMode.LIGHT, mode)
            ThemeOption("koyu", ThemeMode.DARK, mode)
        }
    }
}

@Composable
private fun ThemeOption(label: String, value: ThemeMode, current: ThemeMode) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { SettingsStore.setThemeMode(value) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = value == current, onClick = { SettingsStore.setThemeMode(value) })
        Text(label, color = LocalEkColors.current.mainText, fontSize = 16.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(onBack: () -> Unit, onOpenTopic: (Topic) -> Unit) {
    val ek = LocalEkColors.current
    val entries by SavedStore.entries.collectAsState()

    Scaffold(topBar = { ExtraTopBar("arşiv", onBack) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (entries.isEmpty()) {
                Text("kayıtlı entry yok", modifier = Modifier.align(Alignment.Center), color = ek.secondaryText)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(entries, key = { it.id }) { e ->
                        SavedRow(e, onOpenTopic)
                        HorizontalDivider(color = ek.divider, thickness = 6.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedRow(entry: SavedEntry, onOpenTopic: (Topic) -> Unit) {
    val ek = LocalEkColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = entry.topicTitle.ifBlank { "#${entry.id}" },
                color = EksiPalette.Blue,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        onOpenTopic(Topic(title = entry.topicTitle, link = "/entry/${entry.id}", entryCount = ""))
                    },
            )
            IconButton(onClick = { SavedStore.toggle(entry) }) {
                Icon(Icons.Filled.Delete, contentDescription = "sil", tint = ek.secondaryText, modifier = Modifier.size(20.dp))
            }
        }
        Text(entry.content, color = ek.mainText, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        Text(
            text = "${entry.author}  ${entry.date}",
            color = ek.secondaryText,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
