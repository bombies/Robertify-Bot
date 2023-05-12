package api.routes.locale

import io.ktor.server.plugins.requestvalidation.*
import kotlinx.serialization.Serializable
import main.utils.GeneralUtils.isDiscordId
import main.utils.locale.RobertifyLocale

@Serializable
data class LocaleDto(
    val server_id: String,
    val locale: String
)

fun RequestValidationConfig.validateLocaleDto() {
    validate<LocaleDto> {  dto ->
        if (!dto.server_id.isDiscordId())
            return@validate ValidationResult.Invalid("The server id must be a valid Discord id!")

        try {
            RobertifyLocale.parse(dto.locale.lowercase())
            ValidationResult.Valid
        } catch (e: IllegalArgumentException) {
            ValidationResult.Invalid("${dto.locale} is an invalid locale!")
        }
    }
}
