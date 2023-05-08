package api.routes.requestchannel.dto

import kotlinx.serialization.Serializable

@Serializable
data class ToggleRequestChannelButtonsDto(
    val server_id: String,
    val buttons: List<String>
)
