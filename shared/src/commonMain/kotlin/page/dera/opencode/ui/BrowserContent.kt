package page.dera.opencode.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import page.dera.opencode.api.PortScannerApi
import page.dera.opencode.settings.AppSettings

@Composable
fun BrowserContent(settings: AppSettings) {
    val endpoint = settings.portScannerEndpoint
    val password = settings.serverPassword
    val urlPattern = settings.urlPattern

    if (endpoint.isNullOrBlank()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Port Scanner not configured",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Set the endpoint in Settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    var ports by remember { mutableStateOf<List<Int>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedPort by remember { mutableIntStateOf(-1) }

    LaunchedEffect(endpoint, password) {
        val client = HttpClient()
        val api = PortScannerApi(client, endpoint.trimEnd('/'), password)
        try {
            while (true) {
                api.listPorts()
                    .onSuccess { ports = it; error = null }
                    .onFailure { error = it.message }
                delay(3000)
            }
        } finally {
            client.close()
        }
    }

    if (selectedPort > 0) {
        val url = urlPattern.replace("<port>", selectedPort.toString())
        Box(Modifier.fillMaxSize()) {
            WebViewContent(url)
        }
    } else {
        val portList = ports
        when {
            portList == null && error == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Connection error",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            portList != null && portList.size == 1 -> {
                val port = portList.first()
                val url = urlPattern.replace("<port>", port.toString())
                selectedPort = port
                Box(Modifier.fillMaxSize()) {
                    WebViewContent(url)
                }
            }
            portList != null && portList.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No open ports detected",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Start a server and it will appear here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            portList != null -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                ) {
                    item {
                        Text(
                            "Select a port",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    items(portList) { port ->
                        val url = urlPattern.replace("<port>", port.toString())
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedPort = port }
                                .padding(12.dp),
                        ) {
                            Text(
                                "Port $port",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
expect fun WebViewContent(url: String)
