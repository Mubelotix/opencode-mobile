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
    val model: SessionModel? = null,
    val parentID: String? = null,
    val agent: String? = null,
    val slug: String? = null,
    val tokens: SessionTokens? = null,
    val cost: Double? = null,
    val revert: SessionRevert? = null,
)

@Serializable
data class SessionRevert(
    val messageID: String,
    val partID: String? = null,
    val snapshot: String? = null,
)

@Serializable
data class SessionModel(
    val id: String,
    @SerialName("providerID")
    val providerID: String,
)

@Serializable
data class SwitchModelRef(
    val id: String,
    @SerialName("providerID")
    val providerID: String,
)

@Serializable
data class SwitchModelPayload(
    val model: SwitchModelRef,
)

@Serializable
data class ProviderListResponse(
    val all: List<ProviderInfo> = emptyList(),
    val connected: List<String> = emptyList(),
    val default: Map<String, String> = emptyMap(),
)

@Serializable
data class ProviderInfo(
    val id: String,
    val name: String,
    val models: Map<String, ProviderModel> = emptyMap(),
)

@Serializable
data class ProviderModel(
    val id: String,
    val name: String,
    @SerialName("providerID")
    val providerID: String,
    val limit: ModelLimit,
)

@Serializable
data class ModelLimit(
    val context: Long,
)

@Serializable
data class SessionTokens(
    val input: Long = 0,
    val output: Long = 0,
    val reasoning: Long = 0,
    val cache: SessionTokenCache = SessionTokenCache(),
)

@Serializable
data class SessionTokenCache(
    val read: Long = 0,
    val write: Long = 0,
)

@Serializable
data class SessionTime(
    val created: Long,
    val updated: Long,
    val compacting: Long? = null,
    val archived: Long? = null,
)

@Serializable
data class SessionLocation(
    val directory: String,
    @SerialName("workspaceID")
    val workspaceID: String? = null,
)

@Serializable
data class SessionCreateModel(
    val id: String,
    @SerialName("providerID")
    val providerID: String,
)

@Serializable
data class SessionCreatePayload(
    val location: CreateSessionLocation,
    val model: SessionCreateModel? = null,
)

@Serializable
data class CreateSessionLocation(
    val directory: String,
)

@Serializable
data class SessionCreateResponse(
    val data: SessionInfo,
)

@Serializable
data class V2Session(
    val id: String,
    val title: String,
    val projectID: String,
    val directory: String,
    @SerialName("workspaceID")
    val workspaceID: String? = null,
    val parentID: String? = null,
    val agent: String? = null,
    val model: SessionModel? = null,
    val time: SessionTime,
    val cost: Double? = null,
    val tokens: SessionTokens? = null,
)

fun V2Session.toSessionInfo(): SessionInfo = SessionInfo(
    id = id,
    title = title,
    projectID = projectID,
    time = time,
    location = SessionLocation(directory = directory, workspaceID = workspaceID),
    model = model,
    parentID = parentID,
    agent = agent,
    cost = cost,
    tokens = tokens,
)

@Serializable
data class PromptPayload(
    val prompt: PromptBody,
    val delivery: String? = null,
    val resume: Boolean? = null,
)

@Serializable
data class PromptBody(
    val text: String,
    val files: List<PromptFileAttachment>? = null,
    val agents: List<PromptAgentAttachment>? = null,
)

@Serializable
data class PromptFileAttachment(
    val uri: String,
    val name: String? = null,
    val description: String? = null,
    val source: FileAttachmentSource? = null,
)

@Serializable
data class PromptAgentAttachment(
    val name: String,
    val source: FileAttachmentSource? = null,
)

@Serializable
data class PromptResponse(
    val data: SessionInputAdmitted,
)

@Serializable
data class SessionInputAdmitted(
    val admittedSeq: Long,
    val id: String,
    @SerialName("sessionID")
    val sessionID: String,
    val prompt: PromptData,
    val delivery: String,
    val timeCreated: Long,
    val promotedSeq: Long? = null,
)

@Serializable
data class PromptData(
    val text: String,
    val files: List<PromptFileResponse>? = null,
    val agents: List<PromptAgentResponse>? = null,
)

@Serializable
data class PromptFileResponse(
    val uri: String,
    val mime: String,
    val name: String? = null,
    val description: String? = null,
    val source: FileAttachmentSource? = null,
)

@Serializable
data class PromptAgentResponse(
    val name: String,
    val source: FileAttachmentSource? = null,
)

@Serializable
data class SessionStatusInfo(
    val type: String,
    val attempt: Long? = null,
    val message: String? = null,
    val next: Long? = null,
    val action: SessionRetryAction? = null,
)

@Serializable
data class SessionRetryAction(
    val reason: String,
    val provider: String,
    val title: String,
    val message: String,
    val label: String,
    val link: String? = null,
)

@Serializable
data class AgentInfo(
    val id: String,
    val description: String? = null,
    val mode: String = "primary",
    val hidden: Boolean = false,
    val color: String? = null,
)

@Serializable
data class AgentListResponse(
    val data: List<AgentInfo> = emptyList(),
)

@Serializable
data class SwitchAgentPayload(
    val agent: String,
)

@Serializable
data class TodoInfo(
    val content: String,
    val status: String,
    val priority: String,
)
