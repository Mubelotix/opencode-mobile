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
import page.dera.opencode.api.ValidationResult
import page.dera.opencode.settings.AppSettings
import page.dera.opencode.ui.HomeScreen
import page.dera.opencode.ui.WelcomeScreen

@Composable
@Preview
fun App() {
    val settings = remember { AppSettings() }
    val api = remember { OpencodeApi(HttpClient()) }

    var showHome by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf(settings.serverUrl ?: "") }
    var serverPassword by remember { mutableStateOf(settings.serverPassword ?: "") }

    LaunchedEffect(Unit) {
        if (settings.isConfigured) {
            val result = api.validate(serverUrl, serverPassword.takeIf { it.isNotBlank() })
            if (result is ValidationResult.Success) {
                showHome = true
            }
        }
        isInitialized = true
    }

    MaterialTheme {
        if (!isInitialized) {
            HomeScreen()
        } else if (showHome) {
            HomeScreen()
        } else {
            WelcomeScreen(
                api = api,
                initialUrl = serverUrl,
                initialPassword = serverPassword,
                onConfigured = { url, password ->
                    settings.serverUrl = url
                    settings.serverPassword = password
                    serverUrl = url
                    serverPassword = password
                    showHome = true
                },
            )
        }
    }
}
