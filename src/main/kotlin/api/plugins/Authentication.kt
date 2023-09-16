package api.plugins

import api.models.response.ExceptionResponse
import api.utils.respond
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import io.ktor.util.*
import main.main.Config

const val JWT_AUTH = "auth-jwt"

fun Application.configureAuthentication() {
    val secret = Config.KTOR_API_KEY
    val myRealm = "global_access"

    install(Authentication) {
        jwt(JWT_AUTH) {
            realm = myRealm
            verifier(JWT
                .require(Algorithm.HMAC256(secret))
                .build()
            )

            validate { credential ->
                if (credential.payload.getClaim("username").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }

            challenge { _, _ ->
                call.respond(ExceptionResponse(
                    reason = "Unauthorized",
                    status = HttpStatusCode.Unauthorized
                ))
            }
        }
    }
}

@KtorDsl
fun Routing.routeWithJwt(path: String, routeHandler: Route.() -> Unit): Route {
    return authenticate(JWT_AUTH) {
        route(path) { routeHandler(this) }
    }
}