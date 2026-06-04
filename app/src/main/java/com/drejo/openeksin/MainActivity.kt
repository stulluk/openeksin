package com.drejo.openeksin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.drejo.openeksin.data.SessionManager
import com.drejo.openeksin.data.TopicFeed
import com.drejo.openeksin.data.model.Topic
import com.drejo.openeksin.data.remote.CloudflareActivity
import com.drejo.openeksin.data.remote.LoginActivity
import com.drejo.openeksin.ui.entry.EntryListScreen
import com.drejo.openeksin.ui.theme.EksiPalette
import com.drejo.openeksin.ui.theme.OpeneksinTheme
import com.drejo.openeksin.ui.topic.TopicListScreen
import com.drejo.openeksin.ui.topic.TopicListViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: TopicListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restore any existing session (cookies persist across launches).
        lifecycleScope.launch { SessionManager.refresh() }

        val cloudflareLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                viewModel.load()
            }
        }

        val loginLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                lifecycleScope.launch {
                    SessionManager.refresh()
                    viewModel.load()
                }
            }
        }

        setContent {
            OpeneksinTheme {
                MainScreen(
                    viewModel = viewModel,
                    onVerifyCloudflare = { url ->
                        cloudflareLauncher.launch(
                            Intent(this, CloudflareActivity::class.java)
                                .putExtra(CloudflareActivity.EXTRA_URL, url),
                        )
                    },
                    onLogin = {
                        loginLauncher.launch(Intent(this, LoginActivity::class.java))
                    },
                    onLogout = {
                        SessionManager.logout()
                        viewModel.load()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    viewModel: TopicListViewModel,
    onVerifyCloudflare: (String) -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    var selectedTopic by remember { mutableStateOf<Topic?>(null) }

    val current = selectedTopic
    if (current != null) {
        BackHandler { selectedTopic = null }
        EntryListScreen(
            topic = current,
            onBack = { selectedTopic = null },
            onVerifyCloudflare = onVerifyCloudflare,
        )
        return
    }

    val state by viewModel.state.collectAsState()
    val feed by viewModel.feed.collectAsState()
    val nick by SessionManager.nick.collectAsState()

    val tabs = listOf(
        TopicFeed.AGENDA to "gündem",
        TopicFeed.DEBE to "debe",
        TopicFeed.TODAY to "bugün",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("openeksin") },
                actions = {
                    val currentNick = nick
                    if (currentNick == null) {
                        TextButton(onClick = onLogin) {
                            Text("giriş", color = Color.White)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentNick,
                                color = Color.White,
                                modifier = Modifier.padding(end = 4.dp),
                            )
                            TextButton(onClick = onLogout) {
                                Text("çıkış", color = Color.White)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EksiPalette.Toolbar,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            val selectedIndex = tabs.indexOfFirst { it.first == feed }
            TabRow(
                selectedTabIndex = selectedIndex,
                containerColor = EksiPalette.TabBar,
                contentColor = EksiPalette.TabSelected,
                indicator = { positions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(positions[selectedIndex]),
                        color = EksiPalette.TabSelected,
                    )
                },
            ) {
                tabs.forEach { (tabFeed, label) ->
                    Tab(
                        selected = feed == tabFeed,
                        onClick = { viewModel.selectFeed(tabFeed) },
                        text = { Text(label) },
                        selectedContentColor = EksiPalette.TabSelected,
                        unselectedContentColor = EksiPalette.TabUnselected,
                    )
                }
            }
            TopicListScreen(
                state = state,
                onRetry = viewModel::load,
                onVerifyCloudflare = onVerifyCloudflare,
                onTopicClick = { selectedTopic = it },
            )
        }
    }
}
