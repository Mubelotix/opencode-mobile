package page.dera.opencode.api

import kotlinx.serialization.Serializable

@Serializable
data class PtyInfo(
    val id: String,
    val title: String,
    val command: String,
    val args: List<String> = emptyList(),
    val cwd: String = "",
    val status: String,
    val pid: Long = 0,
    val exitCode: Long? = null,
)

@Serializable
data class PtyCreatePayload(
    val command: String? = null,
    val args: List<String>? = null,
    val cwd: String? = null,
    val title: String? = null,
    val env: Map<String, String>? = null,
)

@Serializable
data class PtyUpdatePayload(
    val title: String? = null,
    val size: TerminalSize? = null,
)

@Serializable
data class TerminalSize(
    val rows: Int,
    val cols: Int,
)

@Serializable
data class PtyCreateResponse(
    val data: PtyInfo,
    val location: PtyLocation? = null,
)

@Serializable
data class PtyListResponse(
    val data: List<PtyInfo> = emptyList(),
    val location: PtyLocation? = null,
)

@Serializable
data class PtyLocation(
    val directory: String = "",
)

@Serializable
data class PtyTokenResponse(
    val data: PtyConnectToken,
)

@Serializable
data class PtyConnectToken(
    val ticket: String,
)
