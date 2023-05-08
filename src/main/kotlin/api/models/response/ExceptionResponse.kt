package api.models.response

import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ExceptionResponse(
    override val message: String,
    override val status: Int,
    override val data: JsonElement?
) : GenericResponse {
    constructor(reason: String, status: HttpStatusCode, data: JsonElement? = null): this(reason, status.value, data)
}
