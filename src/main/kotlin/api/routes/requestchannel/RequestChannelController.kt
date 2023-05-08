package api.routes.requestchannel

import api.plugins.routeWithJwt
import api.routes.requestchannel.dto.CreateRequestChannelDto
import api.utils.respond
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import main.utils.GeneralUtilsKt.isDiscordId
import net.dv8tion.jda.api.sharding.ShardManager
import org.koin.ktor.ext.inject

fun Routing.requestChannel() {
    install(RequestValidation) {
        validate<CreateRequestChannelDto> { dto ->
            if (!dto.server_id.isDiscordId())
                ValidationResult.Invalid("The server id must be a valid Discord id!")
            ValidationResult.Valid
        }
    }

    val shardManager: ShardManager by inject()
    val service = RequestChannelService(shardManager)

    routeWithJwt("/reqchannel") {
        post {
            val dto = call.receive<CreateRequestChannelDto>()
            call.respond(service.createChannel(dto))
        }

        post("button") {

        }

        post("buttons") {

        }

        delete("{id}") {
            val id = call.parameters["id"]!!
            call.respond(service.deleteChannel(id))
        }
    }
}