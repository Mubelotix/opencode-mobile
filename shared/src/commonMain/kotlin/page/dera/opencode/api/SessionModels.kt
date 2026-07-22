package page.dera.opencode.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SessionsResponse(
    val data: List<SessionInfo>,
    val cursor: SessionCursor? = null,
)

@Serializable
data class SessionCursor(
    val next: String? = null,
    val previous: String? = null,
)

@Serializable
data class SessionInfo(
    val id: String,
    val title: String,
    val projectID: String,
    val time: SessionTime,
    val location: SessionLocation,
    val parentID: String? = null,
    val tokens: SessionTokens? = null,
)

@Serializable
data class SessionTokens(
    val input: Long = 0,
    val output: Long = 0,
)

@Serializable
data class SessionTime(
    val created: Long,
    val updated: Long,
)

@Serializable
data class SessionLocation(
    val directory: String,
    @SerialName("workspaceID")
    val workspaceID: String? = null,
)

@Serializable
data class SessionCreatePayload(
    val location: CreateSessionLocation,
)

@Serializable
data class CreateSessionLocation(
    val directory: String,
)

@Serializable
data class SessionCreateResponse(
    val data: SessionInfo,
)
