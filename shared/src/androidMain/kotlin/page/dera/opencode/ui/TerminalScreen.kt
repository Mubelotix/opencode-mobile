package page.dera.opencode.ui

import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.connectbot.terminal.Terminal
import org.connectbot.terminal.TerminalEmulatorFactory
import page.dera.opencode.api.OpencodeApi
import page.dera.opencode.api.PtyCreatePayload

@Composable
actual fun TerminalContent(api: OpencodeApi, directory: String) {
    val scope = rememberCoroutineScope()
    var ptyId by remember { mutableStateOf<String?>(null) }
    var commandChannel by remember { mutableStateOf<Channel<String>?>(null) }
    var ptyJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var ptyDir by remember { mutableStateOf(directory) }

    val channelRef = remember { mutableStateOf<Channel<String>?>(null) }

    val emulator = remember {
        TerminalEmulatorFactory.create(
            looper = Looper.getMainLooper(),
            initialRows = 24,
            initialCols = 80,
            onKeyboardInput = { bytes ->
                val text = bytes.decodeToString()
                if (text.isNotEmpty()) {
                    channelRef.value?.trySend(text)
                }
            },
        )
    }

    val density = LocalDensity.current
    var imeBottomPx by remember { mutableFloatStateOf(0f) }
    val view = LocalView.current

    DisposableEffect(view) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            imeBottomPx = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom.toFloat()
            insets
        }
        onDispose {
            ViewCompat.setOnApplyWindowInsetsListener(view, null)
        }
    }

    val imeBottomDp = with(density) { imeBottomPx.toDp() }

    LaunchedEffect(Unit) {
        val result = api.listPtys(directory)
        if (result.isSuccess) {
            val resp = result.getOrThrow()
            resp.location?.directory?.let { ptyDir = it }
            val existing = resp.data.firstOrNull { it.status == "running" }
            if (existing != null) {
                ptyId = existing.id
            } else {
                val create = api.createPty(ptyDir, PtyCreatePayload(title = "Terminal"))
                if (create.isSuccess) ptyId = create.getOrThrow().id
            }
        }
    }

    DisposableEffect(ptyId) {
        val id = ptyId
        val dir = ptyDir
        if (id != null) {
            val ch = Channel<String>(Channel.UNLIMITED)
            channelRef.value = ch
            commandChannel = ch
            ptyJob?.cancel()
            ptyJob = scope.launch(Dispatchers.Default) {
                try {
                    val tokenResult = api.connectPtyToken(id, dir)
                    val ticket = tokenResult.getOrNull()?.ticket
                    connectPtyWebSocket(
                        api = api, ptyID = id, directory = dir, cursor = 0, ticket = ticket,
                        onFrame = { text ->
                            emulator.writeInput(text.encodeToByteArray())
                        },
                        onMeta = {},
                        commands = ch,
                    )
                } catch (_: Exception) { }
            }
        }
        onDispose {
            ptyJob?.cancel(); ptyJob = null
            channelRef.value = null
            commandChannel?.close(); commandChannel = null
        }
    }

    if (ptyId == null) {
        Box(Modifier.fillMaxSize().background(Color(0xFF121212)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    } else {
        Box(Modifier.fillMaxSize().padding(bottom = imeBottomDp)) {
            Terminal(
                terminalEmulator = emulator,
                keyboardEnabled = true,
                modifier = Modifier.fillMaxSize(),
                backgroundColor = Color(0xFF121212),
            )
        }
    }
}
