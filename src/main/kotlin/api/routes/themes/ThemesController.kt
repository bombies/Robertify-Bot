package api.routes.themes

import api.plugins.routeWithJwt
import api.routes.themes.dto.ThemeDto
import api.utils.respond
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import main.constants.RobertifyThemeKt
import main.utils.GeneralUtilsKt.isDiscordId
import net.dv8tion.jda.api.sharding.ShardManager
import org.koin.ktor.ext.inject

fun Routing.themes() {
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
    }

    val shardManager: ShardManager by inject()
    val server = ThemesService(shardManager)

    routeWithJwt("/themes") {
        post {
            val themeDto = call.receive<ThemeDto>()
            call.respond(server.updateTheme(themeDto))
        }
    }
}