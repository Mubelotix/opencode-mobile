package page.dera.opencode.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListPrefetchScope
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.foundation.lazy.layout.NestedPrefetchScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.time.Clock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import page.dera.opencode.api.CommandInfo
import page.dera.opencode.api.LegacyInfo
import page.dera.opencode.api.LegacyPart
import page.dera.opencode.api.LegacyToolState
import page.dera.opencode.api.LegacyWithParts
import page.dera.opencode.api.OpencodeApi
import page.dera.opencode.api.PermissionRequest
import page.dera.opencode.api.ReviewComment
import page.dera.opencode.api.PermissionToolRef
import page.dera.opencode.api.QuestionInfo
import page.dera.opencode.api.QuestionOption
import page.dera.opencode.api.QuestionRequest
import page.dera.opencode.api.QuestionToolRef
import page.dera.opencode.api.ProviderInfo
import page.dera.opencode.api.SessionModel
import page.dera.opencode.api.SessionTokens
import page.dera.opencode.api.TodoInfo
import page.dera.opencode.ui.TerminalContent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.serialization.builtins.ListSerializer
import opencodeapp.shared.generated.resources.globeIcon
import opencodeapp.shared.generated.resources.folderIcon
import opencodeapp.shared.generated.resources.scissorsIcon
import opencodeapp.shared.generated.resources.searchIcon
import opencodeapp.shared.generated.resources.sendIcon
import opencodeapp.shared.generated.resources.infoIcon
import opencodeapp.shared.generated.resources.botIcon
import opencodeapp.shared.generated.resources.brainIcon
import opencodeapp.shared.generated.resources.circleQuestionMarkIcon
import opencodeapp.shared.generated.resources.checkIcon
import opencodeapp.shared.generated.resources.circleXIcon
import opencodeapp.shared.generated.resources.infinityIcon
import opencodeapp.shared.generated.resources.Res
import opencodeapp.shared.generated.resources.clock
import opencodeapp.shared.generated.resources.file
import opencodeapp.shared.generated.resources.folder
import opencodeapp.shared.generated.resources.terminal
import opencodeapp.shared.generated.resources.triangle_alert
import opencodeapp.shared.generated.resources.listChecksIcon
import opencodeapp.shared.generated.resources.clipboardListIcon
import opencodeapp.shared.generated.resources.filePenIcon
import opencodeapp.shared.generated.resources.menuIcon
import opencodeapp.shared.generated.resources.messageSquareIcon
import opencodeapp.shared.generated.resources.terminalIcon
import org.jetbrains.compose.resources.painterResource

private val prettyJson = Json { prettyPrint = true }
private val urlHostRegex = Regex("://([^/]+)")
private val hiddenPartTypes = setOf("step-start", "step-finish", "snapshot", "patch")

// One MutableState per streaming part: appending a delta only invalidates the
// composable rendering that part. (A plain SnapshotStateMap<String, String> would
// recompose every reader of the map — i.e. all visible messages — on every delta.)
@Stable
private class StreamingTexts {
    private val states = mutableStateMapOf<String, MutableState<String>>()
    fun append(key: String, delta: String) {
        val state = states.getOrPut(key) { mutableStateOf("") }
        state.value += delta
    }
    fun clear(key: String) { states.remove(key) }
    fun textOrNull(key: String): String? = states[key]?.value
    fun isStreaming(key: String): Boolean = states.containsKey(key)
}

// The lazy list works on one row per message part, not per message: items must be
// small enough to compose within a frame while scrolling.
private sealed interface ChatRow {
    val key: String
    val contentType: String
}
private data class UserTextRow(override val key: String, val text: String) : ChatRow {
    override val contentType get() = "user"
}
private data class SystemTextRow(override val key: String, val text: String) : ChatRow {
    override val contentType get() = "system"
}
private data class AssistantErrorRow(override val key: String, val errorName: String) : ChatRow {
    override val contentType get() = "error"
}
// Derived strings are precomputed here so row composition does no JSON digging.
private data class AssistantPartRow(
    override val key: String,
    val part: LegacyPart,
    val toolLabel: String = "",
    val matchSuffix: String? = null,
    val taskDesc: String? = null,
    val isDirectory: Boolean = false,
) : ChatRow {
    override val contentType get() = "${part.type}:${part.tool ?: ""}"
}
private data object IndicatorRow : ChatRow {
    override val key get() = "sending_indicator"
    override val contentType get() = "indicator"
}
private data class CompactionRow(override val key: String) : ChatRow {
    override val contentType get() = "compaction_indicator"
}

// The default prefetch strategy composes only 1 item ahead, which a fling
// outruns instantly with rows this small; when several rows then enter the
// viewport in one frame, their synchronous composition blows the frame budget
// and the fling position jumps even further — a jank cascade. Prefetching a
// deeper window absorbs the fling with idle-time composition.
@OptIn(ExperimentalFoundationApi::class)
private class DeepPrefetchStrategy(private val depth: Int) : LazyListPrefetchStrategy {
    private val handles = mutableMapOf<Int, LazyLayoutPrefetchState.PrefetchHandle>()
    private var direction = 0

