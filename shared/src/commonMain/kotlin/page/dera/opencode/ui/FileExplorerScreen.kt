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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import opencodeapp.shared.generated.resources.Res
import opencodeapp.shared.generated.resources.folder
import opencodeapp.shared.generated.resources.file
import opencodeapp.shared.generated.resources.menuIcon
import org.jetbrains.compose.resources.painterResource
import page.dera.opencode.api.FsEntry
import page.dera.opencode.api.OpencodeApi
import page.dera.opencode.api.PortScannerApi
import page.dera.opencode.settings.AppSettings
import opencodeapp.shared.generated.resources.folderPlusIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    api: OpencodeApi,
    settings: AppSettings,
    initialDirectory: String = "/home",
    onOpenProject: (directory: String) -> Unit,
    onMenuClick: (() -> Unit)? = null,
) {
    var currentPath by remember { mutableStateOf(initialDirectory) }
    var entries by remember { mutableStateOf<List<FsEntry>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var pathInput by remember { mutableStateOf(currentPath) }
    var showPathInput by remember { mutableStateOf(false) }
    val pathStack = remember { mutableListOf(initialDirectory) }
    var stackPos by remember { mutableStateOf(0) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var isCreatingFolder by remember { mutableStateOf(false) }
    var folderCreationError by remember { mutableStateOf<String?>(null) }
    var reloadCounter by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val httpClient = remember { HttpClient() }
    val helperEndpoint = remember(settings.portScannerEndpoint) { settings.portScannerEndpoint }
    val helperApi = remember(helperEndpoint) {
        if (helperEndpoint != null) PortScannerApi(httpClient, helperEndpoint.trimEnd('/'), settings.serverPassword) else null
    }

    fun refreshDirectory() {
        isLoading = true
        errorMessage = null
        entries = null
    }

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

    LaunchedEffect(currentPath, reloadCounter) {
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
                navigationIcon = {
                    if (onMenuClick != null) {
                        IconButton(onClick = onMenuClick) {
                            Image(
                                painter = painterResource(menuIcon),
                                contentDescription = "Tabs",
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (helperApi != null && !isLoading && entries != null) {
                FloatingActionButton(
                    onClick = {
                        newFolderName = ""
                        showNewFolderDialog = true
                    },
                ) {
                    Image(
                        painter = painterResource(folderPlusIcon),
                        contentDescription = "New folder",
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
                    )
                }
            }
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
                            if (parentPath != null) {
                                item(key = "parent") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { loadDirectory(parentPath!!) }
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

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isCreatingFolder) showNewFolderDialog = false
            },
            title = { Text("New Folder") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = {
                            newFolderName = it
                            folderCreationError = null
                        },
                        label = { Text("Folder name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isCreatingFolder,
                    )
                    if (folderCreationError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            folderCreationError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newFolderName.trim()
                        if (name.isNotEmpty() && !name.contains("/")) {
                            isCreatingFolder = true
                            folderCreationError = null
                            scope.launch {
                                val result = helperApi?.createDirectory("$currentPath/$name")
                                if (result?.isSuccess == true) {
                                    showNewFolderDialog = false
                                    isLoading = true
                                    entries = null
                                    reloadCounter++
                                } else {
                                    folderCreationError = result?.exceptionOrNull()?.message ?: "Failed to create folder"
                                }
                                isCreatingFolder = false
                            }
                        }
                    },
                    enabled = newFolderName.isNotBlank() && !isCreatingFolder,
                ) {
                    Text(if (isCreatingFolder) "Creating..." else "Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNewFolderDialog = false },
                    enabled = !isCreatingFolder,
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}
