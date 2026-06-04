package com.drejo.openeksin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drejo.openeksin.data.EksiRepository
import com.drejo.openeksin.data.Feed
import com.drejo.openeksin.data.Feeds
import com.drejo.openeksin.data.SessionManager
import com.drejo.openeksin.data.local.RelationStore
import com.drejo.openeksin.data.local.SavedStore
import com.drejo.openeksin.data.local.SettingsStore
import com.drejo.openeksin.data.local.ThemeMode
import com.drejo.openeksin.data.model.MessageThread
import com.drejo.openeksin.data.model.Topic
import com.drejo.openeksin.data.remote.CloudflareActivity
import com.drejo.openeksin.data.remote.Endpoints
import com.drejo.openeksin.data.remote.LoginActivity
import com.drejo.openeksin.ui.entry.EntryListScreen
import com.drejo.openeksin.ui.message.MessageThreadScreen
import com.drejo.openeksin.ui.message.MessagesScreen
import com.drejo.openeksin.ui.misc.ArchiveScreen
import com.drejo.openeksin.ui.misc.SearchScreen
import com.drejo.openeksin.ui.misc.SettingsScreen
import com.drejo.openeksin.ui.theme.EksiPalette
import com.drejo.openeksin.ui.theme.OpeneksinTheme
import com.drejo.openeksin.ui.topic.FeedPage
import com.drejo.openeksin.ui.topic.StandaloneFeedScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingsStore.init(applicationContext)
        SavedStore.init(applicationContext)
        RelationStore.init(applicationContext)
        setContent {
            val themeMode by SettingsStore.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            OpeneksinTheme(darkTheme = darkTheme) {
                AppRoot()
            }
        }
    }
}

