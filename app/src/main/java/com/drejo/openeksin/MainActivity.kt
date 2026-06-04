package com.drejo.openeksin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.drejo.openeksin.data.TopicFeed
import com.drejo.openeksin.data.remote.CloudflareActivity
import com.drejo.openeksin.data.remote.Endpoints
import com.drejo.openeksin.ui.theme.EksiPalette
import com.drejo.openeksin.ui.theme.OpeneksinTheme
import com.drejo.openeksin.ui.topic.TopicListScreen
import com.drejo.openeksin.ui.topic.TopicListViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TopicListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cloudflareLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                viewModel.load()
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
                    onOpenLink = { link ->
                        val url = if (link.startsWith("http")) link else Endpoints.BASE + link
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
    onOpenLink: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val feed by viewModel.feed.collectAsState()

    val tabs = listOf(
        TopicFeed.AGENDA to "gündem",
        TopicFeed.DEBE to "debe",
        TopicFeed.TODAY to "bugün",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("openeksin") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EksiPalette.TabBackground,
                    titleContentColor = Color.White,
                ),
            )
        },
    ) { innerPadding ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            TabRow(selectedTabIndex = tabs.indexOfFirst { it.first == feed }) {
                tabs.forEach { (tabFeed, label) ->
                    Tab(
                        selected = feed == tabFeed,
                        onClick = { viewModel.selectFeed(tabFeed) },
                        text = { Text(label) },
                    )
                }
            }
            TopicListScreen(
                state = state,
                onRetry = viewModel::load,
                onVerifyCloudflare = onVerifyCloudflare,
                onTopicClick = { onOpenLink(it.link) },
            )
        }
    }
}
