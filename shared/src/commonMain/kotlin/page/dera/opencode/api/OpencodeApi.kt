package page.dera.opencode.api

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.put
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

    suspend fun listLatestSessions(limit: Int): Result<List<SessionInfo>> {
        if (limit <= 0) return Result.success(emptyList())
        return try {
            val fetchLimit = (limit * 3).coerceAtMost(50)
            val b = client.get("$serverUrl/api/session") {
                authHeader()?.let { (k, v) -> header(k, v) }
                url.parameters.append("limit", fetchLimit.toString())
                url.parameters.append("order", "desc")
            }.bodyAsText()
            val all = json.decodeFromString<SessionsResponse>(b).data
            val sorted = all.sortedByDescending { it.time.updated }
            Result.success(sorted)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getSession(sessionId: String, directory: String): Result<SessionInfo> {
        return try {
            val b = client.get("$serverUrl/api/session/$sessionId") {
                authHeader()?.let { (k, v) -> header(k, v) }
                url.parameters.append("directory", directory)
            }.bodyAsText()
            Result.success(json.decodeFromString<SessionCreateResponse>(b).data)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun listProviders(directory: String): Result<List<ProviderInfo>> {
        return try {
            val b = client.get("$serverUrl/provider") {
                authHeader()?.let { (k, v) -> header(k, v) }
                url.parameters.append("directory", directory)
            }.bodyAsText()
            Result.success(json.decodeFromString<ProviderListResponse>(b).all)
        } catch (e: Exception) {
            Result.failure(e)
        }
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

    suspend fun listCommands(directory: String): Result<List<CommandInfo>> {
        return try {
            val b = client.get("$serverUrl/command") {
                authHeader()?.let { (k, v) -> header(k, v) }
                url.parameters.append("directory", directory)
            }.bodyAsText()
            Result.success(json.decodeFromString<List<CommandInfo>>(b))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun sendCommand(sessionId: String, command: String, arguments: String, directory: String): Result<Unit> {
        return try {
            val p = CommandPayload(command = command, arguments = arguments)
            val body = json.encodeToString(CommandPayload.serializer(), p)
            println("OC_DBG: sendCommand POST /session/$sessionId/command body=$body")
            val resp = client.post("$serverUrl/session/$sessionId/command") {
                authHeader()?.let { (k, v) -> header(k, v) }; contentType(ContentType.Application.Json)
                setBody(body)
            }
            println("OC_DBG: sendCommand status=${resp.status} command=$command")
            if (resp.status.isSuccess()) Result.success(Unit) else {
                val respBody = try { resp.bodyAsText() } catch (_: Exception) { "n/a" }
                println("OC_DBG: sendCommand fail body=$respBody")
                Result.failure(Exception("Server returned ${resp.status.value}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun interruptSession(sessionId: String): Result<Unit> {
        return try {
            val resp = client.post("$serverUrl/session/$sessionId/abort") {
                authHeader()?.let { (k, v) -> header(k, v) }
            }
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun compactSession(sessionId: String, providerID: String, modelID: String, directory: String): Result<Unit> {
        return try {
            val payload = SummarizePayload(providerID = providerID, modelID = modelID, auto = false)
            val body = json.encodeToString(SummarizePayload.serializer(), payload)
            println("OC_DBG: summarize POST /session/$sessionId/summarize body=$body")
            val resp = client.post("$serverUrl/session/$sessionId/summarize") {
                authHeader()?.let { (k, v) -> header(k, v) }
                contentType(ContentType.Application.Json)
                url.parameters.append("directory", directory)
                setBody(body)
            }
            println("OC_DBG: summarize status=${resp.status}")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateSessionTitle(sessionId: String, title: String, directory: String): Result<Unit> {
        return try {
            val p = SessionUpdatePayload(title = title)
            client.patch("$serverUrl/session/$sessionId") {
                authHeader()?.let { (k, v) -> header(k, v) }
                contentType(ContentType.Application.Json)
                url.parameters.append("directory", directory)
                setBody(json.encodeToString(SessionUpdatePayload.serializer(), p))
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getSessionsStatus(directory: String): Result<Map<String, SessionStatusInfo>> {
        return try {
            val b = client.get("$serverUrl/api/session/status") {
                authHeader()?.let { (k, v) -> header(k, v) }
                url.parameters.append("directory", directory)
            }.bodyAsText()
            val map = json.decodeFromString<Map<String, SessionStatusInfo>>(b)
            Result.success(map)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteSession(sessionId: String, directory: String): Result<Unit> {
        return try {
            val resp = client.delete("$serverUrl/session/$sessionId") {
                authHeader()?.let { (k, v) -> header(k, v) }
                url.parameters.append("directory", directory)
            }
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getSessionTodo(sessionId: String, directory: String): Result<List<TodoInfo>> {
        return try {
            val b = client.get("$serverUrl/api/session/$sessionId/todo") {
                authHeader()?.let { (k, v) -> header(k, v) }
                url.parameters.append("directory", directory)
            }.bodyAsText()
            val list = json.decodeFromString<List<TodoInfo>>(b)
            Result.success(list)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getVcsDiff(directory: String, mode: String = "git"): Result<List<FileDiff>> {
        return try {
            val b = client.get("$serverUrl/vcs/diff") {
                authHeader()?.let { (k, v) -> header(k, v) }
                url.parameters.append("directory", directory)
                url.parameters.append("mode", mode)
            }.bodyAsText()
            Result.success(json.decodeFromString<List<FileDiff>>(b))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun readFile(directory: String, filePath: String): Result<String> {
        return try {
            val b = client.get("$serverUrl/api/fs/read/$filePath") {
                authHeader()?.let { (k, v) -> header(k, v) }
                header("x-opencode-directory", directory)
            }.bodyAsText()
            Result.success(b.trim())
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun listPtys(directory: String): Result<PtyListResponse> {
        return try {
            val b = client.get("$serverUrl/api/pty") {
                authHeader()?.let { (k, v) -> header(k, v) }
                url.parameters.append("directory", directory)
            }.bodyAsText()
            val wrapper = json.decodeFromString<PtyListResponse>(b)
            Result.success(wrapper)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun createPty(directory: String, payload: PtyCreatePayload): Result<PtyInfo> {
        return try {
            val body = json.encodeToString(PtyCreatePayload.serializer(), payload)
            println("OC_DBG: createPty directory=$directory body=$body")
            val b = client.post("$serverUrl/api/pty") {
                authHeader()?.let { (k, v) -> header(k, v) }
                contentType(ContentType.Application.Json)
                url.parameters.append("directory", directory)
                setBody(body)
            }.bodyAsText()
            println("OC_DBG: createPty response body=$b")
            val wrapper = json.decodeFromString<PtyCreateResponse>(b)
            Result.success(wrapper.data)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun removePty(ptyID: String, directory: String): Result<Unit> {
        return try {
            client.delete("$serverUrl/api/pty/$ptyID") {
                authHeader()?.let { (k, v) -> header(k, v) }
                url.parameters.append("directory", directory)
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun connectPtyToken(ptyID: String, directory: String): Result<PtyConnectToken> {
        return try {
            val b = client.post("$serverUrl/api/pty/$ptyID/connect-token") {
                authHeader()?.let { (k, v) -> header(k, v) }
                url.parameters.append("directory", directory)
                header("x-opencode-ticket", "1")
            }.bodyAsText()
            val wrapper = json.decodeFromString<PtyTokenResponse>(b)
            Result.success(wrapper.data)
        } catch (e: Exception) { Result.failure(e) }
    }

    fun buildPtyWebSocketUrl(ptyID: String, directory: String, cursor: Int = 0, ticket: String? = null): String {
        val httpUrl = serverUrl.replace("http://", "ws://").replace("https://", "wss://")
        val sb = StringBuilder("$httpUrl/api/pty/$ptyID/connect")
        val params = mutableListOf<String>()
        params.add("directory=$directory")
        params.add("cursor=$cursor")
        if (ticket != null) params.add("ticket=$ticket")
        if (!password.isNullOrBlank()) {
            val token = Base64.Default.encode("opencode:$password".encodeToByteArray())
            params.add("auth_token=$token")
        }
        sb.append("?").append(params.joinToString("&"))
        return sb.toString()
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

@Serializable
data class SessionUpdatePayload(val title: String)
