package page.dera.opencode.api

import kotlinx.serialization.Serializable

@Serializable
data class ReviewComment(
    val id: String,
    val filePath: String,
    val lineStart: Int,
    val lineEnd: Int,
    val text: String,
)
