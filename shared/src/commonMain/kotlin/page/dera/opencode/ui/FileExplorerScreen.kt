package page.dera.opencode.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import opencodeapp.shared.generated.resources.Res
import opencodeapp.shared.generated.resources.folder
import opencodeapp.shared.generated.resources.file
import opencodeapp.shared.generated.resources.star
import org.jetbrains.compose.resources.painterResource
import page.dera.opencode.api.FsEntry
import page.dera.opencode.api.OpencodeApi
import page.dera.opencode.settings.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    api: OpencodeApi,
    settings: AppSettings,
    initialDirectory: String = "/home",
    onOpenProject: (directory: String) -> Unit,
) {
    var currentPath by remember { mutableStateOf(initialDirectory) }
    var entries by remember { mutableStateOf<List<FsEntry>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var pathInput by remember { mutableStateOf(currentPath) }
    var showPathInput by remember { mutableStateOf(false) }
    var pinnedFolders by remember { mutableStateOf(settings.pinnedFolders) }
    val pathStack = remember { mutableListOf(initialDirectory) }
    var stackPos by remember { mutableStateOf(0) }

    fun navigateTo(path: String) {
        while (pathStack.size > stackPos + 1) pathStack.removeLast()
        pathStack.add(path)
        stackPos = pathStack.lastIndex
        currentPath = path
        showPathInput = false
        isLoading = true
        errorMessage = null
        entries = null
    }

    fun loadDirectory(path: String) {
        navigateTo(path)
    }

    fun goBack(): Boolean {
        if (stackPos <= 0) return false
        stackPos--
        val path = pathStack[stackPos]
        currentPath = path
        showPathInput = false
        isLoading = true
        errorMessage = null
        entries = null
        return true
    }

    val parentPath = currentPath.trimEnd('/').let { path ->
        if (path == "/" || path.isEmpty()) null
        else path.substringBeforeLast('/', "").ifEmpty { "/" }
    }

    BackHandler {
        goBack()
    }

    LaunchedEffect(currentPath) {
        if (currentPath.isBlank()) return@LaunchedEffect
        val result = api.listFiles(directory = currentPath)
        result.fold(
            onSuccess = {
                entries = it.data.sortedWith(compareBy({ it.type != "directory" }, { it.name.startsWith(".") }, { it.name.lowercase() }))
            },
            onFailure = { errorMessage = it.message ?: "Failed to list directory" },
        )
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showPathInput) {
                        TextField(
                            value = pathInput,
                            onValueChange = { pathInput = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Path") },
                        )
                    } else {
                        Text(
                            text = currentPath,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { loadDirectory(currentPath) }) {
                            Text("Retry")
                        }
                    }
                }
                entries != null -> {
                    val list = entries
                    if (list != null) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            if (pinnedFolders.isNotEmpty() && parentPath == null) {
                                item(key = "pinned_header") {
                                    Text(
                                        text = "Starred",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                    )
                                }
                                items(pinnedFolders, key = { "pinned_$it" }) { path ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { loadDirectory(path) }
                                            .padding(horizontal = 16.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Image(
                                            painter = painterResource(Res.drawable.star),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = path,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Image(
                                                painter = painterResource(Res.drawable.star),
                                                contentDescription = "Unstar",
                                                modifier = Modifier.size(20.dp).clickable {
                                                    settings.togglePinnedFolder(path)
                                                    pinnedFolders = settings.pinnedFolders
                                                },
                                            )
                                    }
                                }
                                item(key = "pinned_spacer") {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                            if (parentPath != null) {
                                item(key = "parent") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { goBack() }
                                            .padding(horizontal = 16.dp, vertical = 4.dp),
                                    ) {
                                        Text("..", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            items(list, key = { it.path }) { entry ->
                                val fullPath = if (currentPath == "/") "/${entry.name}" else "$currentPath/${entry.name}"
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f).clickable {
                                            if (entry.isDirectory) loadDirectory(fullPath)
                                        }.padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Image(
                                            painter = painterResource(if (entry.isDirectory) Res.drawable.folder else Res.drawable.file),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = entry.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    if (entry.isDirectory) {
                                        Image(
                                            painter = painterResource(Res.drawable.star),
                                            contentDescription = if (settings.isPinned(fullPath)) "Unstar" else "Star",
                                            modifier = Modifier.size(20.dp).clickable {
                                                settings.togglePinnedFolder(fullPath)
                                                pinnedFolders = settings.pinnedFolders
                                            },
                                            alpha = if (settings.isPinned(fullPath)) 1f else 0.3f,
                                        )
                                    }
                                }
                            }
                            item(key = "open_button") {
                                Spacer(modifier = Modifier.height(24.dp))
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    OutlinedButton(
                                        onClick = { onOpenProject(currentPath) },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("Open as project")
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
