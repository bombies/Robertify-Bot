package api.routes.requestchannel.responses

import api.models.response.GenericJsonResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import main.utils.database.mongodb.cache.redis.guild.RequestChannelConfigModel

data class RequestChannelCreationResponse(
    private val channel_id: String,
    private val message_id: String,
    private val config: RequestChannelConfigModel,
) : GenericJsonResponse {

    override fun build(): JsonElement =
        Json.encodeToJsonElement(RequestChannelDto(channel_id, message_id, config))

}

@Serializable
data class RequestChannelDto(val channel_id: String, val message_id: String, val config: RequestChannelConfigModel)
