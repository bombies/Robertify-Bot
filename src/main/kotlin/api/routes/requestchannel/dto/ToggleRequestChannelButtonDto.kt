package api.routes.requestchannel.dto

import kotlinx.serialization.Serializable

@Serializable
data class ToggleRequestChannelButtonDto(
    val server_id: String,
    val button: String
)