    override fun LazyListPrefetchScope.onScroll(delta: Float, layoutInfo: LazyListLayoutInfo) {
        if (layoutInfo.visibleItemsInfo.isEmpty()) return
        val forward = delta < 0f
        val newDirection = if (forward) 1 else -1
        if (newDirection != direction) {
            handles.values.forEach { it.cancel() }
            handles.clear()
            direction = newDirection
        }
        val first = layoutInfo.visibleItemsInfo.first().index
        val last = layoutInfo.visibleItemsInfo.last().index
        val targets = if (forward) (last + 1)..(last + depth) else (first - depth) until first
        val iterator = handles.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key < first - depth || entry.key > last + depth) {
                entry.value.cancel()
                iterator.remove()
            }
        }
        for (i in targets) {
            if (i < 0 || i >= layoutInfo.totalItemsCount) continue
            if (i !in handles) handles[i] = schedulePrefetch(i)
        }
    }

    override fun LazyListPrefetchScope.onVisibleItemsUpdated(layoutInfo: LazyListLayoutInfo) {}

    override fun NestedPrefetchScope.onNestedPrefetch(firstVisibleItemIndex: Int) {
        repeat(2) { schedulePrefetch(firstVisibleItemIndex + it) }
    }
}

// Resolved once per screen; painterResource machinery per row measurably added
// to composition time. data class so remembered painter instances compare equal
// and rows keep skipping.
private data class ChatPainters(
    val bot: Painter,
    val folder: Painter,
    val file: Painter,
    val filePen: Painter,
    val search: Painter,
    val question: Painter,
    val globe: Painter,
    val terminal: Painter,
    val invalid: Painter,
    val brain: Painter,
    val alert: Painter,
    val listChecks: Painter,
    val scissors: Painter,
)

