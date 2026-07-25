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

class WakatimeApi(
    private val client: HttpClient,
    val apiUrl: String,
    private val apiKey: String,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private val baseUrl: String
        get() = apiUrl.trimEnd('/')

    private fun authHeader(): Pair<String, String> {
        val credentials = Base64.Default.encode(apiKey.encodeToByteArray())
        return "Authorization" to "Basic $credentials"
    }

    suspend fun validate(): ValidationResult {
        return try {
            val r = client.get("$baseUrl/users/current") {
                authHeader().let { (k, v) -> header(k, v) }
            }
            if (r.status.isSuccess()) {
                println("OC_DBG: wakatime validate ok: GET $baseUrl/users/current -> ${r.status}")
                return ValidationResult.Success
            }
            if (r.status.value != 404) {
                println("OC_DBG: wakatime validate users/current: ${r.status}")
                return ValidationResult.Error("Status ${r.status.value}")
            }
            println("OC_DBG: wakatime validate retrying health endpoint...")
            val health = client.get("$baseUrl/health") {
                authHeader().let { (k, v) -> header(k, v) }
            }
            if (health.status.isSuccess()) {
                println("OC_DBG: wakatime validate ok: GET $baseUrl/health -> ${health.status}")
                ValidationResult.Success
            } else {
                println("OC_DBG: wakatime validate health: ${health.status}")
                ValidationResult.Error("Status ${health.status.value}")
            }
        } catch (e: Exception) {
            println("OC_DBG: wakatime validate error: ${e.message}")
            ValidationResult.Error("Connection failed: ${e.message}")
        }
    }

    suspend fun sendHeartbeat(heartbeat: WakatimeHeartbeat): Result<Unit> {
        return try {
            val body = json.encodeToString(WakatimeHeartbeat.serializer(), heartbeat)
            println("OC_DBG: wakatime heartbeat -> ${heartbeat.entity} (${heartbeat.type}) body=$body")
            val resp = client.post("$baseUrl/users/current/heartbeats") {
                authHeader().let { (k, v) -> header(k, v) }
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            val respBody = resp.bodyAsText()
            if (resp.status.isSuccess()) {
                println("OC_DBG: wakatime heartbeat ok: ${resp.status} body=$respBody")
                Result.success(Unit)
            } else {
                println("OC_DBG: wakatime heartbeat fail: ${resp.status} body=$respBody")
                Result.failure(Exception("HTTP ${resp.status.value}: $respBody"))
            }
        } catch (e: Exception) {
            println("OC_DBG: wakatime heartbeat error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun sendHeartbeats(heartbeats: List<WakatimeHeartbeat>): Result<Unit> {
        return try {
            println("OC_DBG: wakatime heartbeats bulk -> ${heartbeats.size} entries")
            val resp = client.post("$baseUrl/users/current/heartbeats.bulk") {
                authHeader().let { (k, v) -> header(k, v) }
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(WakatimeHeartbeatList.serializer(),
                    WakatimeHeartbeatList(heartbeats)))
            }
            if (resp.status.isSuccess()) {
                println("OC_DBG: wakatime heartbeats ok: ${resp.status}")
                Result.success(Unit)
            } else {
                println("OC_DBG: wakatime heartbeats fail: ${resp.status} body=${resp.bodyAsText()}")
                Result.failure(Exception("HTTP ${resp.status.value}: ${resp.bodyAsText()}"))
            }
        } catch (e: Exception) {
            println("OC_DBG: wakatime heartbeats error: ${e.message}")
            Result.failure(e)
        }
    }

    companion object {
        const val DEFAULT_API_URL = "https://api.wakatime.com/api/v1"

        fun normalizeUrl(raw: String): String =
            raw.trim().let { if (it.contains("://")) it.trimEnd('/') else "https://${it.trimEnd('/')}" }
    }
}

@Serializable
data class WakatimeHeartbeat(
    val entity: String,
    val type: String = "file",
    val time: Double,
    val category: String? = null,
    val project: String? = null,
    val branch: String? = null,
    val language: String? = null,
    val editor: String? = null,
    val operating_system: String? = null,
    val machine: String? = null,
    val user_agent: String? = null,
    val is_write: Boolean? = null,
    val lines: Int? = null,
    val lineno: Int? = null,
    val cursorpos: Int? = null,
    val dependencies: String? = null,
)

@Serializable
data class WakatimeHeartbeatList(
    val heartbeats: List<WakatimeHeartbeat>,
)
