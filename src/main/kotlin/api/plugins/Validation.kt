package api.plugins

import api.routes.requestchannel.dto.CreateRequestChannelDto
import api.routes.themes.dto.ThemeDto
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import main.constants.RobertifyThemeKt
import main.utils.GeneralUtilsKt.isDiscordId

fun Application.configureValidation() {
    install(RequestValidation) {
        validate<ThemeDto> { themeDto ->
            if (!themeDto.server_id.isDiscordId())
                ValidationResult.Invalid("The server id must be a valid Discord id!")

            try {
                RobertifyThemeKt.parse(themeDto.theme)
                ValidationResult.Valid
            } catch (e: IllegalArgumentException) {
                ValidationResult.Invalid("${themeDto.theme} is an invalid theme!")
            }
        }

        validate<CreateRequestChannelDto> { dto ->
            if (!dto.server_id.isDiscordId())
                ValidationResult.Invalid("The server id must be a valid Discord id!")
            ValidationResult.Valid
        }
    }
}