package page.dera.opencode.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.time.Clock
import kotlinx.coroutines.launch
import page.dera.opencode.api.LegacyInfo
import page.dera.opencode.api.LegacyPart
import page.dera.opencode.api.LegacyToolState
import page.dera.opencode.api.LegacyWithParts
import page.dera.opencode.api.OpencodeApi
import page.dera.opencode.api.PermissionRequest
import page.dera.opencode.api.PermissionToolRef
import page.dera.opencode.api.QuestionInfo
import page.dera.opencode.api.QuestionOption
import page.dera.opencode.api.QuestionRequest
import page.dera.opencode.api.QuestionToolRef
import page.dera.opencode.api.SessionTokens
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.rememberMarkdownState
import opencodeapp.shared.generated.resources.globeIcon
import opencodeapp.shared.generated.resources.searchIcon
import opencodeapp.shared.generated.resources.botIcon
import opencodeapp.shared.generated.resources.brainIcon
import opencodeapp.shared.generated.resources.circleQuestionMarkIcon
import opencodeapp.shared.generated.resources.Res
import opencodeapp.shared.generated.resources.clock
import opencodeapp.shared.generated.resources.file
import opencodeapp.shared.generated.resources.folder
import opencodeapp.shared.generated.resources.terminal
import opencodeapp.shared.generated.resources.triangle_alert
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(api: OpencodeApi, sessionId: String, sessionTitle: String, directory: String, projectRoot: String, onBack: () -> Unit) {
    val messages = remember { mutableStateListOf<LegacyWithParts>() }
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var sessionTokens by remember { mutableStateOf<SessionTokens?>(null) }
    var sessionCost by remember { mutableStateOf<Double?>(null) }
    var currentTitle by remember { mutableStateOf(sessionTitle) }
    val streamingText = remember { mutableStateMapOf<String, String>() }
    var streamVersion by remember { mutableStateOf(0) }
    val permissionRequests = remember { mutableStateListOf<PermissionRequest>() }
    val questionRequests = remember { mutableStateListOf<QuestionRequest>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(sessionId) {
        isLoading = true
        permissionRequests.clear()
        api.getSessionMessages(sessionId).onSuccess { messages.clear(); messages.addAll(it) }.onFailure { errorText = it.message }
        isLoading = false
    }

    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }

    LaunchedEffect(sessionId, directory) {
        try {
            println("OC_DBG: SSE collect started session=$sessionId")
            api.streamGlobalEvents(directory).collect { event ->
                val props = event.payload?.properties?.jsonObject ?: return@collect
                val sid = props["sessionID"]?.jsonPrimitive?.content ?: return@collect
                if (sid != sessionId) return@collect
                val evType = event.payload?.type ?: "null"
                when (evType) {
                    "message.part.delta" -> {
                        val k = "${props["messageID"]?.jsonPrimitive?.content}:${props["partID"]?.jsonPrimitive?.content}"
                        streamingText[k] = (streamingText[k] ?: "") + (props["delta"]?.jsonPrimitive?.content ?: "")
                        streamVersion++
                    }
                    "message.part.updated" -> {
                        val p = props["part"]?.jsonObject ?: return@collect
                        val k = "${p["messageID"]?.jsonPrimitive?.content}:${p["id"]?.jsonPrimitive?.content}"
                        streamingText.remove(k)
                        val part = try { Json { ignoreUnknownKeys = true; coerceInputValues = true }.decodeFromJsonElement(LegacyPart.serializer(), p) } catch (_: Exception) { return@collect }
                        val mi = messages.indexOfFirst { it.info.id == p["messageID"]?.jsonPrimitive?.content }
                        if (mi >= 0) {
                        val mp = messages[mi].parts.toMutableList()
                        val pi = mp.indexOfFirst { it.id == part.id }
                        if (pi >= 0) mp[pi] = part else mp.add(part)
                        messages[mi] = LegacyWithParts(messages[mi].info, mp)
                    }
                    }
                    "message.updated" -> {
                        val infoJson = props["info"]?.jsonObject ?: return@collect
                        val info = try { Json { ignoreUnknownKeys = true; coerceInputValues = true }.decodeFromJsonElement(LegacyInfo.serializer(), infoJson) } catch (_: Exception) { return@collect }
                        val mi = messages.indexOfFirst { it.info.id == info.id }
                        if (mi >= 0) messages[mi] = LegacyWithParts(info, messages[mi].parts) else { println("OC_DBG: message.updated adding new role=${info.role} id=${info.id}"); messages.add(LegacyWithParts(info)) }
                    }
                    "session.status" -> { println("OC_DBG: session.status $sid"); isRunning = true }
                    "session.idle" -> {
                        println("OC_DBG: session.idle $sid, msgs before=${messages.size}")
                        isRunning = false
                        api.getSessionMessages(sessionId).onSuccess { fresh ->
                            messages.removeAll { !it.info.id.startsWith("evt_") }
                            fresh.forEach { f -> if (messages.none { m -> m.info.id == f.info.id }) messages.add(f) }
                            println("OC_DBG: session.idle done, msgs after=${messages.size}")
                        }
                    }
                    "session.updated" -> {
                        val infoJson = props["info"]?.jsonObject ?: return@collect
                        currentTitle = infoJson["title"]?.jsonPrimitive?.contentOrNull ?: currentTitle
                        try {
                            sessionTokens = Json { ignoreUnknownKeys = true; coerceInputValues = true }.decodeFromJsonElement(SessionTokens.serializer(), infoJson["tokens"] ?: return@collect)
                        } catch (_: Exception) {}
                        sessionCost = infoJson["cost"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                    }
                    "permission.asked" -> {
                        println("OC_DBG: permission.asked props=$props")
                        val id = props["id"]?.jsonPrimitive?.content ?: return@collect
                        val permission = props["permission"]?.jsonPrimitive?.content ?: ""
                        val patterns = props["patterns"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                        val always = props["always"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                        val toolJson = props["tool"]?.jsonObject
                        val tool = toolJson?.let {
                            PermissionRequest(
                                id = id, sessionID = sid, permission = permission,
                                patterns = patterns, always = always,
                                tool = page.dera.opencode.api.PermissionToolRef(
                                    messageID = it["messageID"]?.jsonPrimitive?.content ?: "",
                                    callID = it["callID"]?.jsonPrimitive?.content ?: ""
                                )
                            )
                        } ?: PermissionRequest(id = id, sessionID = sid, permission = permission, patterns = patterns, always = always)
                        if (permissionRequests.none { it.id == id }) permissionRequests.add(tool)
                    }
                    "permission.replied" -> {
                        val rid = props["requestID"]?.jsonPrimitive?.content ?: return@collect
                        println("OC_DBG: permission.replied requestID=$rid")
                        permissionRequests.removeAll { it.id == rid }
                    }
                    "question.asked" -> {
                        val id = props["id"]?.jsonPrimitive?.content ?: return@collect
                        val qlist = mutableListOf<QuestionInfo>()
                        props["questions"]?.jsonArray?.forEach { qj ->
                            val obj = qj.jsonObject
                            val opts = mutableListOf<QuestionOption>()
                            obj["options"]?.jsonArray?.forEach { oj ->
                                val oo = oj.jsonObject
                                opts.add(QuestionOption(
                                    label = oo["label"]?.jsonPrimitive?.content ?: "",
                                    description = oo["description"]?.jsonPrimitive?.content ?: ""
                                ))
                            }
                            qlist.add(QuestionInfo(
                                question = obj["question"]?.jsonPrimitive?.content ?: "",
                                header = obj["header"]?.jsonPrimitive?.content ?: "",
                                options = opts,
                                multiple = obj["multiple"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
                                custom = obj["custom"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: true
                            ))
                        }
                        val tool = props["tool"]?.jsonObject?.let {
                            QuestionToolRef(
                                messageID = it["messageID"]?.jsonPrimitive?.content ?: "",
                                callID = it["callID"]?.jsonPrimitive?.content ?: ""
                            )
                        }
                        val req = QuestionRequest(id = id, sessionID = sid, questions = qlist, tool = tool)
                        if (questionRequests.none { it.id == id }) questionRequests.add(req)
                    }
                    "question.replied" -> {
                        val rid = props["requestID"]?.jsonPrimitive?.content ?: return@collect
                        println("OC_DBG: question.replied requestID=$rid")
                        questionRequests.removeAll { it.id == rid }
                    }
                    "question.rejected" -> {
                        val rid = props["requestID"]?.jsonPrimitive?.content ?: return@collect
                        println("OC_DBG: question.rejected requestID=$rid")
                        questionRequests.removeAll { it.id == rid }
                    }
                    "session.diff" -> {
                        val diff = props["diff"]?.jsonArray
                        if (diff != null && diff.isEmpty()) return@collect
                        val rawId = "evt_${event.payload?.type ?: "unknown"}_${kotlin.random.Random.nextLong().toString(36)}"
                        val display = event.payload?.properties?.toString() ?: "{}"
                        println("OC_DBG: session.diff props=$display")
                        if (messages.none { it.info.id == rawId }) {
                            messages.add(LegacyWithParts(
                                LegacyInfo(id = rawId, sessionID = sessionId, role = "system"),
                                listOf(LegacyPart(id = rawId, sessionID = sessionId, messageID = rawId, type = "text", text = "[${event.payload?.type ?: "unknown"}] $display"))
                            ))
                        }
                    }
                    else -> {
                        val rawId = "evt_${event.payload?.type ?: "unknown"}_${kotlin.random.Random.nextLong().toString(36)}"
                        val display = event.payload?.properties?.toString() ?: "{}"
                        println("OC_DBG: unhandled event type=${event.payload?.type} props=$display")
                        if (messages.none { it.info.id == rawId }) {
                            messages.add(LegacyWithParts(
                                LegacyInfo(id = rawId, sessionID = sessionId, role = "system"),
                                listOf(LegacyPart(id = rawId, sessionID = sessionId, messageID = rawId, type = "text", text = "[${event.payload?.type ?: "unknown"}] $display"))
                            ))
                        }
                    }
                }
            }
        } catch (_: Exception) { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(currentTitle.ifEmpty { "Session" }, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        val subtitles = mutableListOf<String>()
                        val totalTokens = (sessionTokens?.input ?: 0) + (sessionTokens?.output ?: 0) + (sessionTokens?.reasoning ?: 0) + (sessionTokens?.cache?.read ?: 0) + (sessionTokens?.cache?.write ?: 0)
                        if (totalTokens > 0) subtitles.add(formatTokenCount(totalTokens))
                        sessionCost?.let { subtitles.add(formatCost(it)) }
                        if (subtitles.isNotEmpty()) {
                            Text(subtitles.joinToString("  ·  "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Text("<", style = MaterialTheme.typography.titleMedium) } }
            )
        },
        bottomBar = {},
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (errorText != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(12.dp),
                ) {
                    Text(
                        errorText!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (messages.isEmpty() && !isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(sessionTitle.ifEmpty { "Session" }, style = MaterialTheme.typography.headlineSmall) }
                else {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(0.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                        itemsIndexed(messages) { _, msg -> MessageCard(msg, streamingText, streamVersion, projectRoot) }
                        if (isSending || isRunning) item { SendingIndicator() }
                    }
                }
            }
            if (permissionRequests.isNotEmpty() || questionRequests.isNotEmpty()) {
                val scrollState = rememberScrollState()
                Box(Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(scrollState)) {
                    Column(Modifier.fillMaxWidth()) {
                        permissionRequests.forEach { req -> PermissionPrompt(req, api, sessionId, directory, projectRoot) }
                        questionRequests.forEach { req -> QuestionPrompt(req, api, sessionId, directory) }
                    }
                }
            }
            val doSend = {
                if (inputText.isNotBlank()) {
                    val t = inputText.trim()
                    inputText = ""
                    isSending = true
                    errorText = null
                    scope.launch {
                        println("OC_DBG: send")
                        if (api.sendPrompt(sessionId, t).isSuccess) {
                            println("OC_DBG: send ok")
                        } else {
                            println("OC_DBG: send fail")
                            errorText = "Failed to send"
                        }
                        isSending = false
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = inputText, onValueChange = { inputText = it }, modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") }, maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { doSend() }), enabled = !isSending
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { doSend() }, enabled = inputText.isNotBlank() && !isSending, modifier = Modifier.height(56.dp)) { Text(if (isSending) "..." else "Send") }
            }
        }
    }
}

@Composable
private fun SendingIndicator() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp),
        ) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                "OpenCode responding...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable private fun ReasoningPart(part: LegacyPart, streamingText: Map<String, String>, streamVersion: Int) {
    streamVersion
    val key = "${part.messageID}:${part.id}"
    val isStreaming = streamingText.containsKey(key)
    val t = streamingText[key] ?: (part.text ?: "")
    var expanded by remember { mutableStateOf(false) }
    val startTimes = remember { mutableStateMapOf<String, Long>() }
    val finalDurations = remember { mutableStateMapOf<String, String>() }
    if (isStreaming && !startTimes.containsKey(key)) {
        startTimes[key] = Clock.System.now().toEpochMilliseconds()
        finalDurations.remove(key)
    }
    val startTime = startTimes[key]
    if (!isStreaming && startTime != null && !finalDurations.containsKey(key)) {
        val totalMs = Clock.System.now().toEpochMilliseconds() - startTime
        finalDurations[key] = if (totalMs < 1000) "Thought for ${totalMs}ms"
        else "Thought for ${thinkingDurationSeconds(totalMs)}"
    }
    val headerText = if (isStreaming) {
        val elapsed = startTime?.let { Clock.System.now().toEpochMilliseconds() - it } ?: 0
        if (elapsed < 1000) "Thinking for ${elapsed}ms" else "Thinking for ${thinkingDurationText(elapsed)}s"
    } else finalDurations[key] ?: "Thought"
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(brainIcon), null, Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(headerText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (expanded && t.isNotBlank()) {
        Box(Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)).padding(8.dp)) {
            Text(t, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun thinkingDurationText(ms: Long): String {
    val tenths = (ms / 100).toInt()
    val whole = tenths / 10
    val frac = tenths % 10
    return if (whole < 10) "Thinking for $whole.${frac}s"
           else "Thinking for ${whole}s"
}

private fun thinkingDurationSeconds(ms: Long): String {
    val tenths = (ms / 100).toInt()
    val whole = tenths / 10
    val frac = tenths % 10
    return if (whole < 10) "$whole.$frac s"
           else "${whole}s"
}

@Composable private fun PermissionPrompt(req: PermissionRequest, api: OpencodeApi, sessionId: String, directory: String, projectRoot: String) {
    val scope = rememberCoroutineScope()
    var replying by remember { mutableStateOf(false) }
    val label = when (req.permission) {
        "tool_execution" -> "Run tool: ${req.tool?.callID?.takeLast(40) ?: "unknown"}"
        "read_file" -> "Read file: ${req.patterns.firstOrNull()?.let { relativePath(it, projectRoot) } ?: "unknown"}"
        "write_file" -> "Write file: ${req.patterns.firstOrNull()?.let { relativePath(it, projectRoot) } ?: "unknown"}"
        "edit_file" -> "Edit file: ${req.patterns.firstOrNull()?.let { relativePath(it, projectRoot) } ?: "unknown"}"
        else -> "${req.permission}: ${req.patterns.firstOrNull()?.takeLast(50) ?: "unknown"}"
    }
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(12.dp)) {
        Column {
            Text("Permission required", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            if (req.patterns.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(req.patterns.joinToString(", ") { relativePath(it, projectRoot) }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .7f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    replying = true
                    scope.launch {
                        println("OC_DBG: permission approve always ${req.id}")
                        api.replyPermission(sessionId, req.id, "always", directory)
                        replying = false
                    }
                }, enabled = !replying) {
                    Text("Always allow")
                }
                Button(onClick = {
                    replying = true
                    scope.launch {
                        println("OC_DBG: permission approve once ${req.id}")
                        api.replyPermission(sessionId, req.id, "once", directory)
                        replying = false
                    }
                }, enabled = !replying) {
                    Text("Approve once")
                }
                Button(onClick = {
                    replying = true
                    scope.launch {
                        println("OC_DBG: permission reject ${req.id}")
                        api.replyPermission(sessionId, req.id, "reject", directory)
                        replying = false
                    }
                }, enabled = !replying, colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )) {
                    Text("Deny")
                }
            }
        }
    }
}

@Composable
private fun QuestionPrompt(req: QuestionRequest, api: OpencodeApi, sessionId: String, directory: String) {
    val scope = rememberCoroutineScope()
    var replying by remember { mutableStateOf(false) }
    val selectedOptions = remember { req.questions.map { mutableStateListOf<String>() } }
    val customAnswers = remember { req.questions.map { mutableStateOf("") } }
    val customEnabled = remember { req.questions.map { mutableStateOf(false) } }

    fun buildAnswers(): List<List<String>> {
        return req.questions.mapIndexed { i, _ ->
            val selected = selectedOptions[i].toList()
            val custom = customAnswers[i].value
            if (selected.isEmpty() && custom.isBlank()) emptyList()
            else if (selected.isEmpty()) listOf(custom)
            else {
                if (custom.isNotBlank()) selected + custom else selected
            }
        }
    }

    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(12.dp)) {
        Column {
            Text("OpenCode asks", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
            req.questions.forEachIndexed { i, q ->
                Spacer(Modifier.height(8.dp))
                Text(q.header.ifEmpty { q.question }, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                if (q.header.isNotEmpty() && q.question.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(q.question, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .7f))
                }
                Spacer(Modifier.height(4.dp))
                q.options.forEach { opt ->
                    val isSelected = opt.label in selectedOptions[i]
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (q.multiple) {
                                if (isSelected) selectedOptions[i].remove(opt.label) else selectedOptions[i].add(opt.label)
                            } else {
                                selectedOptions[i].clear()
                                selectedOptions[i].add(opt.label)
                            }
                        }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (q.multiple) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedOptions[i].add(opt.label) else selectedOptions[i].remove(opt.label)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedOptions[i].clear()
                                    selectedOptions[i].add(opt.label)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Column {
                            Text(opt.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            if (opt.description.isNotBlank()) {
                                Text(opt.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .7f))
                            }
                        }
                    }
                }
                if (q.custom) {
                    val isCustomOn = customEnabled[i].value
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (q.multiple) {
                                customEnabled[i].value = !isCustomOn
                            } else {
                                selectedOptions[i].clear()
                                customEnabled[i].value = !isCustomOn
                            }
                        }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (q.multiple) {
                            Checkbox(
                                checked = isCustomOn,
                                onCheckedChange = { customEnabled[i].value = it },
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            RadioButton(
                                selected = isCustomOn,
                                onClick = {
                                    selectedOptions[i].clear()
                                    customEnabled[i].value = true
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("Type your own answer", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    if (isCustomOn) {
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = customAnswers[i].value,
                            onValueChange = { customAnswers[i].value = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Custom answer...") },
                            maxLines = 2,
                            enabled = !replying,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val answers = buildAnswers()
                    println("OC_DBG: question submit ${req.id} answers=$answers")
                    replying = true
                    scope.launch {
                        val r = api.replyQuestion(sessionId, req.id, answers, directory)
                        println("OC_DBG: question reply result=$r")
                        replying = false
                    }
                }, enabled = !replying) {
                    Text("Submit")
                }
                Button(onClick = {
                    replying = true
                    scope.launch {
                        val r = api.rejectQuestion(sessionId, req.id, directory)
                        println("OC_DBG: question dismiss result=$r")
                        replying = false
                    }
                }, enabled = !replying, colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )) {
                    Text("Dismiss")
                }
            }
        }
    }
}

@Composable
private fun MessageCard(msg: LegacyWithParts, streamingText: Map<String, String>, streamVersion: Int, projectRoot: String) {
    streamVersion
    val parts = msg.parts.filter { it.type !in listOf("step-start", "step-finish", "snapshot", "patch") }
    when (msg.info.role) {
        "user" -> Row(
            Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Column(
                Modifier
                    .widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(12.dp),
            ) {
                Text(
                    parts.filter { it.type == "text" }.joinToString("\n") { it.text ?: "" }
                        .ifEmpty { "Sent a message" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        "assistant" -> AssistantCard(msg.info, parts, streamingText, streamVersion, projectRoot)
        else -> Box(
            Modifier.fillMaxWidth().padding(vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Text(
                    "System message",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                parts.filter { it.type == "text" && !it.text.isNullOrBlank() }.forEach { part ->
                    Text(
                        part.text!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantCard(info: LegacyInfo, parts: List<LegacyPart>, streamingText: Map<String, String>, streamVersion: Int, projectRoot: String) {
    streamVersion
    Column(Modifier.fillMaxWidth().padding(start = 8.dp)) {
        if (info.error != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Image(painterResource(Res.drawable.triangle_alert), null, Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Error",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            info.error!!.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }
        parts.forEach { PartContent(it, streamingText, streamVersion, projectRoot) }
    }
}

@Composable
private fun PartContent(part: LegacyPart, streamingText: Map<String, String>, streamVersion: Int, projectRoot: String) {
    streamVersion
    when (part.type) {
        "text" -> {
            val t = (streamingText["${part.messageID}:${part.id}"] ?: (part.text ?: "")).trimEnd()
            val md = rememberMarkdownState(t, retainState = true)
            Box(
                Modifier
                    .widthIn(max = 360.dp)
                    .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
            ) {
                Markdown(md)
            }
        }
        "reasoning" -> ReasoningPart(part, streamingText, streamVersion)
        "tool" -> ToolPart(part, projectRoot)
        "compaction" -> Box(
            Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Context compacted",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        "agent" -> AgentPart(part)
    }
}

@Composable private fun AgentPart(part: LegacyPart) {
    val label = part.name?.let { "Launched $it" } ?: "Launched agent"
    val sub = part.description?.takeIf { it.isNotBlank() } ?: part.title?.takeIf { it.isNotBlank() }
    Row(Modifier.padding(vertical = 2.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(botIcon), null, Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (sub != null) {
                Spacer(Modifier.height(2.dp))
                Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 2)
            }
        }
    }
}

@Composable
private fun ToolPart(part: LegacyPart, projectRoot: String) {
    val tc = when (part.state?.status) {
        "pending" -> MaterialTheme.colorScheme.outline
        "running" -> MaterialTheme.colorScheme.tertiary
        "completed" -> MaterialTheme.colorScheme.primary
        "error" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }
    val label = toolLabel(part, projectRoot)
    val matchSuffix = matchCount(part)
    if (part.tool == "task") {
        val desc = taskDescription(part)
        Row(
            Modifier
                .padding(vertical = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(botIcon), null, Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (desc != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        desc,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 2,
                    )
                }
            }
            Box(Modifier.size(6.dp).clip(CircleShape).background(tc))
        }
    } else if (part.tool == "bash") {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E1E2E))
                .padding(10.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(Res.drawable.terminal), null, Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFCDD6F4),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(4.dp))
                    Box(Modifier.size(6.dp).clip(CircleShape).background(tc))
                }
                if (part.state is LegacyToolState.Running) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Running...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA6ADC8),
                        )
                    }
                }
            }
        }
    } else {
        var expanded by remember { mutableStateOf(false) }
        val isExpandable = part.tool in setOf("websearch", "webfetch", "question")
        val icon = when {
            part.tool == "read" && isDirectoryRead(part) -> Res.drawable.folder
            part.tool in listOf("glob", "grep") -> searchIcon
            part.tool == "question" -> circleQuestionMarkIcon
            part.tool in listOf("websearch", "webfetch") -> globeIcon
            else -> Res.drawable.file
        }
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(if (isExpandable) Modifier.clickable { expanded = !expanded } else Modifier)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(painterResource(icon), null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    modifier = Modifier.weight(1f),
                )
                if (matchSuffix != null) {
                    Text(
                        " $matchSuffix",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    part.state?.status ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = tc.copy(alpha = .7f),
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
            }
            if (expanded && isExpandable) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 4.dp)
                        .clickable { expanded = false }
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp),
                ) {
                    Column {
                        val inputObj = when (val s = part.state) {
                            is LegacyToolState.Pending -> s.input
                            is LegacyToolState.Running -> s.input
                            is LegacyToolState.Completed -> s.input
                            is LegacyToolState.Error -> s.input
                            null -> null
                        }
                        if (inputObj != null) {
                            val pretty = Json { prettyPrint = true }
                            Text(
                                pretty.encodeToString(JsonElement.serializer(), inputObj),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        val output = when (val s = part.state) {
                            is LegacyToolState.Completed -> s.output.takeIf { it.isNotBlank() }
                            is LegacyToolState.Error -> s.error.takeIf { it.isNotBlank() }
                            else -> null
                        }
                        if (output != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                output,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun matchCount(part: LegacyPart): String? {
    if (part.tool !in setOf("glob", "grep")) return null
    val output = when (val s = part.state) {
        is LegacyToolState.Completed -> s.output
        else -> return null
    }
    val count = output.lines().count { it.isNotBlank() }
    return if (count > 0) "($count matches)" else null
}

private fun isDirectoryRead(part: LegacyPart): Boolean {
    val meta = when (val s = part.state) {
        is LegacyToolState.Running -> s.metadata
        is LegacyToolState.Completed -> s.metadata
        else -> null
    }
    return meta?.jsonObject?.get("display")?.jsonObject?.get("type")?.jsonPrimitive?.contentOrNull == "directory"
}

private fun relativePath(absolute: String, root: String): String {
    val normalized = root.trimEnd('/') + "/"
    val rel = absolute.removePrefix(normalized)
    return if (rel != absolute) rel else absolute.substringAfterLast('/').ifEmpty { absolute }
}

private fun toolLabel(part: LegacyPart, projectRoot: String): String {
    val input = when (val s = part.state) {
        is LegacyToolState.Pending -> s.input
        is LegacyToolState.Running -> s.input
        is LegacyToolState.Completed -> s.input
        is LegacyToolState.Error -> s.input
        null -> null
    }
    val obj = input as? JsonObject ?: return part.tool ?: "tool"
    return when(part.tool) {
        "read","write","edit" -> obj["filePath"]?.toString()?.removeSurrounding("\"")?.let { relativePath(it, projectRoot) }?.ifEmpty{null} ?: (part.tool ?: "tool")
        "glob","grep" -> obj["pattern"]?.toString()?.removeSurrounding("\"").let{ if(it!=null&&it.length>40) it.take(40)+"..." else it} ?: (part.tool ?: "tool")
        "bash" -> obj["command"]?.toString()?.removeSurrounding("\"").let{ if(it!=null) "$$it" else null} ?: "bash"
        "task" -> {
            val subType = obj["subagent_type"]?.toString()?.removeSurrounding("\"") ?: ""
            if (subType.isNotBlank()) "Launched $subType agent" else "Launched agent"
        }
        "question" -> {
            val questions = obj["questions"]?.jsonArray
            val first = questions?.firstOrNull()?.jsonObject
            first?.get("question")?.toString()?.removeSurrounding("\"")
                ?: first?.get("header")?.toString()?.removeSurrounding("\"")
                ?: "Asking question"
        }
        "websearch" -> obj["query"]?.toString()?.removeSurrounding("\"").let {
            if (it != null && it.length > 40) it.take(40) + "..." else it
        } ?: "websearch"
        "webfetch" -> obj["url"]?.toString()?.removeSurrounding("\"").let {
            val host = it?.let { url ->
                Regex("://([^/]+)").find(url)?.groupValues?.get(1)
            } ?: it
            if (host != null && host.length > 40) host.take(40) + "..." else host
        } ?: "webfetch"
        else -> part.tool ?: "tool"
    }
}

private fun taskDescription(part: LegacyPart): String? {
    val input = when (val s = part.state) {
        is LegacyToolState.Pending -> s.input
        is LegacyToolState.Running -> s.input
        is LegacyToolState.Completed -> s.input
        is LegacyToolState.Error -> s.input
        null -> null
    }
    val obj = input as? JsonObject ?: return null
    return obj["description"]?.toString()?.removeSurrounding("\"")?.takeIf { it.isNotBlank() }
}

private fun formatTokenCount(count: Long): String {
    return when {
        count >= 1_000_000 -> "${count / 1_000_000}.${(count % 1_000_000) / 100_000}M tokens"
        count >= 1_000 -> "${count / 1_000}.${(count % 1_000) / 100}k tokens"
        else -> "$count tokens"
    }
}

private fun formatCost(cost: Double): String {
    val dollars = cost.toLong()
    val cents = ((cost - dollars) * 100).toLong()
    return "$$dollars.${cents.toString().padStart(2, '0')}"
}
