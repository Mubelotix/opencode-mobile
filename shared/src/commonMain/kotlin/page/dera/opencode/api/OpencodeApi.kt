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
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
            val response = client.get("$serverUrl/api/health") {
                authHeader()?.let { (key, value) -> header(key, value) }
            }
            if (response.status.isSuccess()) {
                ValidationResult.Success
            } else {
                val body = response.bodyAsText()
                ValidationResult.Error("Server returned ${response.status.value}: $body")
            }
        } catch (e: Exception) {
            ValidationResult.Error("Connection failed: ${e.message ?: "Unknown error"}")
        }
    }

    suspend fun listProjects(): Result<List<ProjectInfo>> {
        return try {
            val response = client.get("$serverUrl/project") {
                authHeader()?.let { (key, value) -> header(key, value) }
            }
            val body = response.bodyAsText()
            val projects = json.decodeFromString<List<ProjectInfo>>(body)
            Result.success(projects)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listSessions(directory: String, cursor: String? = null): Result<SessionsResponse> {
        return try {
            val params = buildList {
                add("directory" to directory)
                add("limit" to "30")
                if (cursor != null) add("cursor" to cursor)
            }
            val response = client.get("$serverUrl/api/session") {
                authHeader()?.let { (key, value) -> header(key, value) }
                params.forEach { (key, value) -> url.parameters.append(key, value) }
            }
            val body = response.bodyAsText()
            val sessionsResponse = json.decodeFromString<SessionsResponse>(body)
            Result.success(sessionsResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listFiles(directory: String, path: String = "."): Result<FsListResponse> {
        return try {
            val response = client.get("$serverUrl/api/fs/list") {
                authHeader()?.let { (key, value) -> header(key, value) }
                header("x-opencode-directory", directory)
                url.parameters.append("path", path)
            }
            val body = response.bodyAsText()
            val result = json.decodeFromString<FsListResponse>(body)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createSession(directory: String): Result<SessionInfo> {
        return try {
            val payload = SessionCreatePayload(
                location = CreateSessionLocation(directory),
                model = SessionCreateModel(id = "deepseek-v4-flash-free", providerID = "opencode"),
            )
            val response = client.post("$serverUrl/api/session") {
                authHeader()?.let { (key, value) -> header(key, value) }
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(SessionCreatePayload.serializer(), payload))
            }
            val body = response.bodyAsText()
            val result = json.decodeFromString<SessionCreateResponse>(body)
            Result.success(result.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSessionMessages(sessionId: String): Result<List<SessionMessage>> {
        return try {
            val allMessages = mutableListOf<SessionMessage>()
            val seen = mutableSetOf<String>()
            var cursor: String? = null

            repeat(20) {
                val response = client.get("$serverUrl/api/session/$sessionId/message") {
                    authHeader()?.let { (key, value) -> header(key, value) }
                    url.parameters.append("limit", "200")
                    url.parameters.append("order", "desc")
                    val cursorVal = cursor
                    if (cursorVal != null) url.parameters.append("cursor", cursorVal)
                }
                val body = response.bodyAsText()
                val root = json.parseToJsonElement(body).jsonObject
                val dataArray = root["data"]?.jsonArray
                if (dataArray != null) {
                    val page = dataArray.mapNotNull { element ->
                        try {
                            json.decodeFromJsonElement(SessionMessage.serializer(), element) as? SessionMessage
                        } catch (_: Exception) { null }
                    }
                    page.forEach { if (seen.add(it.id)) allMessages.add(it) }
                    if (page.isEmpty()) return@repeat
                }
                cursor = root["cursor"]?.jsonObject?.get("next")?.jsonPrimitive?.content
                if (cursor == null) return@repeat
            }

            Result.success(allMessages.sortedBy { it.time.created })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPrompt(sessionId: String, text: String): Result<SessionInputAdmitted> {
        return try {
            val payload = PromptPayload(
                prompt = PromptBody(text = text),
                delivery = "steer",
                resume = true,
            )
            val response = client.post("$serverUrl/api/session/$sessionId/prompt") {
                authHeader()?.let { (key, value) -> header(key, value) }
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(PromptPayload.serializer(), payload))
            }
            val body = response.bodyAsText()
            val result = json.decodeFromString<PromptResponse>(body)
            Result.success(result.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun streamSessionEvents(sessionId: String, after: Long = 0): Flow<SSEEvent> = flow {
        client.prepareGet("$serverUrl/api/session/$sessionId/event") {
            authHeader()?.let { (key, value) -> header(key, value) }
            url.parameters.append("after", after.toString())
        }.execute { response ->
            val channel = response.bodyAsChannel()
            var dataBuffer = ""
            while (coroutineContext.isActive && !channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                when {
                    line.startsWith("data: ") -> {
                        dataBuffer = line.removePrefix("data: ")
                    }
                    line.isEmpty() -> {
                        if (dataBuffer.isNotEmpty()) {
                            try {
                                val event = json.decodeFromString<SSEEvent>(dataBuffer)
                                emit(event)
                            } catch (_: Exception) { }
                            dataBuffer = ""
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun normalizeUrl(raw: String): String {
            val trimmed = raw.trim()
            return if (trimmed.contains("://")) {
                trimmed.trimEnd('/')
            } else {
                "http://${trimmed.trimEnd('/')}"
            }
        }
    }
}

sealed class ValidationResult {
    data object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
