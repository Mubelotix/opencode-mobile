package page.dera.opencode.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import page.dera.opencode.api.OpencodeApi
import page.dera.opencode.api.SessionInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(api: OpencodeApi, directory: String, name: String, onBack: () -> Unit, onSessionClick: (SessionInfo) -> Unit) {
    var sessions by remember { mutableStateOf<List<SessionInfo>>(emptyList()) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var hasMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val limit = 30

    fun handlePage(response: page.dera.opencode.api.SessionsResponse, append: Boolean) {
        val filtered = response.data.filter { it.parentID == null }
        sessions = if (append) sessions + filtered else filtered
        nextCursor = response.cursor?.next
        hasMore = response.data.size >= limit
    }

    LaunchedEffect(Unit) {
        val result = api.listSessions(directory = directory)
        result.fold(
            onSuccess = { handlePage(it, append = false) },
            onFailure = { errorMessage = it.message ?: "Failed to load sessions" },
        )
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!isCreating) {
                        isCreating = true
                        scope.launch {
                            val result = api.createSession(directory)
                            result.fold(
                                onSuccess = { session ->
                                    onSessionClick(session)
                                },
                                onFailure = { errorMessage = it.message ?: "Failed to create session" },
                            )
                            isCreating = false
                        }
                    }
                },
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("+")
                }
            }
        },
    ) { padding ->
        when {
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = {
                            errorMessage = null
                            isLoading = true
                            scope.launch {
        val result = api.listSessions(directory = directory)
                                result.fold(
                                    onSuccess = { handlePage(it, append = false) },
                                    onFailure = { errorMessage = it.message ?: "Failed to load sessions" },
                                )
                                isLoading = false
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }
            }
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            sessions.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No sessions",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                val grouped = groupByWorkspace(sessions)
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {
                    grouped.forEach { (workspaceName, workspaceSessions) ->
                        item(key = "header_$workspaceName") {
                            Text(
                                text = workspaceName,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                        }
                        items(workspaceSessions, key = { it.id }) { session ->
                            SessionCard(session = session, onClick = { onSessionClick(session) })
                        }
                        item(key = "spacer_$workspaceName") {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    if (hasMore) {
                        item(key = "load_more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    OutlinedButton(onClick = {
                                        isLoadingMore = true
                                        scope.launch {
                                            val result = api.listSessions(
                                                directory = directory,
                                                cursor = nextCursor,
                                            )
                                            result.fold(
                                                onSuccess = { handlePage(it, append = true) },
                                                onFailure = { errorMessage = it.message ?: "Failed to load more" },
                                            )
                                            isLoadingMore = false
                                        }
                                    }) {
                                        Text("Load more")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: SessionInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title.ifBlank { session.id },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = buildSubtitle(session)
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = ">",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun buildSubtitle(session: SessionInfo): String? {
    val parts = mutableListOf<String>()

    val relative = relativeTime(session.time.updated)
    if (relative != null) parts.add(relative)

    val totalTokens = (session.tokens?.input ?: 0) + (session.tokens?.output ?: 0)
    if (totalTokens > 0) parts.add(formatTokens(totalTokens))

    if (parts.isEmpty()) {
        val dir = session.location.directory.trimEnd('/').split('/')
        if (dir.isNotEmpty()) parts.add(dir.takeLast(2).joinToString("/"))
    }

    return parts.joinToString("  ·  ").ifBlank { null }
}

private fun relativeTime(epochMillis: Long): String? {
    if (epochMillis <= 0) return null
    val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    val diff = now - epochMillis
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        seconds < 60 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}

private fun formatTokens(count: Long): String {
    return when {
        count >= 1_000_000 -> "${count / 1_000_000}.${(count % 1_000_000) / 100_000}M tokens"
        count >= 1_000 -> "${count / 1_000}.${(count % 1_000) / 100}k tokens"
        else -> "$count tokens"
    }
}

private fun groupByWorkspace(sessions: List<SessionInfo>): LinkedHashMap<String, List<SessionInfo>> {
    val grouped = LinkedHashMap<String, MutableList<SessionInfo>>()
    for (session in sessions) {
        val key = session.location.workspaceID?.removePrefix("wrk_")?.replaceFirstChar { it.uppercase() }
            ?: "Default"
        grouped.getOrPut(key) { mutableListOf() }.add(session)
    }
    return grouped.mapValues { it.value.toList() } as LinkedHashMap<String, List<SessionInfo>>
}
