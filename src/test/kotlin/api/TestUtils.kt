package api

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*

internal const val TEST_SERVER_ID = "955155414843019374"

fun ApplicationTestBuilder.createClient() =
    createClient {
        install(ContentNegotiation) {
            json()
        }
    }

inline fun defaultTestApplication(crossinline handle: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
    application {
        module(true)
    }

    handle(this)
}
