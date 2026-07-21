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
import page.dera.opencode.api.OpencodeApi
import page.dera.opencode.api.ProjectInfo
import page.dera.opencode.api.SessionInfo
import page.dera.opencode.api.ValidationResult
import page.dera.opencode.settings.AppSettings
import page.dera.opencode.ui.HomeScreen
import page.dera.opencode.ui.ProjectScreen
import page.dera.opencode.ui.SessionDetailScreen
import page.dera.opencode.ui.WelcomeScreen
import page.dera.opencode.ui.BackHandler

private sealed class Screen {
    data class Loading(val api: OpencodeApi) : Screen()
    data class ProjectList(val api: OpencodeApi) : Screen()
    data class ProjectDetail(val api: OpencodeApi, val project: ProjectInfo) : Screen()
    data class SessionDetail(val api: OpencodeApi, val project: ProjectInfo, val session: SessionInfo) : Screen()
    data class Welcome(val api: OpencodeApi) : Screen()
}

@Composable
@Preview
fun App() {
    val settings = remember { AppSettings() }
    val serverUrl = remember { OpencodeApi.normalizeUrl(settings.serverUrl ?: "") }
    val password = remember { settings.serverPassword }
    val api = remember { OpencodeApi(HttpClient(), serverUrl, password) }

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
                is ValidationResult.Success -> Screen.ProjectList(api)
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
                        val newApi = OpencodeApi(HttpClient(), OpencodeApi.normalizeUrl(url), pwd)
                        screen = Screen.ProjectList(newApi)
                    },
                )
            }
            is Screen.ProjectList -> {
                HomeScreen(
                    api = current.api,
                    onProjectClick = { project ->
                        screen = Screen.ProjectDetail(current.api, project)
                    },
                )
            }
            is Screen.ProjectDetail -> {
                BackHandler { screen = Screen.ProjectList(current.api) }
                ProjectScreen(
                    api = current.api,
                    project = current.project,
                    onBack = { screen = Screen.ProjectList(current.api) },
                    onSessionClick = { session ->
                        screen = Screen.SessionDetail(current.api, current.project, session)
                    },
                )
            }
            is Screen.SessionDetail -> {
                BackHandler { screen = Screen.ProjectDetail(current.api, current.project) }
                SessionDetailScreen(
                    sessionId = current.session.id,
                    sessionTitle = current.session.title,
                    onBack = { screen = Screen.ProjectDetail(current.api, current.project) },
                )
            }
        }
    }
}
