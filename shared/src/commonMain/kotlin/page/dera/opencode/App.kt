package page.dera.opencode

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import page.dera.opencode.api.OpencodeApi
import page.dera.opencode.api.SessionInfo
import page.dera.opencode.api.ValidationResult
import page.dera.opencode.settings.AppSettings
import page.dera.opencode.ui.StarredScreen
import page.dera.opencode.ui.FileExplorerScreen
import page.dera.opencode.ui.ProjectScreen
import page.dera.opencode.ui.SessionDetailScreen
import page.dera.opencode.ui.WelcomeScreen
import page.dera.opencode.ui.BackHandler

private sealed class Screen {
    data class Loading(val api: OpencodeApi) : Screen()
    data class Starred(val api: OpencodeApi) : Screen()
    data class Explorer(val api: OpencodeApi) : Screen()
    data class ProjectDetail(val api: OpencodeApi, val directory: String, val name: String) : Screen()
    data class SessionDetail(val api: OpencodeApi, val directory: String, val projectName: String, val session: SessionInfo) : Screen()
    data class Welcome(val api: OpencodeApi) : Screen()
}

@Composable
@Preview
fun App() {
    val settings = remember { AppSettings() }
    val serverUrl = remember { OpencodeApi.normalizeUrl(settings.serverUrl ?: "") }
    val password = remember { settings.serverPassword }
    val api = remember { OpencodeApi(HttpClient { install(HttpTimeout) { requestTimeoutMillis = 300_000 } }, serverUrl, password) }

    var screen: Screen by remember {
        mutableStateOf(
            if (settings.isConfigured) Screen.Loading(api)
            else Screen.Welcome(api)
        )
    }

    LaunchedEffect(screen) {
        if (screen is Screen.Loading) {
            val result = api.validate()
            screen = when (result) {
                is ValidationResult.Success -> {
                    if (settings.pinnedFolders.isEmpty()) Screen.Explorer(api)
                    else Screen.Starred(api)
                }
                is ValidationResult.Error -> Screen.Welcome(api)
            }
        }
    }

    MaterialTheme {
        when (val current = screen) {
            is Screen.Loading -> {}
            is Screen.Welcome -> {
                WelcomeScreen(
                    initialUrl = serverUrl,
                    initialPassword = password ?: "",
                    onConfigured = { url, pwd ->
                        settings.serverUrl = url
                        settings.serverPassword = pwd
                        val newApi = OpencodeApi(HttpClient { install(HttpTimeout) { requestTimeoutMillis = 300_000 } }, OpencodeApi.normalizeUrl(url), pwd)
                        screen = Screen.Starred(newApi)
                    },
                )
            }
            is Screen.Starred -> {
                StarredScreen(
                    settings = settings,
                    onBrowseFiles = { screen = Screen.Explorer(current.api) },
                    onOpenFolder = { path ->
                        val name = path.trimEnd('/').split('/').last().ifEmpty { path }
                        screen = Screen.ProjectDetail(current.api, path, name)
                    },
                )
            }
            is Screen.Explorer -> {
                BackHandler { screen = Screen.Starred(current.api) }
                FileExplorerScreen(
                    api = current.api,
                    settings = settings,
                    onOpenProject = { directory ->
                        val name = directory.trimEnd('/').split('/').last().ifEmpty { directory }
                        screen = Screen.ProjectDetail(current.api, directory, name)
                    },
                )
            }
            is Screen.ProjectDetail -> {
                BackHandler { screen = Screen.Starred(current.api) }
                ProjectScreen(
                    api = current.api,
                    directory = current.directory,
                    name = current.name,
                    settings = settings,
                    onBack = { screen = Screen.Starred(current.api) },
                    onSessionClick = { session ->
                        screen = Screen.SessionDetail(current.api, current.directory, current.name, session)
                    },
                )
            }
            is Screen.SessionDetail -> {
                BackHandler { screen = Screen.ProjectDetail(current.api, current.directory, current.projectName) }
                SessionDetailScreen(
                    api = current.api,
                    sessionId = current.session.id,
                    sessionTitle = current.session.title,
                    directory = current.directory,
                    onBack = { screen = Screen.ProjectDetail(current.api, current.directory, current.projectName) },
                )
            }
        }
    }
}
