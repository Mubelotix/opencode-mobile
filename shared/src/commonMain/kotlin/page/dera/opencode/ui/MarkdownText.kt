package page.dera.opencode.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser

// Lightweight markdown rendering: parse once into AnnotatedString segments, then
// composition is just Text() calls. Replaces the widget-tree-per-block renderer,
// which was too slow to compose within a frame while scrolling.

internal data class MdPalette(
    val codeBg: Color,
    val linkColor: Color,
    val quoteColor: Color,
)

internal sealed interface MdSegment {
    data class TextBlock(val text: AnnotatedString) : MdSegment
    data class CodeBlock(val code: String, val lang: String?) : MdSegment
    data class Table(val header: List<AnnotatedString>, val rows: List<List<AnnotatedString>>) : MdSegment
}

private val markdownFlavour = GFMFlavourDescriptor()

// Keyed by content+palette so themed strings survive item disposal in the lazy list.
// Streaming (still-growing) text bypasses this cache to avoid filling it with prefixes.
private val segmentCache = HashMap<String, List<MdSegment>>()
private const val SEGMENT_CACHE_MAX = 300

internal fun markdownSegments(text: String, palette: MdPalette, cacheable: Boolean): List<MdSegment> {
    if (!cacheable) {
        return Perf.timed("md-parse len=${text.length} streaming", thresholdMs = 1.0) {
            parseMarkdownSegments(text, palette)
        }
    }
    val key = "${palette.hashCode()}:$text"
    segmentCache[key]?.let { return it }
    if (segmentCache.size >= SEGMENT_CACHE_MAX) segmentCache.clear()
    return Perf.timed("md-parse len=${text.length} cache-miss") {
        parseMarkdownSegments(text, palette)
    }.also { segmentCache[key] = it }
}

private fun parseMarkdownSegments(src: String, palette: MdPalette): List<MdSegment> {
    val tree = try {
        MarkdownParser(markdownFlavour).buildMarkdownTreeFromString(src)
    } catch (_: Exception) {
        return listOf(MdSegment.TextBlock(AnnotatedString(src)))
    }
    val out = mutableListOf<MdSegment>()
    var builder: AnnotatedString.Builder? = null

    fun flush() {
        builder?.let {
            val s = it.toAnnotatedString().trimTrailingNewlines()
            if (s.text.isNotBlank()) out.add(MdSegment.TextBlock(s))
        }
        builder = null
    }

    for (child in tree.children) {
        when (child.type) {
            MarkdownElementTypes.CODE_FENCE -> { flush(); out.add(parseCodeFence(child, src)) }
            MarkdownElementTypes.CODE_BLOCK -> { flush(); out.add(parseIndentedCode(child, src)) }
            GFMElementTypes.TABLE -> { flush(); out.add(parseTable(child, src, palette)) }
            else -> {
                val b = builder ?: AnnotatedString.Builder().also { builder = it }
                b.renderBlock(child, src, palette)
            }
        }
    }
    flush()
    return out
}

private fun AnnotatedString.trimTrailingNewlines(): AnnotatedString {
    var s = this
    while (s.text.endsWith('\n')) s = s.subSequence(0, s.text.length - 1)
    return s
}

private fun parseCodeFence(node: ASTNode, src: String): MdSegment.CodeBlock {
    val lang = node.children.firstOrNull { it.type == MarkdownTokenTypes.FENCE_LANG }
        ?.getTextInNode(src)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
    val code = node.children
        .filter { it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT || it.type == MarkdownTokenTypes.EOL }
        .joinToString("") { it.getTextInNode(src) }
        .trim('\n')
    return MdSegment.CodeBlock(code, lang)
}

private fun parseIndentedCode(node: ASTNode, src: String): MdSegment.CodeBlock {
    val code = node.getTextInNode(src).toString()
        .lines().joinToString("\n") { it.removePrefix("    ").removePrefix("\t") }
        .trim('\n')
    return MdSegment.CodeBlock(code, null)
}

private fun parseTable(node: ASTNode, src: String, palette: MdPalette): MdSegment.Table {
    fun rowCells(row: ASTNode): List<AnnotatedString> = row.children
        .filter { it.type == GFMTokenTypes.CELL }
        .map { AnnotatedString(it.getTextInNode(src).toString().trim()) }

    var header: List<AnnotatedString> = emptyList()
    val rows = mutableListOf<List<AnnotatedString>>()
    for (child in node.children) {
        when (child.type) {
            GFMElementTypes.HEADER -> header = rowCells(child)
            GFMElementTypes.ROW -> rowCells(child).takeIf { it.isNotEmpty() }?.let { rows.add(it) }
        }
    }
    return MdSegment.Table(header, rows)
}

private fun AnnotatedString.Builder.ensureBlockGap() {
    if (length == 0) return
    val t = toAnnotatedString().text
    if (!t.endsWith("\n\n")) append(if (t.endsWith('\n')) "\n" else "\n\n")
}

