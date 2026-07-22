package page.dera.opencode.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
sealed interface SessionMessage {
    val id: String
    val time: MessageTime

    @Serializable
    @SerialName("user")
    data class User(
        override val id: String,
        val text: String,
        val files: List<FileAttachment> = emptyList(),
        val agents: List<AgentAttachment> = emptyList(),
        override val time: MessageTime,
    ) : SessionMessage

    @Serializable
    @SerialName("assistant")
    data class Assistant(
        override val id: String,
        val agent: String,
        val model: ModelRef,
        val content: List<AssistantContent> = emptyList(),
        val snapshot: AssistantSnapshot? = null,
        val finish: String? = null,
        val cost: Long? = null,
        val tokens: AssistantTokens? = null,
        val error: AssistantError? = null,
        override val time: MessageTime,
    ) : SessionMessage

    @Serializable
    @SerialName("system")
    data class SystemMsg(
        override val id: String,
        val text: String,
        override val time: MessageTime,
    ) : SessionMessage

    @Serializable
    @SerialName("shell")
    data class Shell(
        override val id: String,
        @SerialName("callID")
        val callID: String,
        val command: String,
        val output: String,
        override val time: MessageTime,
    ) : SessionMessage

    @Serializable
    @SerialName("synthetic")
    data class Synthetic(
        override val id: String,
        @SerialName("sessionID")
        val sessionID: String,
        val text: String,
        override val time: MessageTime,
    ) : SessionMessage

    @Serializable
    @SerialName("agent-switched")
    data class AgentSwitched(
        override val id: String,
        val agent: String,
        override val time: MessageTime,
    ) : SessionMessage

    @Serializable
    @SerialName("model-switched")
    data class ModelSwitched(
        override val id: String,
        val model: ModelRef,
        override val time: MessageTime,
    ) : SessionMessage

    @Serializable
    @SerialName("compaction")
    data class Compaction(
        override val id: String,
        val reason: String,
        val summary: String,
        val recent: String,
        override val time: MessageTime,
    ) : SessionMessage

    @Serializable
    data class Unknown(
        override val id: String = "",
        override val time: MessageTime = MessageTime(created = 0),
        val rawJson: String = "",
        val messageType: String = "",
        val rawMap: Map<String, JsonElement> = emptyMap(),
        val parseError: String = "",
    ) : SessionMessage
}

@Serializable
data class MessageTime(
    val created: Long,
    val completed: Long? = null,
)

@Serializable
data class ModelRef(
    @SerialName("providerID")
    val providerID: String = "",
    @SerialName("modelID")
    val modelID: String = "",
)

@Serializable
data class FileAttachment(
    val uri: String,
    val name: String? = null,
    val description: String? = null,
    val source: FileAttachmentSource? = null,
)

@Serializable
data class FileAttachmentSource(
    val start: Long,
    val end: Long,
    val text: String,
)

@Serializable
data class AgentAttachment(
    val name: String,
    val source: FileAttachmentSource? = null,
)

@Serializable
data class AssistantSnapshot(
    val start: String? = null,
    val end: String? = null,
    val files: List<String>? = null,
)

@Serializable
data class AssistantTokens(
    val input: Long = 0,
    val output: Long = 0,
    val reasoning: Long = 0,
    val cache: TokenCache = TokenCache(),
)

@Serializable
data class TokenCache(
    val read: Long = 0,
    val write: Long = 0,
)

@Serializable
data class AssistantError(
    val type: String = "unknown",
    val message: String,
)

@Serializable
sealed interface AssistantContent {
    @Serializable
    @SerialName("text")
    data class Text(
        val id: String,
        val text: String,
    ) : AssistantContent

    @Serializable
    @SerialName("reasoning")
    data class Reasoning(
        val id: String,
        val text: String,
        @SerialName("providerMetadata")
        val providerMetadata: Map<String, Map<String, kotlinx.serialization.json.JsonElement>>? = null,
        val time: MessageTime? = null,
    ) : AssistantContent

    @Serializable
    @SerialName("tool")
    data class Tool(
        val id: String,
        val name: String,
        val provider: ToolProvider? = null,
        val state: ToolState,
        val time: MessageTime? = null,
    ) : AssistantContent
}

@Serializable
data class ToolProvider(
    val executed: Boolean = false,
    val metadata: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    @SerialName("resultMetadata")
    val resultMetadata: Map<String, kotlinx.serialization.json.JsonElement>? = null,
)

@Serializable
@JsonClassDiscriminator("status")
sealed interface ToolState {
    val status: String

    @Serializable
    @SerialName("pending")
    data class Pending(
        val input: kotlinx.serialization.json.JsonElement? = null,
    ) : ToolState {
        override val status: String = "pending"
    }

    @Serializable
    @SerialName("running")
    data class Running(
        val input: kotlinx.serialization.json.JsonElement? = null,
        val structured: kotlinx.serialization.json.JsonElement? = null,
        val content: kotlinx.serialization.json.JsonArray = kotlinx.serialization.json.JsonArray(emptyList()),
    ) : ToolState {
        override val status: String = "running"
    }

    @Serializable
    @SerialName("completed")
    data class Completed(
        val input: kotlinx.serialization.json.JsonElement? = null,
        val structured: kotlinx.serialization.json.JsonElement? = null,
        val content: kotlinx.serialization.json.JsonArray = kotlinx.serialization.json.JsonArray(emptyList()),
        val attachments: List<FileAttachment>? = null,
        @SerialName("outputPaths")
        val outputPaths: List<String>? = null,
        val result: kotlinx.serialization.json.JsonElement? = null,
    ) : ToolState {
        override val status: String = "completed"
    }

    @Serializable
    @SerialName("error")
    data class Error(
        val input: kotlinx.serialization.json.JsonElement? = null,
        val structured: kotlinx.serialization.json.JsonElement? = null,
        val content: kotlinx.serialization.json.JsonArray = kotlinx.serialization.json.JsonArray(emptyList()),
        val error: ToolError,
        val result: kotlinx.serialization.json.JsonElement? = null,
    ) : ToolState {
        override val status: String = "error"
    }
}

@Serializable
data class ToolError(
    val type: String = "unknown",
    val message: String,
)

@Serializable
data class SessionMessagesResponse(
    val data: List<SessionMessage>,
)

@Serializable
data class SSEEvent(
    val id: String,
    val type: String,
    val properties: kotlinx.serialization.json.JsonElement? = null,
)
