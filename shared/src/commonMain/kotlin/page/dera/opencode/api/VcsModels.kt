package page.dera.opencode.api

import kotlinx.serialization.Serializable

@Serializable
data class FileDiff(
    val file: String,
    val patch: String? = null,
    val additions: Int = 0,
    val deletions: Int = 0,
    val status: String? = null,
)
