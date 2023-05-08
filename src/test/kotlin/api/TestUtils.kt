package api

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*

internal const val TEST_SERVER_ID = "955155414843019374"

fun ApplicationTestBuilder.createClient(headers: (HeadersBuilder.() -> Unit)? = null) =
    createClient {
        install(ContentNegotiation) {
            json()
        }

        if (headers != null)
            headers { headers() }
    }

fun ApplicationTestBuilder.createClientWithToken(token: String) =
    createClient(headers = {
        append(HttpHeaders.Authorization, "Bearer $token")
    })

inline fun defaultTestApplication(crossinline handle: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
    application {
        module(true)
    }

    handle(this)
}
