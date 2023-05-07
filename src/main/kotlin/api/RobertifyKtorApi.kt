package api

import api.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import main.main.ConfigKt

object RobertifyKtorApi {

    fun start() {
        embeddedServer(
            Netty,
            port = ConfigKt.KTOR_API_PORT,
            host = "0.0.0.0",
            module = Application::module
        ).start(wait = true)
    }

}

fun Application.module(test: Boolean = false) {
    configureKoin(test)
    configureSerialization()
    configureAuthentication()
    configureExceptionHandling()
    configureRouting()
}