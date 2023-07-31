package api.models.response

import kotlinx.serialization.json.JsonElement

interface GenericJsonResponse {

    fun build(): JsonElement

}