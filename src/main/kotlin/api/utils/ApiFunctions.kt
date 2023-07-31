package api.utils

import api.models.response.GenericResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

suspend fun ApplicationCall.respond(response: GenericResponse) =
    respond(HttpStatusCode(response.status, response.message), response)