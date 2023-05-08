package api.routes.requestchannel.responses

import api.models.response.GenericJsonResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.json.JSONObject

data class RequestChannelCreationResponse(
    private val channel_id: String,
    private val message_id: String
) : GenericJsonResponse {

    override fun build(): JsonElement =
        Json.encodeToJsonElement(
            JSONObject()
                .put("channel_id", channel_id)
                .put("message_id", message_id)
                .toString()
        )

}
