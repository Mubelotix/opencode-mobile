package page.dera.opencode.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import page.dera.opencode.api.AssistantContent
import page.dera.opencode.api.OpencodeApi
import page.dera.opencode.api.SessionMessage
import page.dera.opencode.api.ToolState
import opencodeapp.shared.generated.resources.Res
import opencodeapp.shared.generated.resources.triangle_alert
import opencodeapp.shared.generated.resources.clock
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    api: OpencodeApi,
    sessionId: String,
    sessionTitle: String,
    onBack: () -> Unit,
) {
    val messages = remember { mutableStateListOf<SessionMessage>() }
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(sessionId) {
        isLoading = true
        val result = api.getSessionMessages(sessionId)
        result.onSuccess { msgs ->
            messages.clear()
            messages.addAll(msgs)
        }.onFailure {
            errorText = it.message ?: "Failed to load messages"
        }
        isLoading = false
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = sessionTitle.ifEmpty { "Session" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("<", style = MaterialTheme.typography.titleMedium)
                    }
                },
            )
        },
        bottomBar = {
            InputBar(
                inputText = inputText,
                onInputChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        val text = inputText.trim()
                        inputText = ""
                        isSending = true
                        errorText = null
                        scope.launch {
                            val result = api.sendPrompt(sessionId, text)
                            if (result.isSuccess) {
                                isStreaming = true
                                while (isStreaming) {
                                    kotlinx.coroutines.coroutineScope {
                                        launch {
                                            try {
                                                api.streamSessionEvents(sessionId).collect { event ->
                                                    val fresh = api.getSessionMessages(sessionId)
                                                    fresh.onSuccess { msgList ->
                                                        messages.clear()
                                                        messages.addAll(msgList)
                                                    }
                                                    if (event.type == "session.next.step.ended" ||
                                                        event.type == "session.next.step.failed") {
                                                        throw CancellationException("step done")
                                                    }
                                                }
                                            } catch (_: CancellationException) {
                                            } catch (e: Exception) {
                                                println("OC_DBG: SSE error: ${e.message}")
                                            }
                                        }
                                    }
                                    delay(500)
                                    val last = api.getSessionMessages(sessionId)
                                    last.onSuccess { msgList ->
                                        messages.clear()
                                        messages.addAll(msgList)
                                        val done = msgList.none {
                                            it is SessionMessage.Assistant && it.finish == null && it.error == null
                                        }
                                        if (done) {
                                            isStreaming = false
                                            return@launch
                                        }
                                    }
                                }
                            } else {
                                errorText = result.exceptionOrNull()?.message ?: "Failed to send"
                                println("OC_DBG: send fail: $errorText")
                            }
                            isSending = false
                        }
                    }
                },
                isSending = isSending,
                isStreaming = isStreaming,
                enabled = !isStreaming,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (errorText != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(12.dp),
                ) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (messages.isEmpty() && !isStreaming) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Loading...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        } else {
                            Text(
                                text = sessionTitle.ifEmpty { "Session" },
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            if (errorText == null) {
                                Text(
                                    text = "No messages yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 8.dp,
                    ),
                ) {
                    items(messages) { message ->
                        MessageBubble(message)
                    }

                    if (isStreaming) {
                        item {
                            StreamingIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean,
    isStreaming: Boolean,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type a message...") },
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            enabled = enabled,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onSend,
            enabled = inputText.isNotBlank() && !isSending && enabled,
            modifier = Modifier.height(56.dp),
        ) {
            if (isSending || isStreaming) {
                Text("Wait")
            } else {
                Text("Send")
            }
        }
    }
}

@Composable
private fun MessageBubble(message: SessionMessage) {
    when (message) {
        is SessionMessage.User -> UserBubble(message)
        is SessionMessage.Assistant -> AssistantBubble(message)
        is SessionMessage.SystemMsg -> SystemBubble(message)
        is SessionMessage.Shell -> ShellBubble(message)
        is SessionMessage.Synthetic -> SyntheticBubble(message)
        is SessionMessage.AgentSwitched -> AgentSwitchedBubble(message)
        is SessionMessage.ModelSwitched -> ModelSwitchedBubble(message)
        is SessionMessage.Compaction -> CompactionBubble(message)
        is SessionMessage.Unknown -> UnknownBubble(message)
    }
}

@Composable
private fun UserBubble(message: SessionMessage.User) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(12.dp),
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (message.files.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                message.files.forEach { file ->
                    Text(
                        text = file.name ?: file.uri,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantBubble(message: SessionMessage.Assistant) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp),
    ) {
        if (message.content.isEmpty() && message.error != null) {
            ErrorContent(message)
        } else {
            message.content.forEach { content ->
                AssistantContentItem(content)
            }
        }

        if (message.error != null && (message.content.isNotEmpty() || message.finish != null)) {
            Spacer(modifier = Modifier.height(8.dp))
            ErrorContent(message)
        }
    }
}

@Composable
private fun ErrorContent(message: SessionMessage.Assistant) {
    val errorMsg = message.error?.message ?: "Unknown error"
    val isRateLimit = errorMsg.contains("429", ignoreCase = true) || errorMsg.contains("rate limit", ignoreCase = true)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Image(
                painter = painterResource(if (isRateLimit) Res.drawable.clock else Res.drawable.triangle_alert),
                contentDescription = if (isRateLimit) "Rate limited" else "Error",
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = if (isRateLimit) "Rate limited" else "Error",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun AssistantContentItem(content: AssistantContent) {
    when (content) {
        is AssistantContent.Text -> TextContent(content)
        is AssistantContent.Reasoning -> ReasoningContent(content)
        is AssistantContent.Tool -> ToolContent(content)
    }
}

@Composable
private fun TextContent(content: AssistantContent.Text) {
    Box(
        modifier = Modifier
            .widthIn(max = 360.dp)
            .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
    ) {
        Text(
            text = content.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReasoningContent(content: AssistantContent.Reasoning) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp),
    ) {
        Column {
            Text(
                text = "Thinking",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Default,
            )
        }
    }
}

@Composable
private fun ToolContent(content: AssistantContent.Tool) {
    val toolColor = when (content.state.status) {
        "pending" -> MaterialTheme.colorScheme.outline
        "running" -> MaterialTheme.colorScheme.tertiary
        "completed" -> MaterialTheme.colorScheme.primary
        "error" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(toolColor.copy(alpha = 0.1f))
            .padding(12.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(toolColor),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = content.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = toolColor,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = content.state.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = toolColor.copy(alpha = 0.7f),
                )
            }

            ToolStateContent(content.state)
        }
    }
}

@Composable
private fun ToolStateContent(state: ToolState) {
    when (state) {
        is ToolState.Completed -> {
            val inputText = extractToolInput(state.input)
            if (inputText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = inputText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 6,
                )
            }
        }
        is ToolState.Error -> {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = state.error.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        is ToolState.Running -> {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Running...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        is ToolState.Pending -> {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Pending...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun SystemBubble(message: SessionMessage.SystemMsg) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message.text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun ShellBubble(message: SessionMessage.Shell) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(12.dp),
        ) {
            Column {
                Text(
                    text = "$ ${message.command}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (message.output.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message.output,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 12,
                    )
                }
            }
        }
    }
}

@Composable
private fun SyntheticBubble(message: SessionMessage.Synthetic) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message.text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun AgentSwitchedBubble(message: SessionMessage.AgentSwitched) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Agent: ${message.agent}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun ModelSwitchedBubble(message: SessionMessage.ModelSwitched) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Model: ${message.model.providerID}/${message.model.modelID}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun CompactionBubble(message: SessionMessage.Compaction) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Context compacted (${message.reason})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = message.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 3,
            )
        }
    }
}

@Composable
private fun UnknownBubble(message: SessionMessage.Unknown) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            .padding(8.dp),
    ) {
        Text(
            text = "${message.messageType}: ${message.id}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        if (message.parseError.isNotEmpty()) {
            Text(
                text = message.parseError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 3,
            )
        }
    }
}

@Composable
private fun StreamingIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "OpenCode is responding...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

private fun extractToolInput(input: kotlinx.serialization.json.JsonElement?): String {
    if (input == null) return ""
    return try {
        when (input) {
            is kotlinx.serialization.json.JsonObject -> {
                val entries = input.entries.take(4).joinToString("\n") { "${it.key}: ${it.value}" }
                if (input.size > 4) "$entries\n..."
                else entries
            }
            is kotlinx.serialization.json.JsonPrimitive -> input.content
            else -> input.toString()
        }
    } catch (_: Exception) {
        ""
    }
}

