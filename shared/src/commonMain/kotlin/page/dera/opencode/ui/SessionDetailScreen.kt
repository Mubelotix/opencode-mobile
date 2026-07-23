package page.dera.opencode.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import org.commonmark.parser.Parser
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text as CommonMarkText
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.ThematicBreak
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TableCell
import opencodeapp.shared.generated.resources.globeIcon
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
import opencodeapp.shared.generated.resources.gitBranchIcon
import opencodeapp.shared.generated.resources.listChecksIcon
import opencodeapp.shared.generated.resources.menuIcon
import opencodeapp.shared.generated.resources.messageSquareIcon
import opencodeapp.shared.generated.resources.terminalIcon
import org.jetbrains.compose.resources.painterResource

private val prettyJson = Json { prettyPrint = true }
private val urlHostRegex = Regex("://([^/]+)")

private val commonmarkParser = Parser.builder()
    .extensions(listOf(TablesExtension.create()))
    .build()

private fun parseCommonMarkToAnnotatedString(
    markdown: String,
    codeBg: Color,
    linkColor: Color,
    quoteColor: Color
): AnnotatedString {
    val document = commonmarkParser.parse(markdown)
    return buildAnnotatedString {
        renderCommonMarkNode(document, codeBg, linkColor, quoteColor, isInsideListItem = false)
    }
}

private fun AnnotatedString.Builder.endsWithNewline(): Boolean {
    val t = toAnnotatedString().text
    return t.isEmpty() || t.endsWith('\n')
}

private fun AnnotatedString.Builder.endsWithDoubleNewline(): Boolean {
    val t = toAnnotatedString().text
    return t.endsWith("\n\n")
}

private fun collectTextContent(node: Node, sb: StringBuilder) {
    var child = node.firstChild
    while (child != null) {
        if (child is CommonMarkText) {
            sb.append(child.literal)
        } else if (child is Code) {
            sb.append(child.literal)
        } else {
            collectTextContent(child, sb)
        }
        child = child.next
    }
}

private fun AnnotatedString.Builder.renderCommonMarkNode(
    node: Node,
    codeBg: Color,
    linkColor: Color,
    quoteColor: Color,
    isInsideListItem: Boolean = false
) {
    when (node) {
        is Paragraph -> {
            if (!isInsideListItem) {
                if (length > 0 && !endsWithNewline()) append("\n")
                renderChildren(node, codeBg, linkColor, quoteColor, isInsideListItem = false)
                append("\n\n")
            } else {
                renderChildren(node, codeBg, linkColor, quoteColor, isInsideListItem = true)
            }
        }
        is Heading -> {
            if (length > 0) {
                if (!endsWithNewline()) append("\n\n") else if (!endsWithDoubleNewline()) append("\n")
            }
            val start = length
            renderChildren(node, codeBg, linkColor, quoteColor, isInsideListItem)
            val fontSize = when (node.level) {
                1 -> 20.sp
                2 -> 18.sp
                else -> 16.sp
            }
            addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = fontSize), start, length)
            append("\n\n")
        }
        is CommonMarkText -> {
            append(node.literal)
        }
        is StrongEmphasis -> {
            val start = length
            renderChildren(node, codeBg, linkColor, quoteColor, isInsideListItem)
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
        }
        is Emphasis -> {
            val start = length
            renderChildren(node, codeBg, linkColor, quoteColor, isInsideListItem)
            addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
        }
        is Code -> {
            val start = length
            append(node.literal)
            addStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg), start, length)
        }
        is Link -> {
            val start = length
            renderChildren(node, codeBg, linkColor, quoteColor, isInsideListItem)
            addStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline), start, length)
        }
        is BulletList -> {
            if (length > 0 && !endsWithNewline()) append("\n")
            var item = node.firstChild
            while (item != null) {
                if (item is ListItem) {
                    if (length > 0 && !endsWithNewline()) append("\n")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = linkColor)) {
                        append("• ")
                    }
                    renderChildren(item, codeBg, linkColor, quoteColor, isInsideListItem = true)
                }
                item = item.next
            }
            append("\n\n")
        }
        is OrderedList -> {
            if (length > 0 && !endsWithNewline()) append("\n")
            var num = node.startNumber
            var item = node.firstChild
            while (item != null) {
                if (item is ListItem) {
                    if (length > 0 && !endsWithNewline()) append("\n")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = linkColor)) {
                        append("$num. ")
                    }
                    num++
                    renderChildren(item, codeBg, linkColor, quoteColor, isInsideListItem = true)
                }
                item = item.next
            }
            append("\n\n")
        }
        is ListItem -> {
            renderChildren(node, codeBg, linkColor, quoteColor, isInsideListItem = true)
        }
        is BlockQuote -> {
            if (length > 0 && !endsWithNewline()) append("\n")
            val start = length
            renderChildren(node, codeBg, linkColor, quoteColor, isInsideListItem)
            addStyle(SpanStyle(fontStyle = FontStyle.Italic, color = quoteColor), start, length)
        }
        is TableBlock -> {
            // TableBlock is handled by MarkdownTableComposable
        }
        is SoftLineBreak -> {
            append("\n")
        }
        is HardLineBreak -> {
            append("\n")
        }
        is ThematicBreak -> {
            if (length > 0 && !endsWithNewline()) append("\n")
            append("───\n")
        }
        else -> {
            renderChildren(node, codeBg, linkColor, quoteColor, isInsideListItem)
        }
    }
}

