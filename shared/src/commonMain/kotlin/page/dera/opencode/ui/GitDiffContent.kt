package page.dera.opencode.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallatinapps.syntaxmp.compose.SyntaxTheme
import com.gallatinapps.syntaxmp.compose.rememberSyntaxAnnotatedString
import com.gallatinapps.syntaxmp.tokenizer.SyntaxTokenizer
import kotlinx.coroutines.launch
import page.dera.opencode.api.FileDiff
import page.dera.opencode.api.FsEntry
import page.dera.opencode.api.OpencodeApi
import page.dera.opencode.api.ReviewComment

private val AddBg = Color(0xFFE6FFEC)
private val DelBg = Color(0xFFFFEBE9)
private val AddFg = Color(0xFF1A7F37)
private val DelFg = Color(0xFFCF222E)
private val ModFg = Color(0xFF8250DF)
private val ContextFg = Color(0xFF6E7681)

private val mono12 = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)

private enum class DisplayLineType { NORMAL, ADDITION, DELETION }
private data class DisplayLine(val content: String, val type: DisplayLineType, val lineNumber: Int?)

private data class TreeEntry(
    val name: String, val path: String, val isDirectory: Boolean,
    val depth: Int, val isExpanded: Boolean,
    val hasChanges: Boolean,
    val additions: Int, val deletions: Int,
)

private data class FileViewState(
    val path: String, val content: String,
    val displayLines: List<DisplayLine>?,
    val stats: Pair<Int, Int>?,
)

private fun languageFromPath(filePath: String): String? {
    val name = filePath.substringAfterLast('/').lowercase()
    val ext = filePath.substringAfterLast('.', "").lowercase()
    if (ext == name) {
        return when (name) {
            "dockerfile" -> "dockerfile"; "makefile", "gnumakefile" -> "makefile"
            "jenkinsfile" -> "groovy"; "vagrantfile" -> "ruby"; "gemfile", "rakefile" -> "ruby"
            else -> null
        }
    }
    return when (ext) {
        "kt", "kts" -> "kotlin"; "java", "jav" -> "java"; "swift" -> "swift"
        "py", "py3", "pyw", "pyi" -> "python"; "js", "jsx", "mjs", "cjs" -> "javascript"
        "ts", "tsx", "mts", "cts" -> "typescript"; "json", "jsonc", "json5" -> "json"
        "xml", "svg", "xsd", "xsl", "xaml", "plist", "wsdl", "rss", "atom", "resx" -> "xml"
        "html", "htm", "xhtml", "shtml" -> "html"
        "css", "scss", "sass", "less", "styl" -> "css"
        "sh", "bash", "zsh", "fish", "ksh", "csh", "tcsh" -> "shell"
        "yaml", "yml" -> "yaml"; "md", "mdx", "markdown", "mkd", "mkdn", "mdown", "ronn" -> "markdown"
        "rs" -> "rust"; "go" -> "go"; "sql", "psql", "pgsql", "mysql", "sqlite", "sqlite3" -> "sql"
        "toml" -> "toml"; "gradle" -> "kotlin"; "c", "h" -> "c"
        "cpp", "cc", "cxx", "c++", "hpp", "hxx", "hh", "h++" -> "c++"
        "cs", "csx" -> "c#"; "rb", "rbw" -> "ruby"
        "php", "phtml", "php5", "php7", "phps", "phar" -> "php"
        "dart" -> "dart"; "ini", "cfg", "conf", "editorconfig", "gitattributes", "gitignore" -> "ini"
        "proto", "protobuf" -> "protobuf"; "graphql", "gql" -> "graphql"
        "csv", "tsv" -> "csv"; "env", "dotenv" -> "dotenv"
        "ps1", "psm1", "psd1" -> "powershell"; "properties" -> "ini"
        "dockerfile", "dockerignore" -> "dockerfile"; "makefile", "mk", "mak" -> "makefile"
        "diff", "patch" -> "diff"; "cmake", "cmake.in" -> "cmake"
        "groovy", "gvy", "gy", "gsh" -> "groovy"; "scala", "sc" -> "scala"
        "lua" -> "lua"; "r", "rmd", "rprofile" -> "r"
        "pl", "perl", "pm" -> "perl"; "hs", "lhs" -> "haskell"
        "elm" -> "elm"; "clj", "cljs", "cljc", "edn" -> "clojure"
        "erl", "hrl" -> "erlang"; "ex", "exs" -> "elixir"
        "ml", "mli" -> "ocaml"; "fs", "fsi", "fsx" -> "f#"
        "jl" -> "julia"; "nim" -> "nim"; "zig" -> "zig"
        "v", "vh" -> "verilog"; "vhd", "vhdl" -> "vhdl"
        "tf", "tfvars", "hcl" -> "terraform"; "vue", "svelte" -> "html"
        "twig", "jinja", "jinja2", "liquid" -> "html"; "lock" -> "toml"
        else -> null
    }
}