// Newest row first, because the list uses reverseLayout.
private fun buildRows(messages: List<LegacyWithParts>, showIndicator: Boolean, projectRoot: String): List<ChatRow> = Perf.timed("buildRows msgs=${messages.size}", thresholdMs = 1.0) {
    val rows = ArrayList<ChatRow>(messages.size * 2 + 1)
    for (msg in messages) {
        val parts = msg.parts.filter { it.type !in hiddenPartTypes }
        when (msg.info.role) {
            "user" -> {
                val textParts = parts.filter { it.type == "text" }
                val text = textParts.joinToString("\n") { it.text ?: "" }
                val label = when {
                    text.isNotBlank() -> text
                    parts.any { it.type == "compaction" } -> "/compact"
                    parts.any { it.type == "subtask" } -> {
                        val cmd = parts.first { it.type == "subtask" }.command
                        if (!cmd.isNullOrBlank()) "/$cmd" else "Launched agent"
                    }
                    parts.any { it.type == "command" } -> parts.first { it.type == "command" }.command?.let { "/$it" } ?: "Sent a message"
                    parts.isEmpty() -> "Sending..."
                    else -> "Sent a message"
                }
                rows.add(UserTextRow("u:${msg.info.id}", label))
                if (parts.any { it.type == "compaction" }) {
                    rows.add(CompactionRow("compaction:${msg.info.id}"))
                }
            }
            "assistant" -> {
                if (msg.info.error != null) rows.add(AssistantErrorRow("e:${msg.info.id}", msg.info.error!!.name))
                parts.forEach { p ->
                    rows.add(
                        if (p.type == "tool") AssistantPartRow(
                            "p:${msg.info.id}:${p.id}", p,
                            toolLabel = toolLabel(p, projectRoot),
                            matchSuffix = matchCount(p),
                            taskDesc = if (p.tool == "task") taskDescription(p) else null,
                            isDirectory = if (p.tool == "read") isDirectoryRead(p) else false,
                        )
                        else AssistantPartRow("p:${msg.info.id}:${p.id}", p)
                    )
                }
            }
            else -> rows.add(SystemTextRow(
                "s:${msg.info.id}",
                parts.filter { it.type == "text" && !it.text.isNullOrBlank() }.joinToString("\n") { it.text!! },
            ))
        }
    }
    if (showIndicator) rows.add(IndicatorRow)
    rows.reverse()
    rows
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(api: OpencodeApi, sessionId: String, sessionTitle: String, directory: String, projectRoot: String, onBack: () -> Unit, onMenuClick: (() -> Unit)? = null, onTitleChange: ((String) -> Unit)? = null) {
    val messages = remember { mutableStateListOf<LegacyWithParts>() }
    @OptIn(ExperimentalFoundationApi::class)
    val listState = rememberLazyListState(prefetchStrategy = remember { DeepPrefetchStrategy(depth = 8) })
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var sessionTokens by remember { mutableStateOf<SessionTokens?>(null) }
    var sessionCost by remember { mutableStateOf<Double?>(null) }
    var sessionModel by remember { mutableStateOf<SessionModel?>(null) }
    var providers by remember { mutableStateOf<List<ProviderInfo>>(emptyList()) }
    var commands by remember { mutableStateOf<List<CommandInfo>>(emptyList()) }
    var currentTitle by remember { mutableStateOf(sessionTitle) }
    var isEditingTitle by remember { mutableStateOf(false) }
    var editTitleText by remember { mutableStateOf(currentTitle) }
    var titleWasFocused by remember { mutableStateOf(false) }
    val titleFocusRequester = remember { FocusRequester() }
    var fileChangeTick by remember { mutableStateOf(0) }
    val reviewComments = remember {
        val saved = try { Settings().getStringOrNull("review_$sessionId") } catch (_: Exception) { null }
        val list = mutableStateListOf<ReviewComment>()
        if (saved != null) try { list.addAll(Json { ignoreUnknownKeys = true }.decodeFromString<List<ReviewComment>>(saved)) } catch (_: Exception) { }
        list
    }
    LaunchedEffect(reviewComments.toList()) {
        val json = Json { prettyPrint = false }
        Settings().putString("review_$sessionId", json.encodeToString(kotlinx.serialization.builtins.ListSerializer(ReviewComment.serializer()), reviewComments.toList()))
    }

    LaunchedEffect(isEditingTitle) {
        if (isEditingTitle) {
            editTitleText = currentTitle
            titleWasFocused = false
            titleFocusRequester.requestFocus()
        }
    }
    val streamingText = remember { StreamingTexts() }
    val permissionRequests = remember { mutableStateListOf<PermissionRequest>() }
    val questionRequests = remember { mutableStateListOf<QuestionRequest>() }
    var selectedTab by remember { mutableStateOf(0) }
    var todos by remember { mutableStateOf<List<TodoInfo>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(sessionId) {
        isLoading = true
        permissionRequests.clear()
        val t0 = Perf.clock.markNow()
        api.getSessionMessages(sessionId).onSuccess {
            val fetchMs = Perf.ms(t0)
            Perf.timed("initial-load apply msgs=${it.size} parts=${it.sumOf { m -> m.parts.size }} fetch=${fetchMs}ms") {
                messages.clear(); messages.addAll(it)
            }
        }.onFailure { errorText = it.message }
        isLoading = false
    }

    LaunchedEffect(sessionId, directory) {
        api.getSession(sessionId, directory).onSuccess { info ->
            sessionTokens = info.tokens
            sessionCost = info.cost
            sessionModel = info.model
            currentTitle = info.title.ifEmpty { currentTitle }
        }
        api.listProviders(directory).onSuccess { providers = it }
        api.listCommands(directory).onSuccess { commands = it }
    }

    LaunchedEffect(sessionId, directory) {
        api.getSessionTodo(sessionId, directory).onSuccess { todos = it }
    }

    val rows by remember { derivedStateOf { buildRows(messages, isSending || isRunning, projectRoot) } }

    val chatPainters = ChatPainters(
        bot = painterResource(botIcon),
        folder = painterResource(Res.drawable.folder),
        file = painterResource(Res.drawable.file),
        filePen = painterResource(filePenIcon),
        search = painterResource(searchIcon),
        question = painterResource(circleQuestionMarkIcon),
        globe = painterResource(globeIcon),
        terminal = painterResource(Res.drawable.terminal),
        invalid = painterResource(circleXIcon),
        brain = painterResource(brainIcon),
        alert = painterResource(Res.drawable.triangle_alert),
        listChecks = painterResource(listChecksIcon),
        scissors = painterResource(scissorsIcon),
    )

    // reverseLayout keeps the viewport pinned to the bottom while the newest row
    // grows; jump back down when a new row arrives, unless the user scrolled away.
    val newestRowKey = rows.firstOrNull()?.key
    LaunchedEffect(newestRowKey) {
        if (newestRowKey != null && listState.firstVisibleItemIndex <= 1) {
            Perf.timed("scrollToItem(0) key=$newestRowKey") {
                listState.scrollToItem(0)
            }
        }
    }

    FrameStatsLogger()
    ScrollStatsLogger(listState)

    LaunchedEffect(sessionId, directory) {
        val pendingDeltas = mutableMapOf<String, StringBuilder>()
        var flushJob: Job? = null
        fun flushDeltas() {
            Perf.timed("flushDeltas n=${pendingDeltas.size}", thresholdMs = 1.0) {
                pendingDeltas.forEach { (k, sb) -> streamingText.append(k, sb.toString()) }
                pendingDeltas.clear()
            }
        }
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
                        pendingDeltas.getOrPut(k) { StringBuilder() }.append(props["delta"]?.jsonPrimitive?.content ?: "")
                        flushJob?.cancel()
                        flushJob = launch {
                            delay(16)
                            flushDeltas()
                            flushJob = null
                        }
                    }
                    else -> {
                        flushDeltas()
                        flushJob?.cancel()
                        flushJob = null
                        when (evType) {
                            "message.part.updated" -> {
                            val t0 = Perf.clock.markNow()
                            val p = props["part"]?.jsonObject ?: return@collect
                            val k = "${p["messageID"]?.jsonPrimitive?.content}:${p["id"]?.jsonPrimitive?.content}"
                            streamingText.clear(k)
                            val part = try { Json { ignoreUnknownKeys = true; coerceInputValues = true }.decodeFromJsonElement(LegacyPart.serializer(), p) } catch (_: Exception) { return@collect }
                            val mi = messages.indexOfFirst { it.info.id == p["messageID"]?.jsonPrimitive?.content }
                            if (mi >= 0) {
                            val mp = messages[mi].parts.toMutableList()
                            val pi = mp.indexOfFirst { it.id == part.id }
                            if (pi >= 0) mp[pi] = part else mp.add(part)
                            messages[mi] = LegacyWithParts(messages[mi].info, mp)
                        }
                            val elapsed = Perf.ms(t0)
                            if (elapsed >= 1.0) println("OC_PERF: part.updated type=${part.type} textLen=${part.text?.length ?: 0} took=${elapsed}ms")
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
                            fileChangeTick++
                            api.getSessionMessages(sessionId).onSuccess { fresh ->
                                Perf.timed("session.idle refresh msgs=${fresh.size}") {
                                    messages.removeAll { !it.info.id.startsWith("evt_") }
                                    fresh.forEach { f -> if (messages.none { m -> m.info.id == f.info.id }) messages.add(f) }
                                }
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
                        "todo.updated" -> {
                            val td = props["todos"]?.jsonArray ?: return@collect
                            todos = td.mapNotNull { try { Json { ignoreUnknownKeys = true; coerceInputValues = true }.decodeFromJsonElement(TodoInfo.serializer(), it) } catch (_: Exception) { null } }
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
                        "session.compacted" -> {
                            val rawId = "evt_session.compacted_${kotlin.random.Random.nextLong().toString(36)}"
                            println("OC_DBG: session.compacted $sid")
                            if (messages.none { it.info.id == rawId }) {
                                messages.add(LegacyWithParts(
                                    LegacyInfo(id = rawId, sessionID = sessionId, role = "assistant"),
                                    listOf(LegacyPart(id = rawId, sessionID = sessionId, messageID = rawId, type = "compaction"))
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
                }
            }
        } catch (_: Exception) { }
    }

    val imeBottomDp = rememberImeBottomDp().dp

    Scaffold(
        modifier = Modifier.padding(bottom = imeBottomDp),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        if (isEditingTitle) {
                            val saveTitle = {
                                val newTitle = editTitleText.trim()
                                if (newTitle.isNotEmpty() && newTitle != currentTitle) {
                                    val oldTitle = currentTitle
                                    currentTitle = newTitle
                                    scope.launch {
                                        val result = api.updateSessionTitle(sessionId, newTitle, directory)
                                        if (result.isFailure) {
                                            currentTitle = oldTitle
                                        } else {
                                            onTitleChange?.invoke(newTitle)
                                        }
                                    }
                                }
                                isEditingTitle = false
                            }
                            OutlinedTextField(
                                value = editTitleText,
                                onValueChange = { editTitleText = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .focusRequester(titleFocusRequester)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) titleWasFocused = true
                                        else if (!focusState.isFocused && titleWasFocused && isEditingTitle) {
                                            saveTitle()
                                        }
                                    }
                                    .fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { saveTitle() }),
                            )
                        } else {
                            Text(
                                currentTitle.ifEmpty { "Session" },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                modifier = Modifier.clickable { isEditingTitle = true },
                            )
                        }

                    }
                },
                navigationIcon = {
                    if (onMenuClick != null) {
                        IconButton(onClick = onMenuClick) {
                            Image(
                                painter = painterResource(menuIcon),
                                contentDescription = "Tabs",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val tabs = listOf(
                    Triple(messageSquareIcon, "Conversation", 0),
                    Triple(folderIcon, "Files", 1),
                    Triple(listChecksIcon, "Todo", 2),
                    Triple(terminalIcon, "Terminal", 3),
                    Triple(infoIcon, "Info", 4),
                    Triple(globeIcon, "Browser", 5),
                )
                tabs.forEach { (icon, desc, index) ->
                    val isSelected = selectedTab == index
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedTab = index }.padding(horizontal = 12.dp, vertical = 2.dp),
                    ) {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = desc,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.height(1.dp))
                        Box(
                            Modifier.width(16.dp).height(2.dp)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(1.dp))
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier.fillMaxSize()
                    .zIndex(if (selectedTab == 0) 1f else 0f)
                    .then(if (selectedTab == 0) Modifier else Modifier.alpha(0f))
            ) {
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
                        LazyColumn(
                            state = listState,
                            reverseLayout = true,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            items(rows, key = { it.key }, contentType = { it.contentType }) { row ->
                                Box(Modifier.timedMeasure(row.key, thresholdMs = 4.0)) {
                                    val t0 = Perf.clock.markNow()
                                    ChatRowContent(row, streamingText, chatPainters, onInterrupt = if (row is IndicatorRow) {{ scope.launch { api.interruptSession(sessionId) } }} else null)
                                    val elapsed = Perf.ms(t0)
                                    if (elapsed >= 2.0) println("OC_PERF: compose row=${row.key} type=${row.contentType} took=${elapsed}ms")
                                }
                            }
                        }
                    }
                }
                if (permissionRequests.isNotEmpty() || questionRequests.isNotEmpty()) {
                    val permScrollState = rememberScrollState()
                    Box(Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(permScrollState)) {
                        Column(Modifier.fillMaxWidth()) {
                            permissionRequests.forEach { req -> PermissionPrompt(req, api, sessionId, directory, projectRoot) }
                            questionRequests.forEach { req -> QuestionPrompt(req, api, sessionId, directory) }
                        }
                    }
                }
                val doSend = {
                    if (inputText.isNotBlank()) {
                        val t = inputText.trim()
                        if (t.startsWith("/")) {
                            val spaceIndex = t.indexOf(' ')
                            val commandName = if (spaceIndex > 0) t.substring(1, spaceIndex) else t.substring(1)
                            val args = if (spaceIndex > 0) t.substring(spaceIndex + 1) else ""
                            if (commandName == "compact") {
                                inputText = ""
                                if (sessionModel == null) {
                                    errorText = "No model configured"
                                } else {
                                    isSending = true
                                    errorText = null
                                    scope.launch {
                                        println("OC_DBG: summarize session")
                                        api.compactSession(sessionId, sessionModel!!.providerID, sessionModel!!.id, directory).onFailure { e ->
                                            println("OC_DBG: summarize fail ${e.message}")
                                            errorText = e.message ?: "Failed to compact"
                                        }
                                        isSending = false
                                    }
                                }
                            } else {
                                val cmd = commands.find { it.name == commandName }
                                if (cmd == null) {
                                    errorText = "Unknown command: /$commandName"
                                } else {
                                    inputText = ""
                                    isSending = true
                                    errorText = null
                                    scope.launch {
                                        println("OC_DBG: sendCommand $commandName args=$args")
                                        api.sendCommand(sessionId, commandName, args, directory).onFailure { e ->
                                            println("OC_DBG: sendCommand fail ${e.message}")
                                            errorText = e.message ?: "Failed to send command"
                                            isSending = false
                                        }.onSuccess {
                                            println("OC_DBG: sendCommand ok")
                                            isSending = false
                                        }
                                    }
                                }
                            }
                        } else {
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
                }
                if (reviewComments.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = {
                            val md = "Here is a review of the code:\n\n" + reviewComments.joinToString("\n\n") { c ->
                                "**${c.filePath}:${c.lineStart}** - ${c.text}"
                            }
                            reviewComments.clear()
                            isSending = true
                            scope.launch {
                                if (api.sendPrompt(sessionId, md).isSuccess) { println("OC_DBG: review sent") }
                                else { errorText = "Failed to send review" }
                                isSending = false
                            }
                        }) {
                            Text("Send ${reviewComments.size} comment${if (reviewComments.size > 1) "s" else ""}")
                        }
                    }
                }
                val showCommands = inputText.startsWith("/") && !inputText.contains(" ")
                val commandFilterText = if (showCommands && inputText.length > 1) inputText.removePrefix("/").lowercase() else ""
                val compactVirtualCmd = CommandInfo(name = "compact", template = "/compact", description = "Compact session context")
                val filteredCommands = remember(inputText, commands) {
                    if (!showCommands) emptyList()
                    else {
                        val matches = commands.filter { it.name.lowercase().contains(commandFilterText) }
                        val includeCompact = commandFilterText.isEmpty() || "compact".contains(commandFilterText)
                        if (includeCompact) listOf(compactVirtualCmd) + matches
                        else matches
                    }
                }
                if (filteredCommands.isNotEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp).zIndex(2f)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp),
                    ) {
                        Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 240.dp)) {
                            filteredCommands.take(12).forEach { cmd ->
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { inputText = "/${cmd.name} " }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "/${cmd.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    if (cmd.description != null) {
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            cmd.description!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
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
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        painter = painterResource(sendIcon),
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank() && !isSending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier
                            .size(48.dp)
                            .padding(12.dp)
                            .clickable(enabled = inputText.isNotBlank() && !isSending) { doSend() }
                    )
                }
            }
            if (selectedTab == 2) {
                TodoContent(todos)
            } else if (selectedTab == 3) {
                Box(Modifier.fillMaxSize()) {
                    TerminalContent(api, directory)
                }
            } else if (selectedTab == 4) {
                InfoContent(sessionTokens, sessionCost, sessionModel, providers, messages)
            } else if (selectedTab != 0 && selectedTab != 1) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                    when (selectedTab) {
                        5 -> "Browser"
                        else -> ""
                    },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(
                Modifier.fillMaxSize()
                    .zIndex(if (selectedTab == 1) 1f else 0f)
                    .then(if (selectedTab == 1) Modifier else Modifier.alpha(0f))
            ) {
                GitDiffContent(api, directory, fileChangeTick, reviewComments)
            }
        }
    }
}

