package api.routes.requestchannel.dto

import io.ktor.server.plugins.requestvalidation.*
import kotlinx.serialization.Serializable
import main.utils.GeneralUtils.isDiscordId

@Serializable
data class ToggleRequestChannelButtonsDto(
    val server_id: String,
    val buttons: List<String>
)

fun RequestValidationConfig.validateToggleRequestChannelButtonsDto() {
    validate<ToggleRequestChannelButtonsDto> { dto ->
        if (!dto.server_id.isDiscordId())
            return@validate ValidationResult.Invalid("The server id must be a valid Discord id!")

        dto.buttons.forEach { s ->
            when (s.lowercase()) {
                "previous", "rewind", "stop",
                "pnp", "skip", "favourite", "loop",
                "shuffle", "disconnect", "filters" -> ValidationResult.Valid

                else -> return@validate ValidationResult.Invalid("$s is an invalid button!")
            }
        }

        ValidationResult.Valid
    }
}
