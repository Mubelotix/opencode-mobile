package page.dera.opencode.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.time.Clock

import kotlinx.coroutines.launch
import opencodeapp.shared.generated.resources.Res
import opencodeapp.shared.generated.resources.star
import opencodeapp.shared.generated.resources.star_filled
import opencodeapp.shared.generated.resources.menuIcon
import org.jetbrains.compose.resources.painterResource
import page.dera.opencode.api.OpencodeApi
import page.dera.opencode.api.SessionInfo
import page.dera.opencode.settings.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(api: OpencodeApi, directory: String, name: String, settings: AppSettings, onBack: () -> Unit, onSessionClick: (SessionInfo) -> Unit, onMenuClick: (() -> Unit)? = null) {
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
                actions = {
                    var starred by remember { mutableStateOf(settings.isPinned(directory)) }
                    Image(
                        painter = painterResource(if (starred) Res.drawable.star_filled else Res.drawable.star),
                        contentDescription = if (starred) "Unstar" else "Star",
                        modifier = Modifier
                            .size(40.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) {
                                settings.togglePinnedFolder(directory)
                                starred = settings.isPinned(directory)
                            }
                            .padding(8.dp),
                    )
                },
            )
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
            else -> {
                val grouped = groupByWorkspace(sessions)
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    item(key = "new_session") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable {
                                    if (!isCreating) {
                                        isCreating = true
                                        scope.launch {
                                            val result = api.createSession(directory)
                                            result.fold(
                                                onSuccess = { onSessionClick(it) },
                                                onFailure = { errorMessage = it.message ?: "Failed to create session" },
                                            )
                                            isCreating = false
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isCreating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text(
                                        text = "+ New session",
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                        }
                    }
                    if (sessions.isNotEmpty()) {
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
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
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
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
        }
    }
}

private fun buildSubtitle(session: SessionInfo): String? {
    val parts = mutableListOf<String>()
    val relative = relativeTime(session.time.updated)
    if (relative != null) parts.add(relative)
    val totalTokens = (session.tokens?.input ?: 0) + (session.tokens?.output ?: 0) + (session.tokens?.reasoning ?: 0) + (session.tokens?.cache?.read ?: 0) + (session.tokens?.cache?.write ?: 0)
    if (totalTokens > 0) parts.add(formatTokens(totalTokens))
    if (session.cost != null && session.cost > 0) {
        val dollars = session.cost.toLong()
        val cents = ((session.cost - dollars) * 100).toLong()
        parts.add("$${dollars}.${cents.toString().padStart(2, '0')}")
    }
    if (parts.isEmpty()) {
        val dir = session.location.directory.trimEnd('/').split('/')
        if (dir.isNotEmpty()) parts.add(dir.takeLast(2).joinToString("/"))
    }
    return parts.joinToString("  ·  ").ifBlank { null }
}

private fun relativeTime(epochMillis: Long): String? {
    if (epochMillis <= 0) return null
    val now = Clock.System.now().toEpochMilliseconds()
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
            ?: "Default Workspace"
        grouped.getOrPut(key) { mutableListOf() }.add(session)
    }
    return grouped.mapValues { it.value.toList() } as LinkedHashMap<String, List<SessionInfo>>
}