private fun AnnotatedString.Builder.renderBlock(node: ASTNode, src: String, p: MdPalette, listIndent: Int = 0) {
    when (node.type) {
        MarkdownElementTypes.PARAGRAPH -> {
            ensureBlockGap()
            renderInlineChildren(node, src, p)
        }
        MarkdownElementTypes.ATX_1, MarkdownElementTypes.ATX_2, MarkdownElementTypes.ATX_3,
        MarkdownElementTypes.ATX_4, MarkdownElementTypes.ATX_5, MarkdownElementTypes.ATX_6,
        MarkdownElementTypes.SETEXT_1, MarkdownElementTypes.SETEXT_2 -> {
            ensureBlockGap()
            val fontSize = when (node.type) {
                MarkdownElementTypes.ATX_1, MarkdownElementTypes.SETEXT_1 -> 20.sp
                MarkdownElementTypes.ATX_2, MarkdownElementTypes.SETEXT_2 -> 18.sp
                else -> 16.sp
            }
            val content = node.children.firstOrNull {
                it.type == MarkdownTokenTypes.ATX_CONTENT || it.type == MarkdownTokenTypes.SETEXT_CONTENT
            }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = fontSize)) {
                if (content != null) {
                    // ATX content starts with the whitespace token after '#'
                    val children = content.children.dropWhile { it.type == MarkdownTokenTypes.WHITE_SPACE }
                    if (children.isNotEmpty()) {
                        for (c in children) renderInline(c, src, p)
                    } else {
                        append(content.getTextInNode(src).toString().trim())
                    }
                } else {
                    append(node.getTextInNode(src).toString().trim('#', ' ', '=', '-', '\n'))
                }
            }
        }
        MarkdownElementTypes.UNORDERED_LIST, MarkdownElementTypes.ORDERED_LIST -> {
            if (listIndent == 0) ensureBlockGap()
            renderList(node, src, p, listIndent)
        }
        MarkdownElementTypes.BLOCK_QUOTE -> {
            ensureBlockGap()
            val start = length
            for (c in node.children) {
                when (c.type) {
                    MarkdownTokenTypes.BLOCK_QUOTE, MarkdownTokenTypes.WHITE_SPACE, MarkdownTokenTypes.EOL -> {}
                    else -> renderBlock(c, src, p, listIndent)
                }
            }
            addStyle(SpanStyle(fontStyle = FontStyle.Italic, color = p.quoteColor), start, length)
        }
        MarkdownTokenTypes.HORIZONTAL_RULE -> {
            ensureBlockGap()
            append("───")
        }
        MarkdownTokenTypes.EOL, MarkdownTokenTypes.WHITE_SPACE -> {}
        else -> {
            if (node.children.isEmpty()) {
                append(node.getTextInNode(src).toString())
            } else {
                ensureBlockGap()
                renderInlineChildren(node, src, p)
            }
        }
    }
}

private fun AnnotatedString.Builder.renderList(node: ASTNode, src: String, p: MdPalette, indent: Int) {
    for (item in node.children) {
        if (item.type != MarkdownElementTypes.LIST_ITEM) continue
        val t = toAnnotatedString().text
        if (t.isNotEmpty() && !t.endsWith('\n')) append('\n')
        append("    ".repeat(indent))
        val marker = item.children.firstOrNull {
            it.type == MarkdownTokenTypes.LIST_BULLET || it.type == MarkdownTokenTypes.LIST_NUMBER
        }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = p.linkColor)) {
            if (marker?.type == MarkdownTokenTypes.LIST_NUMBER) {
                append(marker.getTextInNode(src).toString().trim() + " ")
            } else {
                append("•  ")
            }
        }
        for (c in item.children) {
            when (c.type) {
                MarkdownTokenTypes.LIST_BULLET, MarkdownTokenTypes.LIST_NUMBER,
                MarkdownTokenTypes.WHITE_SPACE, MarkdownTokenTypes.EOL -> {}
                MarkdownElementTypes.PARAGRAPH -> renderInlineChildren(c, src, p)
                MarkdownElementTypes.UNORDERED_LIST, MarkdownElementTypes.ORDERED_LIST ->
                    renderList(c, src, p, indent + 1)
                else -> renderBlock(c, src, p, indent)
            }
        }
    }
}

private fun AnnotatedString.Builder.renderInlineChildren(node: ASTNode, src: String, p: MdPalette) {
    for (c in node.children) renderInline(c, src, p)
}

