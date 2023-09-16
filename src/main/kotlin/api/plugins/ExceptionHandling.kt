package api.plugins

import api.models.response.ExceptionResponse
import api.utils.respond
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory

fun Application.configureExceptionHandling() {
    val logger = LoggerFactory.getLogger("ExceptionHandling")

    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ExceptionResponse(
                    reason = cause.reasons.joinToString(", "),
                    status = HttpStatusCode.BadRequest
                )
            )
        }

        exception<BadRequestException> { call, cause  ->
            call.respond(ExceptionResponse(
                reason = "Invalid request! Ensure the body is accurate before sending another request!",
                status = HttpStatusCode.BadRequest
            ))
        }

        exception<Exception> { call, cause ->
            logger.error("Unexpected error", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ExceptionResponse(
                    reason = "Internal Server Error",
                    status = HttpStatusCode.InternalServerError
                )
            )
        }
    }
}