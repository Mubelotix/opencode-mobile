package page.dera.opencode.ui

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import page.dera.opencode.api.OpencodeApi

actual suspend fun connectPtyWebSocket(
    api: OpencodeApi,
    ptyID: String,
    directory: String,
    cursor: Int,
    ticket: String?,
    onFrame: suspend (String) -> Unit,
    onMeta: suspend (Int) -> Unit,
    commands: Channel<String>,
) {
    val wsUrl = api.buildPtyWebSocketUrl(ptyID, directory, cursor, ticket)
    val client = HttpClient { install(WebSockets) }
    try {
        client.webSocket(wsUrl) {
            coroutineScope {
                val sender = launch {
                    for (cmd in commands) {
                        try { send(Frame.Text("$cmd\r")) } catch (_: Exception) { break }
                    }
                }
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.data.decodeToString()
                            if (text.isNotEmpty()) onFrame(text)
                        }
                        is Frame.Binary -> {
                            val bytes = frame.data
                            if (bytes.isNotEmpty() && bytes[0] == 0.toByte()) {
                                val jsonStr = bytes.copyOfRange(1, bytes.size).decodeToString()
                                try {
                                    val meta = Json { ignoreUnknownKeys = true }.decodeFromString<ptyMeta>(jsonStr)
                                    meta.cursor?.let { onMeta(it) }
                                } catch (_: Exception) {}
                            }
                        }
                        else -> {}
                    }
                }
                sender.cancel()
            }
        }
    } finally {
        client.close()
    }
}

@kotlinx.serialization.Serializable
private data class ptyMeta(val cursor: Int? = null)
