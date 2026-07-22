package page.dera.opencode.api

import kotlinx.serialization.Serializable

@Serializable
data class FsEntry(
    val path: String,
    val type: String,
) {
    val isDirectory: Boolean get() = type == "directory"
    val name: String get() = path.trimEnd('/').split('/').last()
}

@Serializable
data class FsListResponse(
    val data: List<FsEntry>,
    val location: FsLocation? = null,
)

@Serializable
data class FsLocation(
    val directory: String,
    val project: FsProject? = null,
)

@Serializable
data class FsProject(
    val id: String,
    val directory: String,
)
