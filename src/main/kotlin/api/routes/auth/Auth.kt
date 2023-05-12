package api.routes.auth

import api.routes.auth.models.AccessTokenDto
import api.routes.auth.models.LoginDto
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import main.main.Config
import java.util.*
import kotlin.time.Duration.Companion.hours

fun Routing.auth() {
    route("/auth") {
        post("/login") {
            val loginDto = call.receive<LoginDto>()
            if (login(loginDto)) {

                val token = JWT.create()
                    .withClaim("username", loginDto.username)
                    .withIssuedAt(Date())
                    .withExpiresAt(Date(System.currentTimeMillis() + 1.hours.inWholeMilliseconds))
                    .sign(Algorithm.HMAC256(Config.KTOR_API_KEY))
                val tokenDto = AccessTokenDto(token)
                call.respond(tokenDto)
            } else call.respondText("Invalid password!", status = HttpStatusCode.Unauthorized)
        }
    }
}

private fun login(loginDto: LoginDto): Boolean =
    loginDto.password == Config.KTOR_API_KEY