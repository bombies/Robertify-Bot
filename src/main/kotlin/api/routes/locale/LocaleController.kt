package api.routes.locale

import api.plugins.routeWithJwt
import api.utils.respond
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import net.dv8tion.jda.api.sharding.ShardManager
import org.koin.ktor.ext.inject

fun Routing.locale() {
    val shardManager: ShardManager by inject()
    val service = LocaleService(shardManager)

    routeWithJwt("/locale") {
        post {
            val dto = call.receive<LocaleDto>()
            call.respond(service.setLocale(dto))
        }
    }
}