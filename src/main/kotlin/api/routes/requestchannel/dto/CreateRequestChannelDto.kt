package api.routes.requestchannel.dto

import io.ktor.server.plugins.requestvalidation.*
import kotlinx.serialization.Serializable
import main.utils.GeneralUtils.isDiscordId

@Serializable
data class CreateRequestChannelDto(val server_id: String)

fun RequestValidationConfig.validateCreateRequestChannelDto() {
    validate<CreateRequestChannelDto> { dto ->
        if (!dto.server_id.isDiscordId())
            return@validate ValidationResult.Invalid("The server id must be a valid Discord id!")
        ValidationResult.Valid
    }
}
