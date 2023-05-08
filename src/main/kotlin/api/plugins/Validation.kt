package api.plugins

import api.routes.requestchannel.dto.CreateRequestChannelDto
import api.routes.requestchannel.dto.ToggleRequestChannelButtonDto
import api.routes.requestchannel.dto.ToggleRequestChannelButtonsDto
import api.routes.themes.dto.ThemeDto
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import main.constants.RobertifyThemeKt
import main.utils.GeneralUtilsKt.isDiscordId

fun Application.configureValidation() {
    install(RequestValidation) {
        validate<ThemeDto> { themeDto ->
            if (!themeDto.server_id.isDiscordId())
                return@validate ValidationResult.Invalid("The server id must be a valid Discord id!")

            try {
                RobertifyThemeKt.parse(themeDto.theme)
                ValidationResult.Valid
            } catch (e: IllegalArgumentException) {
                ValidationResult.Invalid("${themeDto.theme} is an invalid theme!")
            }
        }

        validate<CreateRequestChannelDto> { dto ->
            if (!dto.server_id.isDiscordId())
                return@validate ValidationResult.Invalid("The server id must be a valid Discord id!")
            ValidationResult.Valid
        }

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
}