package page.dera.opencode.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownTextTest {
    private val palette = MdPalette(codeBg = Color.Gray, linkColor = Color.Blue, quoteColor = Color.DarkGray)

    private fun segments(md: String) = markdownSegments(md, palette, cacheable = false)

    @Test
    fun plainTextIsSingleTextBlock() {
        val segs = segments("hello world")
        assertEquals(1, segs.size)
        val block = segs[0] as MdSegment.TextBlock
        assertEquals("hello world", block.text.text)
    }

    @Test
    fun paragraphsAreSeparatedByBlankLine() {
        val block = segments("one\n\ntwo")[0] as MdSegment.TextBlock
        assertEquals("one\n\ntwo", block.text.text)
    }

    @Test
    fun boldGetsBoldSpan() {
        val block = segments("a **bold** word")[0] as MdSegment.TextBlock
        assertEquals("a bold word", block.text.text)
        assertTrue(block.text.spanStyles.any { it.item.fontWeight == FontWeight.Bold && block.text.text.substring(it.start, it.end) == "bold" })
    }

    @Test
    fun inlineCodeKeepsTextWithoutBackticks() {
        val block = segments("use `foo()` here")[0] as MdSegment.TextBlock
        assertEquals("use foo() here", block.text.text)
    }

    @Test
    fun headingRendersWithoutHashes() {
        val block = segments("# Title\n\nbody")[0] as MdSegment.TextBlock
        assertEquals("Title\n\nbody", block.text.text)
    }

    @Test
    fun fencedCodeBecomesCodeSegment() {
        val segs = segments("before\n\n```kotlin\nval x = 1\n```\n\nafter")
        assertEquals(3, segs.size)
        val code = segs[1] as MdSegment.CodeBlock
        assertEquals("kotlin", code.lang)
        assertEquals("val x = 1", code.code)
        assertEquals("after", (segs[2] as MdSegment.TextBlock).text.text)
    }

    @Test
    fun bulletListRendersBullets() {
        val block = segments("- one\n- two")[0] as MdSegment.TextBlock
        assertEquals("•  one\n•  two", block.text.text)
    }

    @Test
    fun orderedListKeepsNumbers() {
        val block = segments("1. one\n2. two")[0] as MdSegment.TextBlock
        assertEquals("1. one\n2. two", block.text.text)
    }

    @Test
    fun tableBecomesTableSegment() {
        val segs = segments("| a | b |\n|---|---|\n| 1 | 2 |")
        assertEquals(1, segs.size)
        val table = segs[0] as MdSegment.Table
        assertEquals(listOf("a", "b"), table.header.map { it.text })
        assertEquals(listOf(listOf("1", "2")), table.rows.map { r -> r.map { it.text } })
    }

    @Test
    fun linkKeepsLabelText() {
        val block = segments("see [docs](https://example.com) now")[0] as MdSegment.TextBlock
        assertEquals("see docs now", block.text.text)
    }

    @Test
    fun streamingPrefixDoesNotCrash() {
        // Simulates partial markdown arriving during streaming
        val segs = segments("some **unclosed\n\n```koт")
        assertTrue(segs.isNotEmpty())
    }
}
