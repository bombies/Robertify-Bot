package api.routes.themes.dto

import io.ktor.server.plugins.requestvalidation.*
import kotlinx.serialization.Serializable
import main.constants.RobertifyThemeKt
import main.utils.GeneralUtilsKt.isDiscordId

@Serializable
data class ThemeDto(
    val server_id: String,
    val theme: String
)

fun RequestValidationConfig.validateThemeDto() {
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
}
