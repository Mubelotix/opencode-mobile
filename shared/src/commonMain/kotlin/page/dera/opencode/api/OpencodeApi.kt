package page.dera.opencode.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.io.encoding.Base64
import kotlinx.serialization.json.Json

class OpencodeApi(
    private val client: HttpClient,
    val serverUrl: String,
    private val password: String?,
) {
    private val json = Json { ignoreUnknownKeys = true }

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
            val payload = SessionCreatePayload(location = CreateSessionLocation(directory))
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
