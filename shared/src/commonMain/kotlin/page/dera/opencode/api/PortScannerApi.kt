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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class PortScannerApi(
    private val client: HttpClient,
    val endpoint: String,
    private val password: String?,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private fun authHeader(): Pair<String, String>? {
        if (password.isNullOrBlank()) return null
        val credentials = Base64.Default.encode("opencode:$password".encodeToByteArray())
        return "Authorization" to "Basic $credentials"
    }

    suspend fun listPorts(): Result<List<Int>> {
        return try {
            val body = client.get(endpoint) {
                authHeader()?.let { (k, v) -> header(k, v) }
            }.bodyAsText()
            val resp = json.decodeFromString<PortScannerResponse>(body)
            Result.success(resp.ports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDirectory(path: String): Result<Unit> {
        return try {
            val resp = client.post("$endpoint/fs/mkdir") {
                authHeader()?.let { (k, v) -> header(k, v) }
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(MkdirRequest.serializer(), MkdirRequest(path)))
            }
            if (resp.status.isSuccess()) {
                val body = resp.bodyAsText()
                val r = json.decodeFromString<MkdirResponse>(body)
                if (r.ok) Result.success(Unit)
                else Result.failure(Exception(r.error))
            } else {
                Result.failure(Exception("HTTP ${resp.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Serializable
data class PortScannerResponse(val ports: List<Int>)

@Serializable
data class MkdirRequest(val path: String)

@Serializable
data class MkdirResponse(val ok: Boolean, val error: String = "")
