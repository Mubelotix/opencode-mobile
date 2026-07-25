package page.dera.opencode.ui

import kotlinx.coroutines.channels.Channel
import page.dera.opencode.api.OpencodeApi

expect suspend fun connectPtyWebSocket(
    api: OpencodeApi,
    ptyID: String,
    directory: String,
    cursor: Int = 0,
    ticket: String? = null,
    onFrame: suspend (String) -> Unit,
    onMeta: suspend (Int) -> Unit,
    commands: Channel<String>,
)
