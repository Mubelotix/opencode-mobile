package page.dera.opencode.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import page.dera.opencode.api.LegacyWithParts

@Composable
internal actual fun ChatMessageList(
    modifier: Modifier,
    messages: List<LegacyWithParts>,
    streamingText: Map<String, String>,
    projectRoot: String,
    chatPainters: ChatPainters,
    markdownCache: MutableMap<String, com.mikepenz.markdown.model.MarkdownState>,
    parsedSegments: Map<String, List<DocumentSegment>>,
    isSending: Boolean,
    isRunning: Boolean
) {
    val scrollState = rememberScrollState()
    var displayLimit by remember { mutableStateOf(50) }
    val displayedMessages = if (messages.size > displayLimit) messages.takeLast(displayLimit) else messages
    var preLoadScrollMax by remember { mutableStateOf(Int.MIN_VALUE) }
    var loadTarget by remember { mutableStateOf(0) }

    var prevMessageCount by remember { mutableStateOf(0) }
    LaunchedEffect(messages.size, isSending, isRunning) {
        if (messages.isNotEmpty()) {
            if (prevMessageCount == 0 || messages.size > prevMessageCount) {
                scrollState.scrollTo(scrollState.maxValue)
            }
        }
        prevMessageCount = messages.size
    }

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value }
            .collect { value ->
                if (value <= 300 && messages.size > displayLimit && loadTarget == 0) {
                    preLoadScrollMax = scrollState.maxValue
                    loadTarget = minOf(displayLimit + 50, messages.size)
                }
            }
    }

    LaunchedEffect(loadTarget) {
        val target = loadTarget
        if (target <= 0) return@LaunchedEffect
        while (displayLimit < target) {
            delay(16)
            displayLimit = minOf(displayLimit + 10, target)
        }
        delay(16)
        val saved = preLoadScrollMax
        if (saved != Int.MIN_VALUE && scrollState.maxValue > saved) {
            scrollState.scrollTo(scrollState.maxValue - saved)
            preLoadScrollMax = Int.MIN_VALUE
        }
        loadTarget = 0
    }

    Column(modifier = modifier.verticalScroll(scrollState)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            displayedMessages.forEach { msg ->
                key(msg.info.id) {
                    MessageCard(msg, streamingText, projectRoot, chatPainters, markdownCache, parsedSegments)
                }
            }
            if (isSending || isRunning) SendingIndicator()
        }
    }
}
