package api.models.response

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
sealed interface GenericResponse {
    val message: String
    val status: Int
    val data: JsonElement?
}