package page.dera.opencode.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlin.io.encoding.Base64

class OpencodeApi(private val client: HttpClient) {

    suspend fun validate(serverUrl: String, password: String?): ValidationResult {
        val url = normalizeUrl(serverUrl)
        return try {
            val response = client.get("$url/api/health") {
                if (!password.isNullOrBlank()) {
                    val credentials = Base64.Default.encode("opencode:$password".encodeToByteArray())
                    header("Authorization", "Basic $credentials")
                }
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

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.contains("://")) {
            trimmed.trimEnd('/')
        } else {
            "http://${trimmed.trimEnd('/')}"
        }
    }
}

sealed class ValidationResult {
    data object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
