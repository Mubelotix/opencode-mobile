package page.dera.opencode.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonElement

@Serializable
data class LegacyWithParts(
    val info: LegacyInfo,
    val parts: List<LegacyPart> = emptyList(),
)

@Serializable
data class LegacyInfo(
    val id: String,
    @SerialName("sessionID")
    val sessionID: String,
    val role: String,
    val time: LegacyMessageTime = LegacyMessageTime(),
    val format: LegacyResponseFormat? = null,
    val summary: LegacySummary? = null,
    val agent: String = "",
    val model: LegacySessionModel = LegacySessionModel(),
    val system: String? = null,
    val tools: Map<String, Boolean>? = null,
    val error: LegacyError? = null,
    val parentID: String? = null,
    val modelID: String = "",
    val providerID: String = "",
    val mode: String = "",
    val path: LegacyPath? = null,
    val cost: Double? = null,
    val tokens: LegacyTokens? = null,
    val finish: String? = null,
    val structured: JsonElement? = null,
    val variant: String? = null,
)

@Serializable
data class LegacyMessageTime(
    val created: Long = 0,
    val completed: Long? = null,
)

@Serializable
data class LegacySessionModel(
    @SerialName("providerID")
    val providerID: String = "",
    @SerialName("modelID")
    val modelID: String = "",
)

@Serializable
data class LegacyTokens(
    val input: Long = 0,
    val output: Long = 0,
    val reasoning: Long = 0,
    val cache: LegacyTokenCache = LegacyTokenCache(),
    val total: Long? = null,
)

@Serializable
data class LegacyTokenCache(
    val read: Long = 0,
    val write: Long = 0,
)

@Serializable
data class LegacyResponseFormat(
    val type: String = "text",
)

@Serializable
data class LegacySummary(
    val title: String? = null,
    val body: String? = null,
)

@Serializable
data class LegacyError(
    val name: String = "",
    val data: JsonElement? = null,
)

@Serializable
data class LegacyPath(
    val cwd: String = "",
    val root: String = "",
)

@Serializable
data class LegacyPart(
    val id: String = "",
    val sessionID: String = "",
    val messageID: String = "",
    val type: String,
    val text: String? = null,
    val synthetic: Boolean? = null,
    val time: LegacyMessageTime? = null,
    val callID: String? = null,
    val tool: String? = null,
    val state: LegacyToolState? = null,
    val mime: String? = null,
    val filename: String? = null,
    val url: String? = null,
    val source: LegacyFileSource? = null,
    val reason: String? = null,
    val snapshot: String? = null,
    val cost: Double? = null,
    val tokens: LegacyTokens? = null,
    val name: String? = null,
    val prompt: String? = null,
    val description: String? = null,
    val command: String? = null,
    val agent: String? = null,
    val model: LegacySessionModel? = null,
    val hash: String? = null,
    val files: List<String>? = null,
    val attempt: Int? = null,
    val auto: Boolean? = null,
    val overflow: Boolean? = null,
    @SerialName("tail_start_id")
    val tailStartID: String? = null,
    val error: String? = null,
    val output: String? = null,
    val title: String? = null,
    val metadata: JsonElement? = null,
)

@Serializable
data class LegacyFileSource(
    val type: String? = null,
    val text: LegacySourceText? = null,
    val path: String? = null,
    val range: LegacySourceRange? = null,
    val name: String? = null,
    val kind: Int? = null,
    val clientName: String? = null,
    val uri: String? = null,
)

@Serializable
data class LegacySourceText(
    val value: String = "",
    val start: Long = 0,
    val end: Long = 0,
)

@Serializable
data class LegacySourceRange(
    val start: LegacySourcePos = LegacySourcePos(),
    val end: LegacySourcePos = LegacySourcePos(),
)

@Serializable
data class LegacySourcePos(
    val line: Long = 0,
    val character: Long = 0,
)

@Serializable
data class FileAttachmentSource(
    val start: Long = 0,
    val end: Long = 0,
    val text: String = "",
)

@Serializable
@JsonClassDiscriminator("status")
sealed interface LegacyToolState {
    val status: String

    @Serializable
    @SerialName("pending")
    data class Pending(
        val input: JsonElement? = null,
        val raw: String? = null,
    ) : LegacyToolState {
        override val status: String = "pending"
    }

    @Serializable
    @SerialName("running")
    data class Running(
        val input: JsonElement? = null,
        val title: String? = null,
        val metadata: JsonElement? = null,
        val time: LegacyMessageTime? = null,
    ) : LegacyToolState {
        override val status: String = "running"
    }

    @Serializable
    @SerialName("completed")
    data class Completed(
        val input: JsonElement? = null,
        val output: String = "",
        val title: String = "",
        val metadata: JsonElement? = null,
        val time: LegacyMessageTime? = null,
    ) : LegacyToolState {
        override val status: String = "completed"
    }

    @Serializable
    @SerialName("error")
    data class Error(
        val input: JsonElement? = null,
        val error: String = "",
        val metadata: JsonElement? = null,
        val time: LegacyMessageTime? = null,
    ) : LegacyToolState {
        override val status: String = "error"
    }
}

@Serializable
data class LegacyPromptPayload(
    val parts: List<LegacyPromptPart>,
)

@Serializable
data class LegacyPromptPart(
    val type: String,
    val text: String,
    val synthetic: Boolean = false,
)

@Serializable
data class PermissionRequest(
    val id: String = "",
    val sessionID: String = "",
    val permission: String = "",
    val patterns: List<String> = emptyList(),
    val metadata: JsonElement? = null,
    val always: List<String> = emptyList(),
    val tool: PermissionToolRef? = null,
)

@Serializable
data class PermissionToolRef(
    val messageID: String = "",
    val callID: String = "",
)
