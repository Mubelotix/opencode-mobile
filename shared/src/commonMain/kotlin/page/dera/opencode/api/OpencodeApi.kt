package page.dera.opencode.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlin.io.encoding.Base64
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

class OpencodeApi(
    private val client: HttpClient,
    val serverUrl: String,
    private val password: String?,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private fun authHeader(): Pair<String, String>? {
        if (password.isNullOrBlank()) return null
        val credentials = Base64.Default.encode("opencode:$password".encodeToByteArray())
        return "Authorization" to "Basic $credentials"
    }

    suspend fun validate(): ValidationResult {
        return try {
            val r = client.get("$serverUrl/api/health") { authHeader()?.let { (k, v) -> header(k, v) } }
            if (r.status.isSuccess()) ValidationResult.Success else ValidationResult.Error("Status ${r.status.value}")
        } catch (e: Exception) { ValidationResult.Error("Connection failed: ${e.message}") }
    }

    suspend fun listProjects(): Result<List<ProjectInfo>> {
        return try {
            val b = client.get("$serverUrl/project") { authHeader()?.let { (k, v) -> header(k, v) } }.bodyAsText()
            Result.success(json.decodeFromString<List<ProjectInfo>>(b))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun listSessions(directory: String, cursor: String? = null): Result<SessionsResponse> {
        return try {
            val p = buildList { add("directory" to directory); add("limit" to "30"); cursor?.let { add("cursor" to it) } }
            val b = client.get("$serverUrl/api/session") {
                authHeader()?.let { (k, v) -> header(k, v) }; p.forEach { (k, v) -> url.parameters.append(k, v) }
            }.bodyAsText()
            Result.success(json.decodeFromString<SessionsResponse>(b))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun listFiles(directory: String, path: String = "."): Result<FsListResponse> {
        return try {
            val b = client.get("$serverUrl/api/fs/list") {
                authHeader()?.let { (k, v) -> header(k, v) }; header("x-opencode-directory", directory); url.parameters.append("path", path)
            }.bodyAsText()
            Result.success(json.decodeFromString<FsListResponse>(b))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun createSession(directory: String): Result<SessionInfo> {
        return try {
            val p = SessionCreatePayload(location = CreateSessionLocation(directory), model = SessionCreateModel(id = "deepseek-v4-flash-free", providerID = "opencode"))
            val b = client.post("$serverUrl/api/session") {
                authHeader()?.let { (k, v) -> header(k, v) }; contentType(ContentType.Application.Json)
                setBody(json.encodeToString(SessionCreatePayload.serializer(), p))
            }.bodyAsText()
            Result.success(json.decodeFromString<SessionCreateResponse>(b).data)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getSessionMessages(sessionId: String): Result<List<LegacyWithParts>> {
        return try {
            val b = client.get("$serverUrl/session/$sessionId/message") { authHeader()?.let { (k, v) -> header(k, v) } }.bodyAsText()
            val list = when (val e = json.parseToJsonElement(b)) { is JsonArray -> e; else -> JsonArray(emptyList()) }
            Result.success(list.mapNotNull { try { json.decodeFromJsonElement(LegacyWithParts.serializer(), it) } catch (_: Exception) { null } })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun replyPermission(sessionId: String, requestId: String, reply: String, directory: String): Result<Unit> {
        return try {
            val resp = client.post("$serverUrl/permission/$requestId/reply") {
                authHeader()?.let { (k, v) -> header(k, v) }; contentType(ContentType.Application.Json)
                url.parameters.append("directory", directory)
                setBody(json.encodeToString(PermissionReplyPayload.serializer(), PermissionReplyPayload(reply)))
            }
            println("OC_DBG: permission reply status=${resp.status} requestId=$requestId reply=$reply")
            if (resp.status.isSuccess()) Result.success(Unit) else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { println("OC_DBG: permission reply error=${e.message}"); Result.failure(e) }
    }

    suspend fun replyQuestion(sessionId: String, requestId: String, answers: List<List<String>>, directory: String): Result<Unit> {
        return try {
            val resp = client.post("$serverUrl/question/$requestId/reply") {
                authHeader()?.let { (k, v) -> header(k, v) }; contentType(ContentType.Application.Json)
                url.parameters.append("directory", directory)
                setBody(json.encodeToString(QuestionReplyPayload.serializer(), QuestionReplyPayload(answers)))
            }
            println("OC_DBG: question reply status=${resp.status} body=${resp.bodyAsText()}")
            if (resp.status.isSuccess()) Result.success(Unit) else Result.failure(Exception("HTTP ${resp.status.value}: ${resp.bodyAsText()}"))
        } catch (e: Exception) { println("OC_DBG: question reply error=${e.message}"); Result.failure(e) }
    }

    suspend fun rejectQuestion(sessionId: String, requestId: String, directory: String): Result<Unit> {
        return try {
            val resp = client.post("$serverUrl/question/$requestId/reject") {
                authHeader()?.let { (k, v) -> header(k, v) }
                url.parameters.append("directory", directory)
            }
            println("OC_DBG: question reject status=${resp.status} body=${resp.bodyAsText()}")
            if (resp.status.isSuccess()) Result.success(Unit) else Result.failure(Exception("HTTP ${resp.status.value}: ${resp.bodyAsText()}"))
        } catch (e: Exception) { println("OC_DBG: question reject error=${e.message}"); Result.failure(e) }
    }

    suspend fun sendPrompt(sessionId: String, text: String): Result<Unit> {
        return try {
            val p = LegacyPromptPayload(parts = listOf(LegacyPromptPart(type = "text", text = text, synthetic = false)))
            client.post("$serverUrl/session/$sessionId/prompt_async") {
                authHeader()?.let { (k, v) -> header(k, v) }; contentType(ContentType.Application.Json)
                setBody(json.encodeToString(LegacyPromptPayload.serializer(), p))
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    fun streamGlobalEvents(directory: String): Flow<GlobalEvent> = flow {
        while (true) {
            println("OC_DBG: SSE connecting for dir=$directory")
            try {
                client.prepareGet("$serverUrl/global/event") {
                    authHeader()?.let { (k, v) -> header(k, v) }; url.parameters.append("directory", directory)
                }.execute { response ->
                    val channel = response.bodyAsChannel()
                    var dataBuffer = ""
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break
                        when {
                            line.startsWith("data: ") -> dataBuffer = line.removePrefix("data: ")
                            line.isEmpty() && dataBuffer.isNotEmpty() -> {
                                try { emit(json.decodeFromString<GlobalEvent>(dataBuffer)) } catch (_: Exception) { }
                                dataBuffer = ""
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("OC_DBG: SSE error: ${e.message}")
            }
            println("OC_DBG: SSE disconnected, reconnecting in 1s...")
            delay(1000)
        }
    }

    companion object {
        fun normalizeUrl(raw: String): String = raw.trim().let { if (it.contains("://")) it.trimEnd('/') else "http://${it.trimEnd('/')}" }
    }
}

sealed class ValidationResult {
    data object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

@Serializable
data class GlobalEvent(
    val directory: String = "",
    val project: String? = null,
    val workspace: String? = null,
    val payload: GlobalEventPayload? = null,
)

@Serializable
data class GlobalEventPayload(
    val type: String = "",
    val properties: JsonElement? = null,
)

@Serializable
data class PermissionReplyPayload(val reply: String, val message: String? = null)