@Composable
private fun SendingIndicator(onInterrupt: (() -> Unit)? = null) {
    Box(
        Modifier
            .fillMaxWidth()
            .then(if (onInterrupt != null) Modifier.clickable { onInterrupt() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp),
        ) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                if (onInterrupt != null) "OpenCode is generating (tap to stop)"
                else "OpenCode responding...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable private fun ReasoningPart(part: LegacyPart, streamingText: StreamingTexts, painters: ChatPainters) {
    val key = "${part.messageID}:${part.id}"
    val isStreaming = streamingText.isStreaming(key)
    val t = streamingText.textOrNull(key) ?: (part.text ?: "")
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
        if (elapsed < 1000) "Thinking for ${elapsed}ms" else thinkingDurationText(elapsed)
    } else finalDurations[key] ?: "Thought"
    Column(Modifier.clickable { expanded = !expanded }) {
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painters.brain, null, Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(headerText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (expanded && t.isNotBlank()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)).padding(8.dp)) {
                Text(truncateForDisplay(t), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = {
                    replying = true
                    scope.launch {
                        println("OC_DBG: permission approve always ${req.id}")
                        api.replyPermission(sessionId, req.id, "always", directory)
                        replying = false
                    }
                }, enabled = !replying) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(infinityIcon), null, Modifier.size(18.dp))
                        Text("Always allow", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Button(onClick = {
                    replying = true
                    scope.launch {
                        println("OC_DBG: permission approve once ${req.id}")
                        api.replyPermission(sessionId, req.id, "once", directory)
                        replying = false
                    }
                }, enabled = !replying) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(checkIcon), null, Modifier.size(18.dp))
                        Text("Approve once", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                    }
                }
                OutlinedButton(onClick = {
                    replying = true
                    scope.launch {
                        println("OC_DBG: permission reject ${req.id}")
                        api.replyPermission(sessionId, req.id, "reject", directory)
                        replying = false
                    }
                }, enabled = !replying,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(circleXIcon), null, Modifier.size(18.dp))
                        Text("Deny", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                    }
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(checkIcon), null, Modifier.size(18.dp))
                        Text("Submit", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                    }
                }
                OutlinedButton(onClick = {
                    replying = true
                    scope.launch {
                        val r = api.rejectQuestion(sessionId, req.id, directory)
                        println("OC_DBG: question dismiss result=$r")
                        replying = false
                    }
                }, enabled = !replying,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(circleXIcon), null, Modifier.size(18.dp))
                        Text("Dismiss", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatRowContent(row: ChatRow, streamingText: StreamingTexts, painters: ChatPainters, onInterrupt: (() -> Unit)? = null) {
    when (row) {
        is UserTextRow -> Row(
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Column(
                Modifier
                    .widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(12.dp),
            ) {
                SelectionContainer {
                    Text(
                        row.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        is AssistantErrorRow -> {
            if (row.errorName == "MessageAbortedError") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(painters.invalid, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Interrupted by user",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Image(painters.alert, null, Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Error",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                row.errorName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }
        }
        is AssistantPartRow -> Box(Modifier.fillMaxWidth().padding(start = 8.dp)) {
            PartContent(row, streamingText, painters)
        }
        is SystemTextRow -> Box(
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
                if (row.text.isNotBlank()) {
                    Text(
                        row.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        IndicatorRow -> SendingIndicator(onInterrupt)
        is CompactionRow -> Row(
            Modifier.fillMaxWidth().padding(start = 8.dp).padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painters.scissors, null, Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                "Compacted context",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PartContent(row: AssistantPartRow, streamingText: StreamingTexts, painters: ChatPainters) {
    val part = row.part
    when (part.type) {
        "text" -> {
            val streamed = streamingText.textOrNull("${part.messageID}:${part.id}")
            val t = (streamed ?: part.text ?: "").trimEnd()
            Box(
                Modifier
                    .widthIn(max = 360.dp)
                    .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
            ) {
                SelectionContainer {
                    MarkdownText(t, cacheable = streamed == null)
                }
            }
        }
        "reasoning" -> ReasoningPart(part, streamingText, painters)
        "tool" -> ToolPart(row, painters)
        "compaction" -> Row(
            Modifier.padding(vertical = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painters.scissors, null, Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Compacted context",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        "agent" -> AgentPart(part, painters)
    }
}

@Composable private fun AgentPart(part: LegacyPart, painters: ChatPainters) {
    val label = part.name?.let { "Launched $it" } ?: "Launched agent"
    val sub = part.description?.takeIf { it.isNotBlank() } ?: part.title?.takeIf { it.isNotBlank() }
    Row(Modifier.padding(vertical = 2.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painters.bot, null, Modifier.size(14.dp))
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
private fun ToolPart(row: AssistantPartRow, painters: ChatPainters) {
    val part = row.part
    val tc = when (part.state?.status) {
        "pending" -> MaterialTheme.colorScheme.outline
        "running" -> MaterialTheme.colorScheme.tertiary
        "completed" -> MaterialTheme.colorScheme.primary
        "error" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }
    val label = row.toolLabel
    val matchSuffix = row.matchSuffix
    if (part.tool == "task") {
        val desc = row.taskDesc
        Row(
            Modifier
                .padding(vertical = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painters.bot, null, Modifier.size(14.dp))
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
                    Image(painters.terminal, null, Modifier.size(14.dp))
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
        val expandDiffsSetting = remember { Settings().getBoolean("expand_diffs_by_default", true) }
        var expanded by remember { mutableStateOf(part.tool == "edit" && expandDiffsSetting) }
        val isExpandable = part.tool in setOf("websearch", "webfetch", "question", "invalid", "read", "write", "edit", "todowrite")
        val icon = when {
            part.tool == "invalid" -> painters.invalid
            part.tool == "edit" -> painters.filePen
            part.tool == "read" && row.isDirectory -> painters.folder
            part.tool in listOf("glob", "grep") -> painters.search
            part.tool == "question" -> painters.question
            part.tool in listOf("websearch", "webfetch") -> painters.globe
            part.tool == "todowrite" -> painters.listChecks
            else -> painters.file
        }
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(if (isExpandable) Modifier.clickable { expanded = !expanded } else Modifier)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(icon, null, Modifier.size(14.dp))
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
                        if (part.tool == "edit") {
                            val inputObj = remember(part) {
                                when (val s = part.state) {
                                    is LegacyToolState.Pending -> s.input
                                    is LegacyToolState.Running -> s.input
                                    is LegacyToolState.Completed -> s.input
                                    is LegacyToolState.Error -> s.input
                                    null -> null
                                }
                            }
                            val obj = inputObj?.jsonObject
                            val old = obj?.get("oldString")?.jsonPrimitive?.content ?: ""
                            val new = obj?.get("newString")?.jsonPrimitive?.content ?: ""
                            val diffLines = remember(old, new) { if (old.isNotEmpty() || new.isNotEmpty()) computeEditDiff(old, new) else emptyList() }
                            if (diffLines.isNotEmpty()) {
                                val monoStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                                diffLines.forEach { line ->
                                        val bg = when {
                                            line.isAddition -> EditAddBg
                                            line.isDeletion -> EditDelBg
                                            else -> Color.Transparent
                                        }
                                        val fg = when {
                                            line.isAddition -> EditAddFg
                                            line.isDeletion -> EditDelFg
                                            else -> Color.Transparent
                                        }
                                        val prefix = when {
                                            line.isAddition -> "+"
                                            line.isDeletion -> "-"
                                            else -> " "
                                        }
                                        Row(
                                            Modifier.fillMaxWidth().background(bg).padding(horizontal = 4.dp, vertical = 1.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                prefix,
                                                style = monoStyle,
                                                color = fg,
                                            )
                                            Text(
                                                line.content,
                                                style = monoStyle,
                                                color = if (fg == Color.Transparent) MaterialTheme.colorScheme.onSurfaceVariant else fg,
                                            )
                                        }
                                    }
                                }
                        } else {
                            val inputPretty = remember(part) {
                                val inputObj = when (val s = part.state) {
                                    is LegacyToolState.Pending -> s.input
                                    is LegacyToolState.Running -> s.input
                                    is LegacyToolState.Completed -> s.input
                                    is LegacyToolState.Error -> s.input
                                    null -> null
                                }
                                inputObj?.let { truncateForDisplay(prettyJson.encodeToString(JsonElement.serializer(), it)) }
                            }
                            if (inputPretty != null) {
                                Text(
                                    inputPretty,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        val output = remember(part) {
                            when (val s = part.state) {
                                is LegacyToolState.Completed -> s.output.takeIf { it.isNotBlank() }
                                is LegacyToolState.Error -> s.error.takeIf { it.isNotBlank() }
                                else -> null
                            }?.let { truncateForDisplay(it) }
                        }
                        if (output != null && part.tool != "edit") {
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

private fun truncateForDisplay(s: String, max: Int = 8000): String =
    if (s.length <= max) s else s.take(max) + "\n… (${s.length - max} more characters)"

private data class EditDiffLine(val content: String, val isAddition: Boolean, val isDeletion: Boolean)

private fun computeEditDiff(oldString: String, newString: String): List<EditDiffLine> {
    val oldLines = oldString.lines()
    val newLines = newString.lines()
    if (oldLines == newLines) return oldLines.map { EditDiffLine(it, false, false) }
    val ops = computeDiffOps(oldLines, newLines)
    val result = mutableListOf<EditDiffLine>()
    var oi = 0
    var ni = 0
    for (op in ops) {
        when (op) {
            DiffOp.EQUAL -> {
                result.add(EditDiffLine(oldLines[oi++], false, false))
                ni++
            }
            DiffOp.DELETE -> result.add(EditDiffLine(oldLines[oi++], false, true))
            DiffOp.INSERT -> result.add(EditDiffLine(newLines[ni++], true, false))
        }
    }
    return result
}

private enum class DiffOp { EQUAL, DELETE, INSERT }

private fun computeDiffOps(old: List<String>, new: List<String>): List<DiffOp> {
    val m = old.size
    val n = new.size
    val dp = Array(m + 1) { IntArray(n + 1) }
    for (i in 1..m) {
        for (j in 1..n) {
            dp[i][j] = if (old[i - 1] == new[j - 1]) dp[i - 1][j - 1] + 1
            else maxOf(dp[i - 1][j], dp[i][j - 1])
        }
    }
    val ops = mutableListOf<DiffOp>()
    var i = m
    var j = n
    while (i > 0 || j > 0) {
        when {
            i > 0 && j > 0 && old[i - 1] == new[j - 1] -> {
                ops.add(DiffOp.EQUAL); i--; j--
            }
            j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j]) -> {
                ops.add(DiffOp.INSERT); j--
            }
            i > 0 -> {
                ops.add(DiffOp.DELETE); i--
            }
        }
    }
    return ops.asReversed()
}

private val EditAddBg = Color(0xFFE6FFEC)
private val EditDelBg = Color(0xFFFFEBE9)
private val EditAddFg = Color(0xFF1A7F37)
private val EditDelFg = Color(0xFFCF222E)

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
                urlHostRegex.find(url)?.groupValues?.get(1)
            } ?: it
            if (host != null && host.length > 40) host.take(40) + "..." else host
        } ?: "webfetch"
        "todowrite" -> {
            val todos = obj["todos"]?.jsonArray
            val count = todos?.size ?: 0
            "Planned $count todo item${if (count != 1) "s" else ""}"
        }
        "invalid" -> {
            val error = obj["error"]?.toString()?.removeSurrounding("\"") ?: ""
            error.ifBlank { "Invalid tool call" }
        }
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

@Composable
private fun InfoContent(
    sessionTokens: SessionTokens?,
    sessionCost: Double?,
    sessionModel: SessionModel?,
    providers: List<ProviderInfo>,
    messages: List<LegacyWithParts>,
) {
    val tokens = sessionTokens
    if (tokens == null && sessionCost == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Session info not available yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Run a prompt to see token usage and cost",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    val userCount = messages.count { it.info.role == "user" }
    val assistantCount = messages.count { it.info.role == "assistant" }
    val systemCount = messages.count { it.info.role == "system" }

    val input = tokens?.input ?: 0
    val output = tokens?.output ?: 0
    val reasoning = tokens?.reasoning ?: 0
    val cacheRead = tokens?.cache?.read ?: 0
    val cacheWrite = tokens?.cache?.write ?: 0
    val total = input + output + reasoning + cacheRead + cacheWrite

    val contextLimit = remember(sessionModel, providers) {
        val model = sessionModel ?: messages.lastOrNull { it.info.role == "assistant" }?.info?.let {
            SessionModel(id = it.modelID, providerID = it.providerID)
        }
        model?.let { m ->
            providers.find { it.id == m.providerID }
                ?.models?.get(m.id)
                ?.limit?.context
        }
    }

    val lastMsgTokens = messages.lastOrNull { it.info.role == "assistant" }?.info?.tokens
    val msgTotal = lastMsgTokens?.total
        ?: (lastMsgTokens?.input ?: 0) + (lastMsgTokens?.output ?: 0) + (lastMsgTokens?.reasoning ?: 0)
    val msgInput = lastMsgTokens?.input ?: 0
    val msgOutput = lastMsgTokens?.output ?: 0
    val msgReasoning = lastMsgTokens?.reasoning ?: 0

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            "Context Usage",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))

        val contextInput = if (msgTotal > 0) msgTotal - msgOutput - msgReasoning else msgInput
        TokenBreakdownBar(
            input = contextInput,
            output = msgOutput,
            reasoning = msgReasoning,
            contextLimit = contextLimit,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        InfoStat(label = "Total tokens", value = formatTokenCount(total))
        contextLimit?.let {
            val usagePercent = if (it > 0 && msgTotal > 0) (msgTotal.toDouble() / it * 100).toInt() else 0
            InfoStat(label = "Context limit", value = "${formatTokenCount(it)} (${usagePercent}% used)")
        }
        InfoStat(label = "Input tokens", value = formatTokenCount(input))
        InfoStat(label = "Output tokens", value = formatTokenCount(output))
        InfoStat(label = "Reasoning tokens", value = formatTokenCount(reasoning))
        InfoStat(label = "Cache read", value = formatTokenCount(cacheRead))
        InfoStat(label = "Cache write", value = formatTokenCount(cacheWrite))
        InfoStat(label = "Cost", value = sessionCost?.let { formatCost(it) } ?: "—")
        Spacer(Modifier.height(16.dp))

        Text(
            "Messages",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))

        InfoStat(label = "Total messages", value = messages.size.toString())
        InfoStat(label = "User messages", value = userCount.toString())
        InfoStat(label = "Assistant messages", value = assistantCount.toString())
        if (systemCount > 0) {
            InfoStat(label = "System messages", value = systemCount.toString())
        }

        val firstMessage = messages.firstOrNull()
        val lastMessage = messages.lastOrNull()
        if (firstMessage != null || lastMessage != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Timeline",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            firstMessage?.let {
                InfoStat(label = "First message", value = formatTimestamp(it.info.time.created))
            }
            lastMessage?.let {
                InfoStat(label = "Last message", value = formatTimestamp(it.info.time.created))
            }
        }
    }
}

@Composable
private fun TokenBreakdownBar(
    input: Long,
    output: Long,
    reasoning: Long,
    contextLimit: Long?,
    modifier: Modifier = Modifier,
) {
    val used = input + output + reasoning
    if (used <= 0 && contextLimit == null) return

    val free = contextLimit?.let { if (it > used) it - used else 0L } ?: 0
    val totalWidth = if (contextLimit != null && contextLimit > 0) contextLimit else used

    Column(modifier) {
        Row(
            Modifier
                .height(8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp)),
        ) {
            if (used > 0) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .weight(used.toFloat())
                        .background(Color(0xFF4A90D9)),
                )
            }
            Box(
                Modifier
                    .fillMaxHeight()
                    .weight(if (free > 0) free.toFloat() else 1f)
                    .background(Color(0xFFE0E0E0)),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4A90D9)),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Used",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (free > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E0E0)),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Free",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(epochMillis: Long): String {
    if (epochMillis <= 0) return "—"
    val elapsed = Clock.System.now().toEpochMilliseconds() - epochMillis
    if (elapsed < 0) return "—"
    val seconds = elapsed / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        days > 0 -> "${days}d ${hours % 24}h ago"
        hours > 0 -> "${hours}h ${minutes % 60}m ago"
        minutes > 0 -> "${minutes}m ${seconds % 60}s ago"
        else -> "${seconds}s ago"
    }
}

@Composable
private fun InfoStat(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TodoContent(todos: List<TodoInfo>) {
    if (todos.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painterResource(clipboardListIcon),
                    null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "No tasks",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tasks appear when the AI creates a plan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }
    val statusOrder = mapOf("in_progress" to 0, "pending" to 1, "completed" to 2, "cancelled" to 3)
    val sorted = todos.sortedWith(compareBy { statusOrder[it.status] ?: 99 })

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        items(sorted, key = { "${it.content}_${it.status}" }) { todo ->
            TodoRow(todo)
        }
    }
}

@Composable
private fun TodoRow(todo: TodoInfo) {
    val isComplete = todo.status == "completed" || todo.status == "cancelled"
    val textColor = if (isComplete) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            when (todo.status) {
                "completed" -> Text(
                    "\u2713",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF4CAF50),
                )
                "cancelled" -> Text(
                    "\u2715",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFEF4444),
                )
                "in_progress" -> CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                else -> Box(
                    Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(2.dp)),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            todo.content,
            style = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = if (isComplete) TextDecoration.LineThrough else TextDecoration.None,
            ),
            color = textColor,
            modifier = Modifier.weight(1f),
        )
    }
}
