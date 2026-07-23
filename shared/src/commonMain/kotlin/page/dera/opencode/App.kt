package page.dera.opencode

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.launch
import opencodeapp.shared.generated.resources.menuIcon
import opencodeapp.shared.generated.resources.settingsIcon
import opencodeapp.shared.generated.resources.xIcon
import org.jetbrains.compose.resources.painterResource
import page.dera.opencode.api.OpencodeApi
import page.dera.opencode.api.ValidationResult
import page.dera.opencode.settings.AppSettings
import page.dera.opencode.ui.FileExplorerScreen
import page.dera.opencode.ui.ProjectScreen
import page.dera.opencode.ui.SessionDetailScreen
import page.dera.opencode.ui.StarredScreen
import page.dera.opencode.ui.WelcomeScreen
import page.dera.opencode.ui.SettingsScreen
import page.dera.opencode.ui.BackHandler

private sealed class Screen {
    data class Loading(val api: OpencodeApi) : Screen()
    data class Starred(val api: OpencodeApi) : Screen()
    data class Explorer(val api: OpencodeApi) : Screen()
    data class Welcome(val api: OpencodeApi) : Screen()
    data class Settings(val api: OpencodeApi) : Screen()
}

@Composable
fun App() {
    val settings = remember { AppSettings() }
    val serverUrl = remember { OpencodeApi.normalizeUrl(settings.serverUrl ?: "") }
    val password = remember { settings.serverPassword }
    val api = remember { OpencodeApi(HttpClient { install(HttpTimeout) { requestTimeoutMillis = 300_000 } }, serverUrl, password) }

    var tabs by remember { mutableStateOf(listOf<Tab>()) }
    var activeTabId by remember { mutableStateOf<String?>(null) }
    var tabCounter by remember { mutableStateOf(0) }

    var landingScreen: Screen by remember {
        mutableStateOf(
            if (settings.isConfigured) Screen.Loading(api)
            else Screen.Welcome(api)
        )
    }

    LaunchedEffect(landingScreen) {
        if (landingScreen is Screen.Loading) {
            val result = api.validate()
            landingScreen = when (result) {
                is ValidationResult.Success -> Screen.Starred(api)
                is ValidationResult.Error -> Screen.Welcome(api)
            }
        }
    }

    val activeTab = tabs.find { it.id == activeTabId }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun openProject(api: OpencodeApi, directory: String, name: String, currentTabId: String?) {
        val targetTab = currentTabId?.let { tabs.find { t -> t.id == it } }
        if (targetTab != null) {
            tabs = tabs.map { if (it.id == targetTab.id) it.copy(screen = TabScreen.ProjectDetail(api, directory, name)) else it }
        } else {
            val id = "tab_${tabCounter++}"
            tabs = tabs + Tab(id = id, screen = TabScreen.ProjectDetail(api, directory, name))
            activeTabId = id
        }
    }

    MaterialTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Column(Modifier.fillMaxHeight()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Sessions",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                        )
                        if (tabs.isEmpty()) {
                            Text(
                                "No open tabs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                            )
                        }
                        LazyColumn {
                            items(tabs, key = { it.id }) { tab ->
                                val isActive = tab.id == activeTabId
                                NavigationDrawerItem(
                                    label = {
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Text(tab.title(), modifier = Modifier.weight(1f))
                                            IconButton(onClick = {
                                                tabs = tabs.filter { t -> t.id != tab.id }
                                                if (activeTabId == tab.id) {
                                                    activeTabId = tabs.firstOrNull()?.id
                                                }
                                            }) {
                                                Image(
                                                    painter = painterResource(xIcon),
                                                    contentDescription = "Close tab",
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        }
                                    },
                                    selected = isActive,
                                    onClick = {
                                        activeTabId = tab.id
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                activeTabId = null
                                landingScreen = Screen.Starred(api)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 20.dp),
                        ) {
                            Text("+ Open Session")
                        }
                        Spacer(Modifier.weight(1f))
                        NavigationDrawerItem(
                            label = { Text("Settings") },
                            selected = false,
                            onClick = {
                                activeTabId = null
                                landingScreen = Screen.Settings(api)
                                scope.launch { drawerState.close() }
                            },
                            icon = {
                                Image(
                                    painter = painterResource(settingsIcon),
                                    contentDescription = "Settings",
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        )
                    }
                }
            },
        ) {
            if (activeTab != null) {
                TabContent(
                    tab = activeTab,
                    settings = settings,
                    onUpdateTab = { updated -> tabs = tabs.map { if (it.id == updated.id) updated else it } },
                    onCloseTab = {
                        tabs = tabs.filter { t -> t.id != activeTab.id }
                        activeTabId = tabs.firstOrNull()?.id
                    },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onOpenProject = { a, dir, name -> openProject(a, dir, name, activeTab.id) },
                )
            } else {
                LandingContent(
                    screen = landingScreen,
                    settings = settings,
                    onNavigate = { landingScreen = it },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onOpenProject = { a, dir, name -> openProject(a, dir, name, null) },
                )
            }
        }
    }
}

@Composable
private fun LandingContent(
    screen: Screen,
    settings: AppSettings,
    onNavigate: (Screen) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenProject: (OpencodeApi, String, String) -> Unit,
) {
    when (screen) {
        is Screen.Loading -> {}
        is Screen.Welcome -> {
            WelcomeScreen(
                initialUrl = "",
                initialPassword = "",
                onConfigured = { url, pwd ->
                    settings.serverUrl = url
                    settings.serverPassword = pwd
                    val newApi = OpencodeApi(HttpClient { install(HttpTimeout) { requestTimeoutMillis = 300_000 } }, OpencodeApi.normalizeUrl(url), pwd)
                    onNavigate(Screen.Starred(newApi))
                },
            )
        }
        is Screen.Starred -> {
            StarredScreen(
                settings = settings,
                onBrowseFiles = { onNavigate(Screen.Explorer(screen.api)) },
                onOpenFolder = { path ->
                    val name = path.trimEnd('/').split('/').last().ifEmpty { path }
                    onOpenProject(screen.api, path, name)
                },
                onMenuClick = onOpenDrawer,
            )
        }
        is Screen.Explorer -> {
            BackHandler { onNavigate(Screen.Starred(screen.api)) }
            FileExplorerScreen(
                api = screen.api,
                settings = settings,
                onOpenProject = { directory ->
                    val name = directory.trimEnd('/').split('/').last().ifEmpty { directory }
                    onOpenProject(screen.api, directory, name)
                },
                onMenuClick = onOpenDrawer,
            )
        }
        is Screen.Settings -> {
            SettingsScreen(
                settings = settings,
                onDisconnect = {
                    settings.clear()
                    onNavigate(Screen.Welcome(screen.api))
                },
                onBack = { onNavigate(Screen.Starred(screen.api)) },
            )
        }
    }
}

@Composable
private fun TabContent(
    tab: Tab,
    settings: AppSettings,
    onUpdateTab: (Tab) -> Unit,
    onCloseTab: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenProject: (OpencodeApi, String, String) -> Unit,
) {
    when (val ts = tab.screen) {
        is TabScreen.ProjectSelection -> {
            StarredScreen(
                settings = settings,
                onBrowseFiles = { onUpdateTab(tab.copy(screen = TabScreen.Explorer(ts.api))) },
                onOpenFolder = { path ->
                    val name = path.trimEnd('/').split('/').last().ifEmpty { path }
                    onOpenProject(ts.api, path, name)
                },
                onMenuClick = onOpenDrawer,
            )
        }
        is TabScreen.Explorer -> {
            BackHandler { onUpdateTab(tab.copy(screen = TabScreen.ProjectSelection(ts.api))) }
            FileExplorerScreen(
                api = ts.api,
                settings = settings,
                onOpenProject = { directory ->
                    val name = directory.trimEnd('/').split('/').last().ifEmpty { directory }
                    onOpenProject(ts.api, directory, name)
                },
                onMenuClick = onOpenDrawer,
            )
        }
        is TabScreen.ProjectDetail -> {
            BackHandler { onUpdateTab(tab.copy(screen = TabScreen.ProjectSelection(ts.api))) }
            ProjectScreen(
                api = ts.api,
                directory = ts.directory,
                name = ts.name,
                settings = settings,
                onBack = { onUpdateTab(tab.copy(screen = TabScreen.ProjectSelection(ts.api))) },
                onSessionClick = { session ->
                    onUpdateTab(tab.copy(screen = TabScreen.SessionDetail(ts.api, ts.directory, ts.name, session)))
                },
                onMenuClick = onOpenDrawer,
            )
        }
        is TabScreen.SessionDetail -> {
            BackHandler { onUpdateTab(tab.copy(screen = TabScreen.ProjectDetail(ts.api, ts.directory, ts.projectName))) }
            SessionDetailScreen(
                api = ts.api,
                sessionId = ts.session.id,
                sessionTitle = ts.session.title,
                directory = ts.directory,
                projectRoot = ts.session.location.directory,
                onBack = { onUpdateTab(tab.copy(screen = TabScreen.ProjectDetail(ts.api, ts.directory, ts.projectName))) },
                onMenuClick = onOpenDrawer,
                onTitleChange = { newTitle ->
                    onUpdateTab(tab.copy(screen = TabScreen.SessionDetail(ts.api, ts.directory, ts.projectName, ts.session.copy(title = newTitle))))
                },
            )
        }
    }
}
