package page.dera.opencode.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import page.dera.opencode.api.LegacyWithParts

@Composable
internal expect fun ChatMessageList(
    modifier: Modifier = Modifier,
    messages: List<LegacyWithParts>,
    streamingText: Map<String, String>,
    projectRoot: String,
    chatPainters: ChatPainters,
    markdownCache: MutableMap<String, com.mikepenz.markdown.model.MarkdownState>,
    parsedSegments: Map<String, List<DocumentSegment>>,
    isSending: Boolean,
    isRunning: Boolean
)
