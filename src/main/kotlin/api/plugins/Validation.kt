package api.plugins

import api.routes.requestchannel.dto.*
import api.routes.themes.dto.validateThemeDto
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*

fun Application.configureValidation() {
    install(RequestValidation) {
        validateThemeDto()
        validateCreateRequestChannelDto()
        validateToggleRequestChannelButtonDto()
        validateToggleRequestChannelButtonsDto()
    }
}