package api.routes.requestchannel

import api.plugins.routeWithJwt
import api.routes.requestchannel.dto.CreateRequestChannelDto
import api.routes.requestchannel.dto.ToggleRequestChannelButtonDto
import api.routes.requestchannel.dto.ToggleRequestChannelButtonsDto
import api.utils.respond
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import net.dv8tion.jda.api.sharding.ShardManager
import org.koin.ktor.ext.inject

fun Routing.requestChannel() {
    val shardManager: ShardManager by inject()
    val service = RequestChannelService(shardManager)

    routeWithJwt("/reqchannel") {
        post {
            val dto = call.receive<CreateRequestChannelDto>()
            call.respond(service.createChannel(dto))
        }

        patch("button") {
            val dto = call.receive<ToggleRequestChannelButtonDto>()
            call.respond(service.toggleButton(dto))
        }

        patch("buttons") {
            val dto = call.receive<ToggleRequestChannelButtonsDto>()
            call.respond(service.toggleButtons(dto))
        }

        delete("{id}") {
            val id = call.parameters["id"]!!
            call.respond(service.deleteChannel(id))
        }
    }
}