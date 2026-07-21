package page.dera.opencode.api

import kotlinx.serialization.Serializable

@Serializable
data class ProjectInfo(
    val id: String,
    val worktree: String,
    val name: String? = null,
)
