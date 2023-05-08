package api.routes.requestchannel.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateRequestChannelDto(val server_id: String)
