package page.dera.opencode.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import page.dera.opencode.api.OpencodeApi
import page.dera.opencode.api.PtyCreatePayload

@Composable
actual fun rememberImeBottomDp(): Float = 0f

@Composable
actual fun TerminalContent(api: OpencodeApi, directory: String) {
    val scope = rememberCoroutineScope()
    var ptyId by remember { mutableStateOf<String?>(null) }
    val terminalLines = remember { mutableStateListOf<TerminalLine>() }
    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    val inputFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    var commandChannel by remember { mutableStateOf<Channel<String>?>(null) }
    var ptyJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    LaunchedEffect(Unit) {
        val result = api.listPtys(directory)
        if (result.isSuccess) {
            val resp = result.getOrThrow()
            val existing = resp.data.firstOrNull { it.status == "running" && it.cwd == directory }
            if (existing != null) {
                ptyId = existing.id
            } else {
                val create = api.createPty(directory, PtyCreatePayload(title = "Terminal", cwd = directory))
                if (create.isSuccess) ptyId = create.getOrThrow().id
            }
        }
    }

    DisposableEffect(ptyId) {
        val id = ptyId
        val dir = directory
        if (id != null) {
            terminalLines.clear()
            val ch = Channel<String>(Channel.UNLIMITED)
            commandChannel = ch
            ptyJob?.cancel()
            ptyJob = scope.launch(Dispatchers.Default) {
                try {
                    val tokenResult = api.connectPtyToken(id, dir)
                    val ticket = tokenResult.getOrNull()?.ticket
                    connectPtyWebSocket(
                        api = api, ptyID = id, directory = dir, cursor = 0, ticket = ticket,
                        onFrame = { text ->
                            val lines = parseTerminalOutput(text)
                            scope.launch { terminalLines.addAll(lines) }
                        },
                        onMeta = {},
                        commands = ch,
                    )
                } catch (_: Exception) { }
            }
        }
        onDispose {
            ptyJob?.cancel(); ptyJob = null
            commandChannel?.close(); commandChannel = null
        }
    }

    if (ptyId == null) {
        Box(Modifier.fillMaxSize().background(Color(0xFF121212)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    } else {
        Column(Modifier.fillMaxSize().background(Color(0xFF121212)).imePadding()) {
            SelectionContainer(Modifier.weight(1f)) {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp),
                    state = listState,
                ) {
                    items(terminalLines.size) { i -> TerminalLineView(terminalLines[i]) }
                }
            }

            LaunchedEffect(terminalLines.size) {
                if (terminalLines.isNotEmpty()) listState.scrollToItem(terminalLines.size - 1)
            }

            if (terminalLines.isNotEmpty()) {
                LaunchedEffect(listState.layoutInfo.viewportEndOffset) {
                    listState.scrollToItem(terminalLines.size - 1)
                }
            }

            Row(
                Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$ ", color = Color(0xFF4CAF50), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                Spacer(Modifier.width(4.dp))
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f).focusRequester(inputFocus)
                        .onFocusChanged { if (it.isFocused) inputText = inputText.copy(selection = TextRange(inputText.text.length)) },
                    textStyle = TextStyle(color = Color(0xFFE0E0E0), fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    cursorBrush = SolidColor(Color(0xFFE0E0E0)),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        val text = inputText.text
                        if (text.isNotBlank()) {
                            commandChannel?.trySend(text)
                            inputText = TextFieldValue("")
                        }
                    }),
                )
            }

            LaunchedEffect(Unit) { inputFocus.requestFocus() }
        }
    }
}

@Composable
private fun TerminalLineView(line: TerminalLine) {
    val annotated = buildAnnotatedString {
        for (span in line.spans) {
            val color = span.fg?.let { colorForFg(it) } ?: Color(0xFFE0E0E0)
            withStyle(SpanStyle(color = color, fontFamily = FontFamily.Monospace, fontSize = 13.sp)) {
                append(span.text)
            }
        }
    }
    Text(annotated, modifier = Modifier.padding(horizontal = 2.dp))
}

private data class Span(val text: String, val fg: TerminalColor? = null)
private data class TerminalLine(val spans: List<Span>)

