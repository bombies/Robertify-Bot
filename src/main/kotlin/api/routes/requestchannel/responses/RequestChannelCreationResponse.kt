package api.routes.requestchannel.responses

import api.models.response.GenericJsonResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

data class RequestChannelCreationResponse(
    private val channel_id: String,
    private val message_id: String
) : GenericJsonResponse {

    override fun build(): JsonElement =
        Json.encodeToJsonElement(RequestChannelDto(channel_id, message_id))

}

@Serializable
data class RequestChannelDto(val channel_id: String, val message_id: String)
