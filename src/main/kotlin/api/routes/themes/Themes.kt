package api.routes.themes

import api.models.response.ExceptionResponse
import api.models.response.OkResponse
import api.plugins.routeWithJwt
import api.routes.themes.dto.ThemeDto
import api.utils.GuildUtils
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import main.commands.slashcommands.management.ThemeCommandKt
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

    routeWithJwt("/themes") {
        post {
            val themeDto = call.receive<ThemeDto>()
            val guild = GuildUtils(shardManager).getGuild(themeDto.server_id) ?:
            return@post call.respond(HttpStatusCode.NotFound,
                ExceptionResponse(
                    reason = "There was no guild with id: ${themeDto.server_id}",
                    status = HttpStatusCode.NotFound
                )
            )

            ThemeCommandKt().updateTheme(guild, RobertifyThemeKt.parse(themeDto.theme))
            call.respond(HttpStatusCode.OK, OkResponse(
                message = "Successfully set the theme for ${guild.name} to ${themeDto.theme.uppercase()}"
            ))
        }
    }
}