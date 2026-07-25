package page.dera.opencode.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import page.dera.opencode.api.FileDiff
import page.dera.opencode.api.OpencodeApi

private val AddBg = Color(0xFFE6FFEC)
private val DelBg = Color(0xFFFFEBE9)
private val AddFg = Color(0xFF1A7F37)
private val DelFg = Color(0xFFCF222E)
private val HunkFg = Color(0xFF8250DF)
private val HeaderFg = Color(0xFF0969DA)
private val ContextFg = Color(0xFF6E7681)
private val mono12 = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)

@Composable
fun GitReviewContent(api: OpencodeApi, directory: String) {
    var fileDiffs by remember { mutableStateOf<List<FileDiff>>(emptyList()) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val expandedFiles = remember { mutableStateListOf<String>() }
    val reviewedFiles = remember { mutableStateListOf<String>() }
    var expandAll by remember { mutableStateOf(false) }

    LaunchedEffect(directory) {
        isLoading = true; errorText = null
        api.getVcsDiff(directory).fold(
            onSuccess = { diffs -> fileDiffs = diffs.sortedBy { it.file } },
            onFailure = { e -> errorText = e.message ?: "Failed to load diff" },
        )
        isLoading = false
    }

    if (isLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    if (errorText != null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(errorText!!, color = MaterialTheme.colorScheme.error) }; return }
    if (fileDiffs.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No changes", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }; return }

    val unreviewed = fileDiffs.count { it.file !in reviewedFiles }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (unreviewed > 0) {
                Text("$unreviewed to review", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            } else {
                Text("All reviewed", style = MaterialTheme.typography.labelMedium, color = AddFg)
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = if (expandAll) "Collapse all" else "Expand all",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    expandAll = !expandAll
                    if (expandAll) { fileDiffs.forEach { if (it.file !in expandedFiles) expandedFiles.add(it.file) } }
                    else { expandedFiles.clear() }
                }.padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }

        val statusAdd = Color(0xFF1A7F37)
        val statusDel = Color(0xFFCF222E)
        val statusMod = Color(0xFF8250DF)

        LazyColumn(Modifier.fillMaxSize()) {
            items(fileDiffs, key = { it.file }) { diff ->
                val isExpanded = diff.file in expandedFiles
                val isReviewed = diff.file in reviewedFiles
                val statusColor = when (diff.status) {
                    "added" -> statusAdd; "deleted" -> statusDel; else -> statusMod
                }

                Column(
                    Modifier.fillMaxWidth()
                        .background(if (isReviewed) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(bottom = if (isReviewed) 0.dp else 0.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable {
                                if (isExpanded) expandedFiles.remove(diff.file)
                                else expandedFiles.add(diff.file)
                            }
                            .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isReviewed,
                            onCheckedChange = {
                                if (isReviewed) reviewedFiles.remove(diff.file) else reviewedFiles.add(diff.file)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = AddFg),
                            modifier = Modifier.height(32.dp).width(32.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = diff.file,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = if (isReviewed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                            ),
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (diff.additions > 0) Text("+${diff.additions}", style = MaterialTheme.typography.labelSmall, color = statusAdd)
                        if (diff.additions > 0 && diff.deletions > 0) Spacer(Modifier.width(4.dp))
                        if (diff.deletions > 0) Text("-${diff.deletions}", style = MaterialTheme.typography.labelSmall, color = statusDel)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isExpanded) "\u25B2" else "\u25BC",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                        if (diff.patch != null) {
                            val scrollState = rememberScrollState()
                            Box(Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
                                Column(Modifier.padding(start = 8.dp, bottom = 8.dp)) {
                                    for (line in diff.patch!!.lines()) {
                                        val (fg, bg) = when {
                                            line.startsWith("+") && !line.startsWith("+++") -> AddFg to AddBg
                                            line.startsWith("-") && !line.startsWith("---") -> DelFg to DelBg
                                            line.startsWith("@@") -> HunkFg to Color.Transparent
                                            line.startsWith("diff --git") || line.startsWith("index ") || line.startsWith("---") || line.startsWith("+++") -> HeaderFg to Color.Transparent
                                            else -> ContextFg to Color.Transparent
                                        }
                                        val weight = if (line.startsWith("@@")) FontWeight.Bold else FontWeight.Normal
                                        Row(Modifier.fillMaxWidth().background(bg).padding(vertical = 1.dp)) {
                                            Text(line, style = mono12.copy(color = fg, fontWeight = weight))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