private fun splitAnnotatedString(annotated: AnnotatedString): List<AnnotatedString> {
    val text = annotated.text
    if (text.isEmpty()) return listOf(AnnotatedString(""))
    val result = mutableListOf<AnnotatedString>()
    var start = 0
    for (i in text.indices) {
        if (text[i] == '\n') {
            val end = i + 1
            if (end > start) {
                val sub = annotated.subSequence(start, end - 1)
                result.add(if (sub.text.isEmpty()) AnnotatedString("") else sub)
            } else result.add(AnnotatedString(""))
            start = end
        }
    }
    if (start < text.length) result.add(annotated.subSequence(start, text.length))
    if (result.isEmpty() && text.isNotEmpty()) result.add(annotated)
    return result
}

private fun buildDisplayLines(fileContent: String, patch: String): List<DisplayLine> {
    val fileLines = fileContent.lines()
    val addedLines = mutableSetOf<Int>()
    val insertions = mutableMapOf<Int, MutableList<String>>()

    val hdrRegex = Regex("""@@ -(\d+)(?:,\d+)? \+(\d+)(?:,\d+)? @@(?: .*)?""")
    var newLine = 0
    var inHunk = false
    var pendingDel = mutableListOf<String>()

    for (line in patch.lines()) {
        val hm = hdrRegex.find(line)
        if (hm != null) {
            if (pendingDel.isNotEmpty()) {
                newLine = hm.groupValues[2].toInt()
                insertions.getOrPut(newLine) { mutableListOf() }.addAll(pendingDel)
                pendingDel = mutableListOf()
            } else {
                newLine = hm.groupValues[2].toInt()
            }
            inHunk = true
            continue
        }
        if (!inHunk) continue
        when (line.firstOrNull()) {
            ' ' -> { if (pendingDel.isNotEmpty()) { insertions.getOrPut(newLine) { mutableListOf() }.addAll(pendingDel); pendingDel = mutableListOf() }; newLine++ }
            '+' -> { if (pendingDel.isNotEmpty()) { insertions.getOrPut(newLine) { mutableListOf() }.addAll(pendingDel); pendingDel = mutableListOf() }; addedLines.add(newLine); newLine++ }
            '-' -> { pendingDel.add(line.drop(1)) }
            else -> { if (pendingDel.isNotEmpty()) { insertions.getOrPut(newLine) { mutableListOf() }.addAll(pendingDel); pendingDel = mutableListOf() }; inHunk = false }
        }
    }
    if (pendingDel.isNotEmpty() && newLine > 0) {
        insertions.getOrPut(newLine) { mutableListOf() }.addAll(pendingDel)
    }

    val result = mutableListOf<DisplayLine>()
    for ((idx, line) in fileLines.withIndex()) {
        val ln = idx + 1
        insertions[ln]?.forEach { result.add(DisplayLine(it, DisplayLineType.DELETION, null)) }
        val type = if (addedLines.contains(ln)) DisplayLineType.ADDITION else DisplayLineType.NORMAL
        result.add(DisplayLine(line, type, ln))
    }
    insertions[fileLines.size + 1]?.forEach { result.add(DisplayLine(it, DisplayLineType.DELETION, null)) }
    return result
}

