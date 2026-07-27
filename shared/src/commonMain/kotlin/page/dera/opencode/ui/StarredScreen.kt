package page.dera.opencode.ui

import androidx.compose.foundation.Image
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import opencodeapp.shared.generated.resources.star
import opencodeapp.shared.generated.resources.menuIcon
import org.jetbrains.compose.resources.painterResource
import page.dera.opencode.api.OpencodeApi
import page.dera.opencode.api.SessionInfo
import page.dera.opencode.settings.AppSettings
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarredScreen(
    settings: AppSettings,
    api: OpencodeApi,
    onBrowseFiles: () -> Unit,
    onOpenFolder: (path: String) -> Unit,
    onOpenSession: (SessionInfo) -> Unit,
    onMenuClick: (() -> Unit)? = null,
) {
    val latestSessionsLimit = settings.latestSessionsLimit
    var latestSessions by remember { mutableStateOf<List<SessionInfo>>(emptyList()) }

    LaunchedEffect(latestSessionsLimit) {
        if (latestSessionsLimit > 0) {
            api.listLatestSessions(latestSessionsLimit).onSuccess { latestSessions = it.filter { s -> s.parentID == null }.take(latestSessionsLimit) }
        } else {
            latestSessions = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projects") },
                navigationIcon = {
                    if (onMenuClick != null) {
                        IconButton(onClick = onMenuClick) {
                            Image(
                                painter = painterResource(menuIcon),
                                contentDescription = "Open menu",
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        val starred = settings.pinnedFolders
        val hasLatest = latestSessionsLimit > 0 && latestSessions.isNotEmpty()
        val hasStarred = starred.isNotEmpty()

        if (!hasLatest && !hasStarred) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No starred folders",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Star folders in the file browser or project pages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                if (hasLatest) {
                    item(key = "latest_header") {
                        Text(
                            text = "Latest Sessions",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                    items(latestSessions, key = { it.id }) { session ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onOpenSession(session) },
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
                                    val dirName = session.location.directory.trimEnd('/').split('/').lastOrNull() ?: session.location.directory
                                    Text(
                                        text = session.title.ifBlank { dirName },
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    val relativeTime = formatRelativeTime(session.time.updated)
                                    Text(
                                        text = "$dirName, $relativeTime",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }

                if (hasStarred) {
                    item(key = "starred_header") {
                        Text(
                            text = "Starred",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                    items(starred, key = { it }) { path ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onOpenFolder(path) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Image(
                                    painter = painterResource(Res.drawable.star),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val parts = path.trimEnd('/').split('/')
                                    val name = parts.lastOrNull() ?: path
                                    val parent = parts.dropLast(1).takeLast(2).joinToString("/").ifEmpty { null }
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (parent != null) {
                                        Text(
                                            text = parent,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.BottomCenter,
        ) {
            OutlinedButton(
                onClick = onBrowseFiles,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
            ) {
                Text("Browse files")
            }
        }
    }
}

private fun formatRelativeTime(createdEpoch: Long): String {
    if (createdEpoch <= 0) return ""
    val now = Clock.System.now().toEpochMilliseconds()
    val diff = now - createdEpoch
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        days < 30 -> "${days / 7}w ago"
        days < 365 -> "${days / 30}mo ago"
        else -> "${days / 365}y ago"
    }
}
