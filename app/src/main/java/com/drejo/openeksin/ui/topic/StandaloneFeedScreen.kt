package com.drejo.openeksin.ui.topic

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.drejo.openeksin.data.Feed
import com.drejo.openeksin.data.model.Topic
import com.drejo.openeksin.ui.theme.EksiPalette

/** A single feed with its own toolbar + back button (used for olaylar/başlıklar). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandaloneFeedScreen(
    feed: Feed,
    onBack: () -> Unit,
    onVerifyCloudflare: (String) -> Unit,
    onTopicClick: (Topic) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(feed.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
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
    ) { padding ->
        FeedPage(
            feed = feed,
            reloadKey = 0,
            onVerifyCloudflare = onVerifyCloudflare,
            onTopicClick = onTopicClick,
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}