@Composable
fun GitDiffContent(api: OpencodeApi, directory: String, fileChangeTick: Int = 0, reviewComments: MutableList<ReviewComment> = mutableListOf()) {
    val scope = rememberCoroutineScope()
    val engine = remember { SyntaxTokenizer() }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val theme = remember(isDark) { if (isDark) SyntaxTheme.DefaultDark else SyntaxTheme.DefaultLight }

    var diffMap by remember { mutableStateOf<Map<String, FileDiff>>(emptyMap()) }
    var fileView by remember { mutableStateOf<FileViewState?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasChanges by remember { mutableStateOf(false) }
    var drawerOpen by remember { mutableStateOf(false) }
    var commentTarget by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var commentText by remember { mutableStateOf("") }

    val dirCache = remember { mutableStateMapOf<String, List<FsEntry>>() }
    val expandedDirs = remember { mutableStateListOf<String>() }
    val fileContentCache = remember { mutableStateMapOf<String, String>() }
    val reviewedFiles = remember { mutableStateListOf<String>() }

    val changedFiles by remember { derivedStateOf { diffMap.values.filter { it.status != null }.sortedBy { it.file } } }
    val unreviewedFiles by remember { derivedStateOf { changedFiles.filter { it.file !in reviewedFiles } } }
    val currentChangedIndex by remember { derivedStateOf { unreviewedFiles.indexOfFirst { it.file == fileView?.path } } }

    LaunchedEffect(directory) {
        isLoading = true; errorText = null
        api.getVcsDiff(directory).fold(
            onSuccess = { diffs -> diffMap = diffs.associateBy { it.file }; hasChanges = diffs.isNotEmpty() },
            onFailure = { e -> errorText = e.message ?: "Failed to load diff" },
        )
        api.listFiles(directory).fold(
            onSuccess = { r -> dirCache[""] = r.data.sortedWith(compareBy({ it.type != "directory" }, { it.name.startsWith(".") }, { it.name.lowercase() })) },
            onFailure = { e -> if (errorText == null) errorText = e.message ?: "Failed to load files" },
        )
        isLoading = false
    }

    LaunchedEffect(fileChangeTick) {
        if (fileChangeTick > 0) {
            val oldDiff = diffMap
            api.getVcsDiff(directory).fold(
                onSuccess = { diffs ->
                    val newDiff = diffs.associateBy { it.file }
                    diffMap = newDiff
                    hasChanges = diffs.isNotEmpty()
                    reviewedFiles.removeAll { path ->
                        val old = oldDiff[path]?.patch
                        val new = newDiff[path]?.patch
                        old != new
                    }
                },
                onFailure = { },
            )
        }
    }

    fun toggleDir(dirPath: String) {
        if (dirPath in expandedDirs) expandedDirs.remove(dirPath) else {
            expandedDirs.add(dirPath)
            if (dirPath !in dirCache) scope.launch {
                api.listFiles(directory, dirPath).onSuccess { r ->
                    dirCache[dirPath] = r.data.sortedWith(compareBy({ it.type != "directory" }, { it.name.startsWith(".") }, { it.name.lowercase() }))
                }
            }
        }
    }

    fun showFile(relPath: String, content: String, diffs: Map<String, FileDiff> = diffMap) {
        val diff = diffs[relPath]
        if (diff != null && diff.patch != null && diff.status != "deleted") {
            val dLines = buildDisplayLines(content, diff.patch!!)
            fileView = FileViewState(relPath, content, dLines, diff.additions to diff.deletions)
        } else {
            fileView = FileViewState(relPath, content, null, null)
        }
    }

    fun openFile(relPath: String) {
        val cached = fileContentCache[relPath]
        if (cached != null) { showFile(relPath, cached); drawerOpen = false; return }
        scope.launch {
            api.readFile(directory, relPath).onSuccess { content ->
                fileContentCache[relPath] = content
                showFile(relPath, content)
            }.onFailure { e -> errorText = e.message ?: "Failed to read file" }
        }
        drawerOpen = false
    }

    fun navigateReview(dir: Int) {
        val idx = currentChangedIndex
        val list = unreviewedFiles
        if (list.isEmpty()) return
        val next = if (idx < 0) list.first() else {
            val ni = (idx + dir + list.size) % list.size
            list[ni]
        }
        openFile(next.file)
    }

    fun toggleReviewed(filePath: String) {
        if (filePath in reviewedFiles) reviewedFiles.remove(filePath) else reviewedFiles.add(filePath)
    }

    val visibleEntries = remember(expandedDirs.size, dirCache.size, diffMap.size) {
        buildVisibleEntries("", 0, dirCache, expandedDirs, diffMap)
    }

    if (isLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    if (errorText != null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(errorText!!, color = MaterialTheme.colorScheme.error) }; return }

    Box(Modifier.fillMaxSize()) {
        val view = fileView
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (drawerOpen) "\u2715" else "Files", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { drawerOpen = !drawerOpen }.padding(horizontal = 8.dp, vertical = 6.dp))
                Text(view?.path ?: directory.substringAfterLast('/'), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (view?.stats != null) {
                    val (adds, dels) = view.stats!!; Spacer(Modifier.width(8.dp))
                    if (adds > 0) Text("+$adds", style = MaterialTheme.typography.labelSmall, color = AddFg)
                    if (adds > 0 && dels > 0) Spacer(Modifier.width(4.dp))
                    if (dels > 0) Text("-$dels", style = MaterialTheme.typography.labelSmall, color = DelFg)
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (view != null) FileView(view, engine, theme, reviewComments, commentTarget, commentText,
                    onTapLine = { path, line ->
                        commentTarget = path to line
                        commentText = reviewComments.find { it.filePath == path && it.lineStart == line }?.text ?: ""
                    },
                    onRemoveComment = { id -> reviewComments.removeAll { it.id == id } },
                    onUpdateComment = { id, text ->
                        val idx = reviewComments.indexOfFirst { it.id == id }
                        if (idx >= 0) reviewComments[idx] = reviewComments[idx].copy(text = text)
                    },
                )
                else if (!hasChanges) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No changes", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                else if (unreviewedFiles.isNotEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${unreviewedFiles.size} files to review", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { navigateReview(1) }) { Text("Start review") }
                    }
                }
                else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Tap Files to browse", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if ((unreviewedFiles.isNotEmpty() || reviewComments.isNotEmpty()) && view != null) {
                val isCurrentReviewed = view.path in reviewedFiles
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (isCurrentReviewed) "\u2713" else "\u25CB",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp),
                        color = if (isCurrentReviewed) AddFg else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { toggleReviewed(view.path) }.padding(end = 8.dp),
                    )
                    Text("${unreviewedFiles.size} remaining", style = MaterialTheme.typography.labelSmall, color = ModFg, modifier = Modifier.weight(1f))
                    if (reviewComments.isNotEmpty()) {
                        Text("${reviewComments.size}\uD83D\uDCAC", style = MaterialTheme.typography.labelSmall, color = AddFg)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("\u25C0  Prev", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable { navigateReview(-1) }.padding(horizontal = 8.dp, vertical = 4.dp))
                    Text("Next  \u25B6", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { navigateReview(1) }.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
        AnimatedVisibility(visible = drawerOpen, enter = slideInHorizontally(initialOffsetX = { -it }), exit = slideOutHorizontally(targetOffsetX = { -it })) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)).clickable { drawerOpen = false })
                Box(Modifier.fillMaxHeight().width(260.dp).shadow(4.dp).background(MaterialTheme.colorScheme.surface)) {
                    FileTreeContent(visibleEntries, isDark, drawerOpen, ::toggleDir, ::openFile, view?.path, reviewedFiles, diffMap)
                }
            }
        }
        if (commentTarget != null) {
            val (path, line) = commentTarget!!
            Box(
                Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(12.dp)
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Text("Comment on $path:$line", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = commentText, onValueChange = { commentText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Write a review comment") },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { commentTarget = null; commentText = "" }) { Text("Cancel") }
                        TextButton(onClick = {
                            if (commentText.isNotBlank()) {
                                val existing = reviewComments.find { it.filePath == path && it.lineStart == line }
                                if (existing != null) {
                                    val idx = reviewComments.indexOf(existing)
                                    if (idx >= 0) reviewComments[idx] = existing.copy(text = commentText.trim())
                                } else {
                                    val id = "${path}:$line:${reviewComments.size}"
                                    reviewComments.add(ReviewComment(id, path, line, line, commentText.trim()))
                                }
                                commentTarget = null
                                commentText = ""
                            }
                        }) { Text("Save") }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileTreeContent(
    entries: List<TreeEntry>, isDark: Boolean, isVisible: Boolean,
    onToggleDir: (String) -> Unit, onOpenFile: (String) -> Unit, selectedPath: String?,
    reviewedFiles: List<String>, diffMap: Map<String, FileDiff>,
) {
    val unreviewed = if (isDark) Color(0xFFFFD700) else Color(0xFFFFA000)

    LazyColumn(Modifier.fillMaxSize()) {
        items(entries, key = { it.path.ifEmpty { "/" } + if (it.isDirectory) "d" else "f" }) { entry ->
            if (!isVisible) return@items
            val isSelected = entry.path == selectedPath
            val isReviewed = entry.path in reviewedFiles
            val indicator = when {
                !entry.hasChanges -> null
                entry.isDirectory -> {
                    val folderReviewed = diffMap.keys
                        .filter { it.startsWith("${entry.path}/") }
                        .all { it in reviewedFiles }
                    if (folderReviewed) "\u2713" to AddFg
                    else "\u2022" to unreviewed
                }
                isReviewed -> "\u2713" to AddFg
                else -> "\u2022" to unreviewed
            }
            val nameColor = when {
                isSelected -> MaterialTheme.colorScheme.primary
                entry.hasChanges -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            }
            Row(
                Modifier.fillMaxWidth()
                    .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                    .clickable { if (entry.isDirectory) onToggleDir(entry.path) else onOpenFile(entry.path) }
                    .padding(start = (8 + entry.depth * 12).dp, end = 6.dp, top = 3.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (entry.isDirectory) Text(
                    if (entry.isExpanded) "\u25BC" else "\u25B6",
                    style = mono12.copy(fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.width(12.dp),
                ) else Spacer(Modifier.width(12.dp))
                Spacer(Modifier.width(2.dp))
                Text(entry.name, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = nameColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (indicator != null) {
                    Spacer(Modifier.width(4.dp))
                    Text(indicator.first, style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp), color = indicator.second)
                }
            }
        }
    }
}

private fun buildVisibleEntries(
    rootPath: String, depth: Int, dirCache: Map<String, List<FsEntry>>,
    expandedDirs: List<String>, diffMap: Map<String, FileDiff>,
): List<TreeEntry> {
    val entries = dirCache[rootPath] ?: return emptyList()
    val result = mutableListOf<TreeEntry>()
    for (entry in entries) {
        val relPath = if (rootPath.isEmpty()) entry.name else "$rootPath/${entry.name}"
        val isExpanded = entry.isDirectory && relPath in expandedDirs
        val (adds, dels) = if (entry.isDirectory) {
            var a = 0; var d = 0
            for ((p, diff) in diffMap) if (p.startsWith("$relPath/")) { a += diff.additions; d += diff.deletions }
            a to d
        } else { (diffMap[relPath]?.additions ?: 0) to (diffMap[relPath]?.deletions ?: 0) }
        val hasChanges = adds > 0 || dels > 0
        result.add(TreeEntry(entry.name, relPath, entry.isDirectory, depth, isExpanded, hasChanges, adds, dels))
        if (isExpanded) result.addAll(buildVisibleEntries(relPath, depth + 1, dirCache, expandedDirs, diffMap))
    }
    return result
}

@Composable
private fun FileView(
    view: FileViewState, engine: SyntaxTokenizer, theme: SyntaxTheme,
    reviewComments: List<ReviewComment>, commentTarget: Pair<String, Int>?,
    commentText: String,
    onTapLine: (String, Int) -> Unit, onRemoveComment: (String) -> Unit,
    onUpdateComment: (String, String) -> Unit,
) {
    val fileComments = reviewComments.filter { it.filePath == view.path }
    if (view.displayLines == null) {
        PlainFileViewer(view.path, view.content, engine, theme, fileComments, onTapLine, onRemoveComment, onUpdateComment)
    } else {
        DiffFileViewer(view.path, view.displayLines, engine, theme, fileComments, onTapLine, onRemoveComment, onUpdateComment)
    }
}

@Composable
private fun PlainFileViewer(
    filePath: String, content: String, engine: SyntaxTokenizer, theme: SyntaxTheme,
    comments: List<ReviewComment>,
    onTapLine: (String, Int) -> Unit, onRemoveComment: (String) -> Unit,
    onUpdateComment: (String, String) -> Unit,
) {
    val language = remember(filePath) { languageFromPath(filePath) }
    val highlighted = rememberSyntaxAnnotatedString(code = content, languageLabel = language, engine = engine, theme = theme)
    val highlightedLines = remember(highlighted, content) { splitAnnotatedString(highlighted) }
    val pad = maxOf(highlightedLines.size.toString().length, 3)
    val commentedLines = remember(comments) { comments.associateBy { it.lineStart } }
    var editingId by remember { mutableStateOf<String?>(null) }
    var maxLinePx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val hScrollState = rememberScrollState()

    LazyColumn(Modifier.fillMaxSize().padding(bottom = 8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
        items(highlightedLines.size, key = { it }) { idx ->
            val ln = idx + 1
            val hasComment = commentedLines.containsKey(ln)
            Column(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().horizontalScroll(hScrollState)) {
                    Row(
                        Modifier
                            .background(if (hasComment) AddBg.copy(alpha = 0.2f) else Color.Transparent)
                            .widthIn(min = with(density) { maxLinePx.toDp() })
                            .onSizeChanged { size -> if (size.width > maxLinePx) maxLinePx = size.width }
                    ) {
                        Text("$ln".padStart(pad), style = mono12.copy(color = if (hasComment) AddFg else ContextFg), modifier = Modifier.padding(start = 4.dp, end = 8.dp).clickable { onTapLine(filePath, ln) })
                        Text(highlightedLines.getOrElse(idx) { AnnotatedString("") }, style = mono12)
                    }
                }
                if (hasComment) {
                    val c = commentedLines[ln]!!
                    val isEditing = editingId == c.id
                    Box(
                        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 6.dp, top = 2.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        if (isEditing) {
                            val editText = remember(c.id) { mutableStateOf(c.text) }
                            Column {
                                OutlinedTextField(
                                    value = editText.value, onValueChange = { editText.value = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = false,
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Text("Cancel", modifier = Modifier.clickable { editingId = null }.padding(horizontal = 8.dp, vertical = 4.dp), style = mono12.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                                    Text("Delete", modifier = Modifier.clickable { onRemoveComment(c.id); editingId = null }.padding(horizontal = 8.dp, vertical = 4.dp), style = mono12.copy(fontSize = 12.sp, color = DelFg))
                                    Text("Save", modifier = Modifier.clickable {
                                        onUpdateComment(c.id, editText.value.trim())
                                        editingId = null
                                    }.padding(horizontal = 8.dp, vertical = 4.dp), style = mono12.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.primary))
                                }
                            }
                        } else {
                            Text(c.text, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.fillMaxWidth().clickable { editingId = c.id })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiffFileViewer(
    filePath: String, lines: List<DisplayLine>, engine: SyntaxTokenizer, theme: SyntaxTheme,
    comments: List<ReviewComment>,
    onTapLine: (String, Int) -> Unit, onRemoveComment: (String) -> Unit,
    onUpdateComment: (String, String) -> Unit,
) {
    val language = remember(filePath) { languageFromPath(filePath) }
    val pad = maxOf(lines.maxOfOrNull { it.lineNumber ?: 0 }.toString().length, 3)
    val commentedLines = remember(comments) { comments.associateBy { it.lineStart } }
    var editingId by remember { mutableStateOf<String?>(null) }
    var maxLinePx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val hScrollState = rememberScrollState()

    LazyColumn(Modifier.fillMaxSize().padding(bottom = 8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
        items(lines.size, key = { it }) { idx ->
            val line = lines[idx]
            val highlighted = rememberSyntaxAnnotatedString(code = line.content, languageLabel = language, engine = engine, theme = theme)
            val (fg, bg) = when (line.type) {
                DisplayLineType.ADDITION -> AddFg to AddBg
                DisplayLineType.DELETION -> DelFg to DelBg
                DisplayLineType.NORMAL -> Color.Transparent to Color.Transparent
            }
            val ln = line.lineNumber
            val hasComment = ln != null && commentedLines.containsKey(ln)
            val lineBg = if (hasComment) AddBg.copy(alpha = 0.3f) else if (bg != Color.Transparent) bg else Color.Transparent
            Column(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().horizontalScroll(hScrollState)) {
                    Row(
                        Modifier
                            .background(lineBg)
                            .widthIn(min = with(density) { maxLinePx.toDp() })
                            .onSizeChanged { size -> if (size.width > maxLinePx) maxLinePx = size.width }
                    ) {
                        Box(Modifier.width(4.dp).fillMaxHeight().background(if (line.type == DisplayLineType.ADDITION) AddFg else if (line.type == DisplayLineType.DELETION) DelFg else Color.Transparent))
                        val numText = if (ln != null) "$ln".padStart(pad) else "-".repeat(pad)
                        Text(numText, style = mono12.copy(color = if (hasComment) AddFg else ContextFg), modifier = Modifier.padding(start = 4.dp, end = 8.dp).clickable { if (ln != null) onTapLine(filePath, ln) })
                        val prefix = when (line.type) { DisplayLineType.ADDITION -> "+"; DisplayLineType.DELETION -> "-"; else -> " " }
                        Text(prefix, style = mono12.copy(color = if (line.type != DisplayLineType.NORMAL) fg else ContextFg, fontWeight = if (line.type != DisplayLineType.NORMAL) FontWeight.Bold else FontWeight.Normal))
                        Text(highlighted, style = mono12)
                    }
                }
                if (hasComment) {
                    val c = commentedLines[ln]!!
                    val isEditing = editingId == c.id
                    Box(
                        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 6.dp, top = 2.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        if (isEditing) {
                            val editText = remember(c.id) { mutableStateOf(c.text) }
                            Column {
                                OutlinedTextField(
                                    value = editText.value, onValueChange = { editText.value = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = false,
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Text("Cancel", modifier = Modifier.clickable { editingId = null }.padding(horizontal = 8.dp, vertical = 4.dp), style = mono12.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                                    Text("Delete", modifier = Modifier.clickable { onRemoveComment(c.id); editingId = null }.padding(horizontal = 8.dp, vertical = 4.dp), style = mono12.copy(fontSize = 12.sp, color = DelFg))
                                    Text("Save", modifier = Modifier.clickable {
                                        onUpdateComment(c.id, editText.value.trim())
                                        editingId = null
                                    }.padding(horizontal = 8.dp, vertical = 4.dp), style = mono12.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.primary))
                                }
                            }
                        } else {
                            Text(c.text, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.fillMaxWidth().clickable { editingId = c.id })
                        }
                    }
                }
            }
        }
    }
}