private sealed interface Screen {
    data object Home : Screen
    data class Entries(val topic: Topic) : Screen
    data object Messages : Screen
    data class Thread(val thread: MessageThread) : Screen
    data class FeedScreen(val feed: Feed) : Screen
    data object Search : Screen
    data object Archive : Screen
    data object Settings : Screen
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    fun navigate(screen: Screen) { backStack.add(screen) }
    fun back() { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var messagesReload by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { SessionManager.refresh() }

    val cloudflareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) reloadKey++
    }
    val loginLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            scope.launch {
                SessionManager.refresh()
                reloadKey++
            }
        }
    }

    val onVerifyCloudflare: (String) -> Unit = { url ->
        cloudflareLauncher.launch(
            Intent(context, CloudflareActivity::class.java)
                .putExtra(CloudflareActivity.EXTRA_URL, url),
        )
    }
    val onTopicClick: (Topic) -> Unit = { navigate(Screen.Entries(it)) }
    val onOpenLink: (String, String) -> Unit = { href, title ->
        val isTopic = href.startsWith("/") && !href.startsWith("//") && !href.startsWith("/?")
        if (isTopic) {
            navigate(Screen.Entries(Topic(title = title, link = href, entryCount = "")))
        } else {
            val url = if (href.startsWith("http")) href else Endpoints.BASE + href
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    BackHandler(enabled = backStack.size > 1) { back() }

    when (val current = backStack.last()) {
        Screen.Home -> HomeScreen(
            reloadKey = reloadKey,
            onVerifyCloudflare = onVerifyCloudflare,
            onTopicClick = onTopicClick,
            onLogin = { loginLauncher.launch(Intent(context, LoginActivity::class.java)) },
            onLogout = {
                SessionManager.logout()
                RelationStore.clear()
                reloadKey++
            },
            onOpenMessages = { navigate(Screen.Messages) },
            onOpenFeed = { feed -> navigate(Screen.FeedScreen(feed)) },
            onOpenSearch = { navigate(Screen.Search) },
            onOpenArchive = { navigate(Screen.Archive) },
            onOpenSettings = { navigate(Screen.Settings) },
        )

        is Screen.Entries -> EntryListScreen(
            topic = current.topic,
            onBack = { back() },
            onVerifyCloudflare = onVerifyCloudflare,
            onOpenLink = onOpenLink,
        )

        Screen.Messages -> MessagesScreen(
            onBack = { back() },
            onOpenThread = { navigate(Screen.Thread(it)) },
            reloadKey = messagesReload,
        )

        is Screen.Thread -> MessageThreadScreen(
            thread = current.thread,
            onBack = { back() },
            onDeleted = { messagesReload++; back() },
        )

        is Screen.FeedScreen -> StandaloneFeedScreen(
            feed = current.feed,
            onBack = { back() },
            onVerifyCloudflare = onVerifyCloudflare,
            onTopicClick = onTopicClick,
        )

        Screen.Search -> SearchScreen(onBack = { back() }, onOpenTopic = onTopicClick)

        Screen.Archive -> ArchiveScreen(onBack = { back() }, onOpenTopic = onTopicClick)

        Screen.Settings -> SettingsScreen(onBack = { back() })
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun HomeScreen(
    reloadKey: Int,
    onVerifyCloudflare: (String) -> Unit,
    onTopicClick: (Topic) -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenFeed: (Feed) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenArchive: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val nick by SessionManager.nick.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var channels by remember { mutableStateOf<List<Feed>>(emptyList()) }
    LaunchedEffect(Unit) {
        channels = try {
            EksiRepository().channels()
        } catch (e: Exception) {
            emptyList()
        }
    }
    val feeds = remember(channels) { Feeds.BUILTIN + channels }
    val pagerState = rememberPagerState(pageCount = { feeds.size })

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                nick = nick,
                onClose = { scope.launch { drawerState.close() } },
                onLogin = onLogin,
                onLogout = onLogout,
                onOpenMessages = onOpenMessages,
                onOpenFeed = onOpenFeed,
                onOpenSearch = onOpenSearch,
                onOpenArchive = onOpenArchive,
                onOpenSettings = onOpenSettings,
            )
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val title = feeds.getOrNull(pagerState.currentPage)?.title ?: "openeksin"
                        Text(title)
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "menü", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenSearch) {
                            Icon(Icons.Filled.Search, contentDescription = "ara", tint = Color.White)
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
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = EksiPalette.TabBar,
                    contentColor = EksiPalette.TabSelected,
                    edgePadding = 8.dp,
                    indicator = { positions ->
                        if (pagerState.currentPage < positions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(positions[pagerState.currentPage]),
                                color = EksiPalette.TabSelected,
                            )
                        }
                    },
                ) {
                    feeds.forEachIndexed { index, feed ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(feed.title, maxLines = 1) },
                            selectedContentColor = EksiPalette.TabSelected,
                            unselectedContentColor = EksiPalette.TabUnselected,
                        )
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    feeds.getOrNull(page)?.let { feed ->
                        FeedPage(
                            feed = feed,
                            reloadKey = reloadKey,
                            onVerifyCloudflare = onVerifyCloudflare,
                            onTopicClick = onTopicClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerContent(
    nick: String?,
    onClose: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenFeed: (Feed) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenArchive: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    ModalDrawerSheet(drawerContainerColor = EksiPalette.DrawerBackground) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "openeksin",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(24.dp),
            )
            HorizontalDivider(color = EksiPalette.DrawerSecondaryText.copy(alpha = 0.4f))

            DrawerSection("genel")
            DrawerItem(Icons.AutoMirrored.Filled.List, "başlıklar") { onClose() }
            DrawerItem(Icons.Filled.Search, "ara") { onClose(); onOpenSearch() }
            DrawerItem(Icons.Filled.Archive, "arşiv") { onClose(); onOpenArchive() }
            DrawerItem(Icons.Filled.Settings, "ayarlar") { onClose(); onOpenSettings() }

            HorizontalDivider(
                color = EksiPalette.DrawerSecondaryText.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 8.dp),
            )
            DrawerSection("yazar")
            if (nick == null) {
                DrawerItem(Icons.Filled.Person, "giriş") { onClose(); onLogin() }
            } else {
                DrawerItem(Icons.Filled.Person, nick) { onClose() }
                DrawerItem(Icons.Filled.MailOutline, "mesajlar") { onClose(); onOpenMessages() }
                DrawerItem(Icons.Filled.DateRange, "olaylar") {
                    onClose(); onOpenFeed(Feed("olaylar", Endpoints.EVENTS))
                }
                DrawerItem(Icons.AutoMirrored.Filled.ArrowForward, "çıkış") { onClose(); onLogout() }
            }
        }
    }
}

@Composable
private fun DrawerSection(label: String) {
    Text(
        text = label,
        color = EksiPalette.DrawerSecondaryText,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun DrawerItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(20.dp))
        Text(label, color = Color.White, fontSize = 16.sp)
    }
}
