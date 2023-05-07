package api.models.response

import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class ExceptionResponse(
    val reason: String,
    val status: Int,
) {
    constructor(reason: String, status: HttpStatusCode): this(reason, status.value)
}
