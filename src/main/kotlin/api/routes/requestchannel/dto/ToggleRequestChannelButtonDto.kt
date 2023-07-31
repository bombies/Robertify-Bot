package api.routes.requestchannel.dto

import io.ktor.server.plugins.requestvalidation.*
import kotlinx.serialization.Serializable
import main.utils.GeneralUtils.isDiscordId

@Serializable
data class ToggleRequestChannelButtonDto(
    val server_id: String,
    val button: String
)

fun RequestValidationConfig.validateToggleRequestChannelButtonDto() {
    validate<ToggleRequestChannelButtonDto> { dto ->
        if (!dto.server_id.isDiscordId())
            return@validate ValidationResult.Invalid("The server id must be a valid Discord id!")

        when (dto.button.lowercase()) {
            "previous", "rewind", "stop",
            "pnp", "skip", "favourite", "loop",
            "shuffle", "disconnect", "filters" -> ValidationResult.Valid

            else -> ValidationResult.Invalid("${dto.button} is an invalid button!")
        }
    }
}
