package api.plugins

import api.routes.auth.auth
import api.routes.themes.themes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Welcome to the API!")
        }
        auth()
        themes()
    }
}