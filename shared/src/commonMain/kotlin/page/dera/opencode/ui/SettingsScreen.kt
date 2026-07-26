package page.dera.opencode.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import page.dera.opencode.api.ValidationResult
import page.dera.opencode.api.WakatimeApi
import page.dera.opencode.settings.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    wakatimeApi: WakatimeApi?,
    onDisconnect: () -> Unit,
    onBack: () -> Unit,
) {
    var serverUrl by remember { mutableStateOf(settings.serverUrl ?: "") }
    var password by remember { mutableStateOf(settings.serverPassword ?: "") }
    var passwordVisible by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    var wakatimeApiUrl by remember { mutableStateOf(settings.wakatimeApiUrl ?: WakatimeApi.DEFAULT_API_URL) }
    var wakatimeApiKey by remember { mutableStateOf(settings.wakatimeApiKey ?: "") }
    var wakatimeApiKeyVisible by remember { mutableStateOf(false) }
    var wakatimeSaved by remember { mutableStateOf(false) }
    var wakatimeValidating by remember { mutableStateOf(false) }
    var wakatimeValidationMessage by remember { mutableStateOf<String?>(null) }
    var wakatimeValidationIsError by remember { mutableStateOf(false) }
    var latestSessionsLimit by remember { mutableStateOf(settings.latestSessionsLimit.toString()) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Opencode",
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it; saved = false },
                label = { Text("Server Address") },
                placeholder = { Text("https://your-server.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; saved = false },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) "Hide" else "Show")
                    }
                },
            )

            Button(
                onClick = {
                    settings.serverUrl = serverUrl.ifBlank { null }
                    settings.serverPassword = password.ifBlank { null }
                    saved = true
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !saved,
            ) {
                Text(if (saved) "Saved" else "Save")
            }

            Spacer(Modifier.height(16.dp))

            HorizontalDivider()

            Text(
                text = "Display",
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = latestSessionsLimit,
                onValueChange = { value ->
                    val filtered = value.filter { it.isDigit() }
                    latestSessionsLimit = filtered
                    val num = filtered.toIntOrNull() ?: 0
                    settings.latestSessionsLimit = num
                },
                label = { Text("Latest sessions count") },
                singleLine = true,
                supportingText = { Text("Set to 0 to hide. Max 20.") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            Spacer(Modifier.height(16.dp))

            var expandDiffs by remember { mutableStateOf(settings.expandDiffsByDefault) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Expand diffs by default",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Always show diff blocks expanded in edit tool calls",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Switch(
                    checked = expandDiffs,
                    onCheckedChange = {
                        expandDiffs = it
                        settings.expandDiffsByDefault = it
                    },
                )
            }

            Spacer(Modifier.height(16.dp))

            HorizontalDivider()

            Spacer(Modifier.height(16.dp))

            Text(
                text = "WakaTime",
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = wakatimeApiUrl,
                onValueChange = { wakatimeApiUrl = it; wakatimeSaved = false },
                label = { Text("API Endpoint") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )

            OutlinedTextField(
                value = wakatimeApiKey,
                onValueChange = { wakatimeApiKey = it; wakatimeSaved = false },
                label = { Text("API Key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (wakatimeApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { wakatimeApiKeyVisible = !wakatimeApiKeyVisible }) {
                        Text(if (wakatimeApiKeyVisible) "Hide" else "Show")
                    }
                },
            )

            wakatimeValidationMessage?.let {
                Text(
                    text = it,
                    color = if (wakatimeValidationIsError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        settings.wakatimeApiUrl = wakatimeApiUrl.ifBlank { null }
                        settings.wakatimeApiKey = wakatimeApiKey.ifBlank { null }
                        wakatimeSaved = true
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    enabled = !wakatimeSaved,
                ) {
                    Text(if (wakatimeSaved) "Saved" else "Save")
                }

                Button(
                    onClick = {
                        wakatimeValidating = true
                        wakatimeValidationMessage = null
                        wakatimeValidationIsError = false
                        val api = WakatimeApi(
                            HttpClient(),
                            WakatimeApi.normalizeUrl(wakatimeApiUrl.ifBlank { WakatimeApi.DEFAULT_API_URL }),
                            wakatimeApiKey,
                        )
                        scope.launch {
                            val result = api.validate()
                            wakatimeValidationMessage = when (result) {
                                is ValidationResult.Success -> "Connected successfully"
                                is ValidationResult.Error -> result.message
                            }
                            wakatimeValidationIsError = result is ValidationResult.Error
                            wakatimeValidating = false
                        }
                    },
                    modifier = Modifier.height(48.dp),
                    enabled = !wakatimeValidating && wakatimeApiKey.isNotBlank(),
                ) {
                    Text(if (wakatimeValidating) "Testing..." else "Test Connection")
                }
            }

            Spacer(Modifier.height(16.dp))

            HorizontalDivider()

            Text(
                text = "Port Scanner",
                style = MaterialTheme.typography.titleMedium,
            )

            var portScannerEndpoint by remember { mutableStateOf(settings.portScannerEndpoint ?: "") }
            var urlPattern by remember { mutableStateOf(settings.urlPattern) }
            var portScannerSaved by remember { mutableStateOf(false) }

            OutlinedTextField(
                value = portScannerEndpoint,
                onValueChange = { portScannerEndpoint = it; portScannerSaved = false },
                label = { Text("Scanner Endpoint") },
                placeholder = { Text("192.168.1.54:8765") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )

            OutlinedTextField(
                value = urlPattern,
                onValueChange = { urlPattern = it; portScannerSaved = false },
                label = { Text("URL Pattern") },
                placeholder = { Text("http://localhost:<port>") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                supportingText = { Text("Use <port> as placeholder for the port number") },
            )

            Button(
                onClick = {
                    settings.portScannerEndpoint = portScannerEndpoint.ifBlank { null }?.let { normalizeUrl(it) }
                    settings.urlPattern = urlPattern.ifBlank { "http://localhost:<port>" }
                    portScannerSaved = true
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !portScannerSaved,
            ) {
                Text(if (portScannerSaved) "Saved" else "Save")
            }

            Spacer(Modifier.height(16.dp))

            HorizontalDivider()

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Danger Zone",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )

            Button(
                onClick = {
                    showDisconnectDialog = true
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Disconnect")
            }
        }
    }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("Disconnect") },
            text = { Text("Are you sure you want to disconnect? This will clear your server URL and password.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisconnectDialog = false
                        settings.clear()
                        onDisconnect()
                    },
                ) {
                    Text("Disconnect", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

private fun normalizeUrl(raw: String): String =
    raw.trim().let { if (it.contains("://")) it.trimEnd('/') else "http://${it.trimEnd('/')}" }
