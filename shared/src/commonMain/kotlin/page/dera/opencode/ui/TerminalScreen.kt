package page.dera.opencode.ui

import androidx.compose.runtime.Composable
import page.dera.opencode.api.OpencodeApi

@Composable
expect fun TerminalContent(api: OpencodeApi, directory: String)

@Composable
expect fun rememberImeBottomDp(): Float