private fun colorForFg(c: TerminalColor): Color = when (c) {
    TerminalColor.Black -> Color(0xFF000000)
    TerminalColor.Red -> Color(0xFFEF4444)
    TerminalColor.Green -> Color(0xFF4CAF50)
    TerminalColor.Yellow -> Color(0xFFEAB308)
    TerminalColor.Blue -> Color(0xFF3B82F6)
    TerminalColor.Magenta -> Color(0xFFA855F7)
    TerminalColor.Cyan -> Color(0xFF06B6D4)
    TerminalColor.White -> Color(0xFFE0E0E0)
    TerminalColor.BrightBlack -> Color(0xFF666666)
    TerminalColor.BrightRed -> Color(0xFFF87171)
    TerminalColor.BrightGreen -> Color(0xFF86EFAC)
    TerminalColor.BrightYellow -> Color(0xFFFDE047)
    TerminalColor.BrightBlue -> Color(0xFF93C5FD)
    TerminalColor.BrightMagenta -> Color(0xFFD8B4FE)
    TerminalColor.BrightCyan -> Color(0xFF67E8F9)
    TerminalColor.BrightWhite -> Color(0xFFFFFFFF)
}

enum class TerminalColor {
    Black, Red, Green, Yellow, Blue, Magenta, Cyan, White,
    BrightBlack, BrightRed, BrightGreen, BrightYellow, BrightBlue, BrightMagenta, BrightCyan, BrightWhite,
}

private fun stripNonColorEscapes(text: String): String {
    val sb = StringBuilder(text.length)
    var i = 0
    while (i < text.length) {
        if (text[i] == '\u001b') {
            i++
            if (i >= text.length) break
            when (text[i]) {
                '[' -> {
                    sb.append('\u001b').append('[')
                    i++
                    while (i < text.length && (text[i].isDigit() || text[i] == ';' || text[i] == '?')) sb.append(text[i++])
                    if (i < text.length) { sb.append(text[i]); i++ }
                }
                ']', 'P', '_', '^' -> {
                    i++
                    while (i < text.length && text[i] != '\u0007' && text[i] != '\u001b') i++
                    if (i < text.length) i++
                }
                '(' -> { i += 2 }
                ')' -> { i += 2 }
                else -> i++
            }
        } else if (text[i] == '\r') {
            i++
        } else {
            sb.append(text[i++])
        }
    }
    return sb.toString()
}

private fun parseTerminalOutput(text: String): List<TerminalLine> {
    val cleaned = stripNonColorEscapes(text)
    val lines = mutableListOf<TerminalLine>()
    var fg: TerminalColor? = null
    var i = 0
    var current = StringBuilder()
    val spans = mutableListOf<Span>()

    fun flushLine() {
        if (current.isNotEmpty()) {
            spans.add(Span(current.toString(), fg))
            current.clear()
        }
        if (spans.isNotEmpty()) {
            lines.add(TerminalLine(spans.toList()))
            spans.clear()
        }
    }

    while (i < cleaned.length) {
        if (cleaned[i] == '\u001b' && i + 1 < cleaned.length && cleaned[i + 1] == '[') {
            i += 2
            val ps = i
            while (i < cleaned.length && (cleaned[i].isDigit() || cleaned[i] == ';' || cleaned[i] == '?')) i++
            if (i < cleaned.length) {
                val cmd = cleaned[i++]
                if (cmd == 'm') {
                    val params = cleaned.substring(ps, i - 1).split(';').mapNotNull { it.toIntOrNull() }
                    val prevFg = fg
                    if (params.isEmpty() || params[0] == 0) { fg = null }
                    else for (p in params) when (p) {
                        30 -> fg = TerminalColor.Black; 31 -> fg = TerminalColor.Red; 32 -> fg = TerminalColor.Green; 33 -> fg = TerminalColor.Yellow; 34 -> fg = TerminalColor.Blue; 35 -> fg = TerminalColor.Magenta; 36 -> fg = TerminalColor.Cyan; 37 -> fg = TerminalColor.White
                        90 -> fg = TerminalColor.BrightBlack; 91 -> fg = TerminalColor.BrightRed; 92 -> fg = TerminalColor.BrightGreen; 93 -> fg = TerminalColor.BrightYellow; 94 -> fg = TerminalColor.BrightBlue; 95 -> fg = TerminalColor.BrightMagenta; 96 -> fg = TerminalColor.BrightCyan; 97 -> fg = TerminalColor.BrightWhite
                    }
                    if (fg != prevFg) {
                        if (current.isNotEmpty()) {
                            spans.add(Span(current.toString(), prevFg))
                            current.clear()
                        }
                    }
                }
            }
        } else if (cleaned[i] == '\n') {
            flushLine(); i++
        } else {
            current.append(cleaned[i++])
        }
    }
    flushLine()
    return lines
}
