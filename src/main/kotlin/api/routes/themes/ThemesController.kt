package api.routes.themes

import api.plugins.routeWithJwt
import api.routes.themes.dto.ThemeDto
import api.utils.respond
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import net.dv8tion.jda.api.sharding.ShardManager
import org.koin.ktor.ext.inject

fun Routing.themes() {
    val shardManager: ShardManager by inject()
    val server = ThemesService(shardManager)

    routeWithJwt("/themes") {
        post {
            val themeDto = call.receive<ThemeDto>()
            call.respond(server.updateTheme(themeDto))
        }
    }
}