private fun AnnotatedString.Builder.renderChildren(
    node: Node,
    codeBg: Color,
    linkColor: Color,
    quoteColor: Color,
    isInsideListItem: Boolean
) {
    var child = node.firstChild
    while (child != null) {
        val next = child.next
        renderCommonMarkNode(child, codeBg, linkColor, quoteColor, isInsideListItem)
        child = next
    }
}

@Composable
private fun MarkdownTableComposable(
    tableBlock: TableBlock,
    codeBg: Color,
    linkColor: Color,
    quoteColor: Color
) {
    val headerRows = remember(tableBlock) {
        mutableListOf<List<Node>>().apply {
            var curr = tableBlock.firstChild
            while (curr != null) {
                if (curr is TableHead) {
                    var r = curr.firstChild
                    while (r != null) {
                        if (r is TableRow) {
                            val cells = mutableListOf<Node>()
                            var c = r.firstChild
                            while (c != null) {
                                if (c is TableCell) cells.add(c)
                                c = c.next
                            }
                            add(cells)
                        }
                        r = r.next
                    }
                }
                curr = curr.next
            }
        }
    }
    val bodyRows = remember(tableBlock) {
        mutableListOf<List<Node>>().apply {
            var curr = tableBlock.firstChild
            while (curr != null) {
                if (curr is TableBody) {
                    var r = curr.firstChild
                    while (r != null) {
                        if (r is TableRow) {
                            val cells = mutableListOf<Node>()
                            var c = r.firstChild
                            while (c != null) {
                                if (c is TableCell) cells.add(c)
                                c = c.next
                            }
                            add(cells)
                        }
                        r = r.next
                    }
                }
                curr = curr.next
            }
        }
    }

    val numCols = remember(headerRows, bodyRows) {
        (headerRows + bodyRows).maxOfOrNull { it.size } ?: 0
    }

    if (numCols == 0) return

    val colWidthsDp = remember(headerRows, bodyRows, numCols) {
        val allRows = headerRows + bodyRows
        Array(numCols) { colIdx ->
            val maxLen = allRows.maxOfOrNull { row ->
                val cellNode = row.getOrNull(colIdx)
                if (cellNode != null) {
                    val sb = StringBuilder()
                    collectTextContent(cellNode, sb)
                    sb.length
                } else 0
            } ?: 0
            (maxLen * 9f + 24f).coerceIn(100f, 260f).dp
        }
    }

    val horizontalScrollState = rememberScrollState()

    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Box(Modifier.horizontalScroll(horizontalScrollState)) {
            Column {
                headerRows.forEach { row ->
                    Row(
                        Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                            .padding(vertical = 2.dp)
                    ) {
                        for (colIdx in 0 until numCols) {
                            val cellNode = row.getOrNull(colIdx)
                            val cellText = remember(cellNode) {
                                if (cellNode != null) {
                                    buildAnnotatedString {
                                        renderCommonMarkNode(cellNode, codeBg, linkColor, quoteColor, true)
                                    }
                                } else AnnotatedString("")
                            }
                            Box(
                                Modifier
                                    .width(colWidthsDp[colIdx])
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    cellText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                bodyRows.forEachIndexed { rowIdx, row ->
                    val bg = if (rowIdx % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    Row(
                        Modifier
                            .background(bg)
                            .padding(vertical = 2.dp)
                    ) {
                        for (colIdx in 0 until numCols) {
                            val cellNode = row.getOrNull(colIdx)
                            val cellText = remember(cellNode) {
                                if (cellNode != null) {
                                    buildAnnotatedString {
                                        renderCommonMarkNode(cellNode, codeBg, linkColor, quoteColor, true)
                                    }
                                } else AnnotatedString("")
                            }
                            Box(
                                Modifier
                                    .width(colWidthsDp[colIdx])
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    cellText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeBlockComposable(
    code: String,
    info: String?
) {
    val displayCode = remember(code) {
        if (code.length > 3000) code.take(3000) + "\n... (truncated for preview)" else code
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E2E))
            .padding(10.dp)
    ) {
        Column {
            if (!info.isNullOrBlank()) {
                Text(
                    info.trim(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                displayCode,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = Color(0xFFCDD6F4)
                )
            )
        }
    }
}

private sealed class DocumentSegment {
    data class TextSegment(val annotated: AnnotatedString) : DocumentSegment()
    data class CodeSegment(val code: String, val info: String?) : DocumentSegment()
    data class TableSegment(val block: TableBlock) : DocumentSegment()
}

private val commonmarkSegmentCache = mutableMapOf<String, List<DocumentSegment>>()

@Composable
private fun CommonMarkDocumentContent(
    markdown: String,
    codeBg: Color,
    linkColor: Color,
    quoteColor: Color,
    textColor: Color
) {
    val segments = remember(markdown, codeBg, linkColor, quoteColor) {
        val cacheKey = "${markdown.hashCode()}_${codeBg.hashCode()}_${linkColor.hashCode()}"
        commonmarkSegmentCache.getOrPut(cacheKey) {
            val document = commonmarkParser.parse(markdown)
            val result = mutableListOf<DocumentSegment>()
            var child = document.firstChild
            var pendingBuilder: AnnotatedString.Builder? = null

            fun flushText() {
                pendingBuilder?.let { b ->
                    var res = b.toAnnotatedString()
                    while (res.text.endsWith('\n')) {
                        res = res.subSequence(0, res.text.length - 1)
                    }
                    if (res.text.isNotBlank()) {
                        result.add(DocumentSegment.TextSegment(res))
                    }
                    pendingBuilder = null
                }
            }

            while (child != null) {
                when (child) {
                    is TableBlock -> {
                        flushText()
                        result.add(DocumentSegment.TableSegment(child))
                    }
                    is FencedCodeBlock -> {
                        flushText()
                        result.add(DocumentSegment.CodeSegment(child.literal.trimEnd(), child.info))
                    }
                    is IndentedCodeBlock -> {
                        flushText()
                        result.add(DocumentSegment.CodeSegment(child.literal.trimEnd(), null))
                    }
                    else -> {
                        val b = pendingBuilder ?: AnnotatedString.Builder().also { pendingBuilder = it }
                        b.renderCommonMarkNode(child, codeBg, linkColor, quoteColor, isInsideListItem = false)
                    }
                }
                child = child.next
            }
            flushText()
            result
        }
    }

    Column {
        segments.forEachIndexed { idx, seg ->
            when (seg) {
                is DocumentSegment.TextSegment -> {
                    Text(seg.annotated, style = MaterialTheme.typography.bodyMedium, color = textColor)
                }
                is DocumentSegment.CodeSegment -> {
                    CodeBlockComposable(seg.code, seg.info)
                }
                is DocumentSegment.TableSegment -> {
                    MarkdownTableComposable(seg.block, codeBg, linkColor, quoteColor)
                }
            }
            if (idx < segments.lastIndex) {
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

private data class ChatPainters(
    val bot: Painter,
    val folder: Painter,
    val file: Painter,
    val search: Painter,
    val question: Painter,
    val globe: Painter,
    val terminal: Painter,
    val invalid: Painter,
    val brain: Painter,
    val alert: Painter
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(api: OpencodeApi, sessionId: String, sessionTitle: String, directory: String, projectRoot: String, onBack: () -> Unit, onMenuClick: (() -> Unit)? = null, onTitleChange: ((String) -> Unit)? = null) {
    val messages = remember { mutableStateListOf<LegacyWithParts>() }
    val listState = rememberLazyListState()
    val displayedMessages = messages
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var sessionTokens by remember { mutableStateOf<SessionTokens?>(null) }
    var sessionCost by remember { mutableStateOf<Double?>(null) }
    var currentTitle by remember { mutableStateOf(sessionTitle) }
    var isEditingTitle by remember { mutableStateOf(false) }
    var editTitleText by remember { mutableStateOf(currentTitle) }
    var titleWasFocused by remember { mutableStateOf(false) }
    val titleFocusRequester = remember { FocusRequester() }

    val chatPainters = ChatPainters(
        bot = painterResource(botIcon),
        folder = painterResource(Res.drawable.folder),
        file = painterResource(Res.drawable.file),
        search = painterResource(searchIcon),
        question = painterResource(circleQuestionMarkIcon),
        globe = painterResource(globeIcon),
        terminal = painterResource(Res.drawable.terminal),
        invalid = painterResource(circleXIcon),
        brain = painterResource(brainIcon),
        alert = painterResource(Res.drawable.triangle_alert)
    )
    val markdownCache = remember { mutableMapOf<String, com.mikepenz.markdown.model.MarkdownState>() }

    LaunchedEffect(isEditingTitle) {
        if (isEditingTitle) {
            editTitleText = currentTitle
            titleWasFocused = false
            titleFocusRequester.requestFocus()
        }
    }
    val streamingText = remember { mutableStateMapOf<String, String>() }
    val permissionRequests = remember { mutableStateListOf<PermissionRequest>() }
    val questionRequests = remember { mutableStateListOf<QuestionRequest>() }
    var selectedTab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(sessionId) {
        isLoading = true
        permissionRequests.clear()
        api.getSessionMessages(sessionId).onSuccess { messages.clear(); messages.addAll(it) }.onFailure { errorText = it.message }
        isLoading = false
    }

    var prevMessageCount by remember { mutableStateOf(0) }
    LaunchedEffect(messages.size, isSending, isRunning) {
        if (messages.isNotEmpty()) {
            if (prevMessageCount == 0 || messages.size > prevMessageCount) {
                val totalItems = messages.size + if (isSending || isRunning) 1 else 0
                if (totalItems > 0) {
                    listState.scrollToItem(totalItems - 1)
                }
            }
        }
        prevMessageCount = messages.size
    }

    LaunchedEffect(sessionId, directory) {
        val pendingDeltas = mutableMapOf<String, StringBuilder>()
        var flushJob: Job? = null
        fun flushDeltas() {
            pendingDeltas.forEach { (k, sb) -> streamingText[k] = (streamingText[k] ?: "") + sb.toString() }
            pendingDeltas.clear()
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
                }
            }
        } catch (_: Exception) { }
    }

    Scaffold(
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
                        val subtitles = mutableListOf<String>()
                        val totalTokens = (sessionTokens?.input ?: 0) + (sessionTokens?.output ?: 0) + (sessionTokens?.reasoning ?: 0) + (sessionTokens?.cache?.read ?: 0) + (sessionTokens?.cache?.write ?: 0)
                        if (totalTokens > 0) subtitles.add(formatTokenCount(totalTokens))
                        sessionCost?.let { subtitles.add(formatCost(it)) }
                        if (subtitles.isNotEmpty()) {
                            Text(subtitles.joinToString("  ·  "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
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
                    Triple(gitBranchIcon, "Git Diff", 1),
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
            if (selectedTab == 0) {
                Column(
                    Modifier.fillMaxSize()
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
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
                        ) {
                                items(
                                    items = displayedMessages,
                                    key = { it.info.id },
                                    contentType = { it.info.role }
                                ) { msg ->
                                    MessageCard(msg, streamingText, projectRoot, chatPainters, markdownCache)
                                }
                                if (isSending || isRunning) {
                                    item(key = "sending_indicator", contentType = "indicator") {
                                        SendingIndicator()
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
                Row(Modifier.fillMaxWidth().imePadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom) {
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
                }}
            if (selectedTab != 0) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                    when (selectedTab) {
                        1 -> "Git Diff"
                        2 -> "Todo"
                        3 -> "Terminal"
                        4 -> "Info"
                        5 -> "Browser"
                        else -> ""
                    },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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

@Composable private fun ReasoningPart(part: LegacyPart, streamingText: Map<String, String>, chatPainters: ChatPainters) {
    val key = remember(part.messageID, part.id) { "${part.messageID}:${part.id}" }
    val isStreaming = streamingText.containsKey(key)
    val t = remember(streamingText[key], part.text) { streamingText[key] ?: (part.text ?: "") }
    var expanded by remember { mutableStateOf(false) }
    val headerText = remember(isStreaming) {
        if (isStreaming) "Thinking..." else "Thought"
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable { expanded = !expanded },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(chatPainters.brain, null, Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(headerText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (expanded && t.isNotBlank()) {
        Box(
            Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)).padding(8.dp)
        ) {
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
private fun MessageCard(msg: LegacyWithParts, streamingText: Map<String, String>, projectRoot: String, chatPainters: ChatPainters, markdownCache: MutableMap<String, com.mikepenz.markdown.model.MarkdownState>) {
    val parts = remember(msg.parts) { msg.parts.filter { it.type !in listOf("step-start", "step-finish", "snapshot", "patch") } }
    when (msg.info.role) {
        "user" -> Row(
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            SelectionContainer {
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
        }
        "assistant" -> AssistantCard(msg.info, parts, streamingText, projectRoot, chatPainters, markdownCache)
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
private fun AssistantCard(info: LegacyInfo, parts: List<LegacyPart>, streamingText: Map<String, String>, projectRoot: String, chatPainters: ChatPainters, markdownCache: MutableMap<String, com.mikepenz.markdown.model.MarkdownState>) {
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
                    Image(chatPainters.alert, null, Modifier.size(22.dp))
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
        parts.forEach { part ->
            key(part.id) {
                PartContent(part, streamingText, projectRoot, chatPainters, markdownCache)
            }
        }
    }
}

@Composable
private fun PartContent(part: LegacyPart, streamingText: Map<String, String>, projectRoot: String, chatPainters: ChatPainters, markdownCache: MutableMap<String, com.mikepenz.markdown.model.MarkdownState>) {
    when (part.type) {
        "text" -> {
            val streamKey = "${part.messageID}:${part.id}"
            val streamed = streamingText[streamKey]
            val t = remember(part.text, streamed) { (streamed ?: (part.text ?: "")).trimEnd() }
            val codeBg = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
            val linkColor = MaterialTheme.colorScheme.primary
            val quoteColor = MaterialTheme.colorScheme.outline
            val textColor = MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                Modifier
                    .widthIn(max = 360.dp)
                    .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
            ) {
                SelectionContainer {
                    CommonMarkDocumentContent(t, codeBg, linkColor, quoteColor, textColor)
                }
            }
        }
        "reasoning" -> ReasoningPart(part, streamingText, chatPainters)
        "tool" -> ToolPart(part, projectRoot, chatPainters)
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
        "agent" -> AgentPart(part, chatPainters)
    }
}

@Composable private fun AgentPart(part: LegacyPart, chatPainters: ChatPainters) {
    val label = part.name?.let { "Launched $it" } ?: "Launched agent"
    val sub = part.description?.takeIf { it.isNotBlank() } ?: part.title?.takeIf { it.isNotBlank() }
    Row(Modifier.padding(vertical = 2.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(chatPainters.bot, null, Modifier.size(14.dp))
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
private fun ToolPart(part: LegacyPart, projectRoot: String, chatPainters: ChatPainters) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val status = part.state?.status
    val tc = remember(status, outlineColor, tertiaryColor, primaryColor, errorColor) {
        when (status) {
            "pending" -> outlineColor
            "running" -> tertiaryColor
            "completed" -> primaryColor
            "error" -> errorColor
            else -> outlineColor
        }
    }
    val label = remember(part.id) { toolLabel(part, projectRoot) }
    val matchSuffix = remember(part.id) { matchCount(part) }
    if (part.tool == "task") {
        val desc = remember(part.id) { taskDescription(part) }
        Row(
            Modifier
                .padding(vertical = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(chatPainters.bot, null, Modifier.size(14.dp))
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
                    Image(chatPainters.terminal, null, Modifier.size(14.dp))
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
        val isExpandable = part.tool in setOf("websearch", "webfetch", "question", "invalid", "read", "write", "edit")
        val isDirectory = remember(part.id) { isDirectoryRead(part) }
        val iconPainter = when {
            part.tool == "invalid" -> chatPainters.invalid
            part.tool == "read" && isDirectory -> chatPainters.folder
            part.tool in listOf("glob", "grep") -> chatPainters.search
            part.tool == "question" -> chatPainters.question
            part.tool in listOf("websearch", "webfetch") -> chatPainters.globe
            else -> chatPainters.file
        }
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(if (isExpandable) Modifier.clickable { expanded = !expanded } else Modifier)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(iconPainter, null, Modifier.size(14.dp))
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
                            val inputStr = remember(part.id) {
                                val raw = try { prettyJson.encodeToString(JsonElement.serializer(), inputObj) } catch (_: Exception) { "" }
                                if (raw.length > 3000) raw.take(3000) + "\n... (truncated)" else raw
                            }
                            Text(
                                inputStr,
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
                            val outputStr = remember(part.id) {
                                if (output.length > 3000) output.take(3000) + "\n... (truncated)" else output
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                outputStr,
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
        "bash" -> {
            val raw = obj["command"]?.jsonPrimitive?.contentOrNull ?: obj["command"]?.toString()?.removeSurrounding("\"")
            val firstLine = raw?.lines()?.firstOrNull { it.isNotBlank() }?.trim() ?: ""
            val singleLine = if (firstLine.length > 50) firstLine.take(50) + "..." else firstLine
            if (singleLine.isNotBlank()) "$ $singleLine" else "bash"
        }
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