private fun AnnotatedString.Builder.renderInline(node: ASTNode, src: String, p: MdPalette) {
    when (node.type) {
        MarkdownElementTypes.STRONG -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            for (c in node.children) if (c.type != MarkdownTokenTypes.EMPH) renderInline(c, src, p)
        }
        MarkdownElementTypes.EMPH -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            for (c in node.children) if (c.type != MarkdownTokenTypes.EMPH) renderInline(c, src, p)
        }
        GFMElementTypes.STRIKETHROUGH -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
            for (c in node.children) if (c.type != GFMTokenTypes.TILDE) renderInline(c, src, p)
        }
        MarkdownElementTypes.CODE_SPAN -> withStyle(
            SpanStyle(fontFamily = FontFamily.Monospace, background = p.codeBg)
        ) {
            for (c in node.children) if (c.type != MarkdownTokenTypes.BACKTICK) append(c.getTextInNode(src).toString())
        }
        MarkdownElementTypes.INLINE_LINK,
        MarkdownElementTypes.FULL_REFERENCE_LINK,
        MarkdownElementTypes.SHORT_REFERENCE_LINK -> {
            val label = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT }
            val dest = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_DESTINATION }
                ?.getTextInNode(src)?.toString()
            val renderLabel: AnnotatedString.Builder.() -> Unit = {
                if (label != null) {
                    for (c in label.children) {
                        if (c.type != MarkdownTokenTypes.LBRACKET && c.type != MarkdownTokenTypes.RBRACKET) {
                            renderInline(c, src, p)
                        }
                    }
                } else {
                    append(node.getTextInNode(src).toString())
                }
            }
            if (dest != null) {
                withLink(LinkAnnotation.Url(dest, linkStyles(p))) { renderLabel() }
            } else {
                withStyle(linkSpan(p)) { renderLabel() }
            }
        }
        MarkdownElementTypes.AUTOLINK -> {
            val url = node.getTextInNode(src).toString().removeSurrounding("<", ">")
            withLink(LinkAnnotation.Url(url, linkStyles(p))) { append(url) }
        }
        GFMTokenTypes.GFM_AUTOLINK -> {
            val url = node.getTextInNode(src).toString()
            withLink(LinkAnnotation.Url(url, linkStyles(p))) { append(url) }
        }
        MarkdownElementTypes.IMAGE -> {
            withStyle(linkSpan(p)) { append(node.getTextInNode(src).toString()) }
        }
        MarkdownTokenTypes.EOL, MarkdownTokenTypes.HARD_LINE_BREAK -> append('\n')
        else -> {
            if (node.children.isEmpty()) {
                append(node.getTextInNode(src).toString())
            } else {
                renderInlineChildren(node, src, p)
            }
        }
    }
}

private fun linkSpan(p: MdPalette) = SpanStyle(color = p.linkColor, textDecoration = TextDecoration.Underline)
private fun linkStyles(p: MdPalette) = TextLinkStyles(style = linkSpan(p))

@Composable
internal fun MarkdownText(text: String, cacheable: Boolean, color: Color = Color.Unspecified) {
    val palette = MdPalette(
        codeBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        linkColor = MaterialTheme.colorScheme.primary,
        quoteColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val segments = remember(text, palette) { markdownSegments(text, palette, cacheable) }
    Column {
        segments.forEachIndexed { idx, seg ->
            when (seg) {
                is MdSegment.TextBlock -> Text(seg.text, style = MaterialTheme.typography.bodyMedium, color = color)
                is MdSegment.CodeBlock -> MarkdownCodeBlock(seg)
                is MdSegment.Table -> MarkdownTable(seg)
            }
            if (idx < segments.lastIndex) Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun MarkdownCodeBlock(seg: MdSegment.CodeBlock) {
    val displayCode = remember(seg) {
        if (seg.code.length > 3000) seg.code.take(3000) + "\n… (truncated)" else seg.code
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E2E))
            .padding(10.dp),
    ) {
        Column {
            if (!seg.lang.isNullOrBlank()) {
                Text(
                    seg.lang,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA6ADC8),
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Text(
                displayCode,
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color(0xFFCDD6F4)),
            )
        }
    }
}

@Composable
private fun MarkdownTable(seg: MdSegment.Table) {
    val numCols = remember(seg) { maxOf(seg.header.size, seg.rows.maxOfOrNull { it.size } ?: 0) }
    if (numCols == 0) return
    val colWidths = remember(seg) {
        List(numCols) { col ->
            val maxLen = (listOf(seg.header) + seg.rows).maxOfOrNull { it.getOrNull(col)?.text?.length ?: 0 } ?: 0
            (maxLen * 9f + 24f).coerceIn(100f, 260f).dp
        }
    }
    val hScroll = rememberScrollState()
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Box(Modifier.horizontalScroll(hScroll)) {
            Column {
                if (seg.header.isNotEmpty()) {
                    Row(Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))) {
                        for (col in 0 until numCols) {
                            Box(
                                Modifier.width(colWidths[col]).padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Text(
                                    seg.header.getOrNull(col) ?: AnnotatedString(""),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }
                seg.rows.forEachIndexed { rowIdx, row ->
                    val bg = if (rowIdx % 2 == 0) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    Row(Modifier.background(bg)) {
                        for (col in 0 until numCols) {
                            Box(
                                Modifier.width(colWidths[col]).padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Text(
                                    row.getOrNull(col) ?: AnnotatedString(""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
