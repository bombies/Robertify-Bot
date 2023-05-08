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

suspend fun HttpClient.postWithToken(path: String, token: String, block: HttpRequestBuilder.() -> Unit) =
    post(path) {
        headers {
            append(HttpHeaders.Authorization, "Bearer $token")
        }
        block()
    }

suspend fun HttpClient.deleteWithToken(path: String, token: String, block: HttpRequestBuilder.() -> Unit) =
    delete(path) {
        headers {
            append(HttpHeaders.Authorization, "Bearer $token")
        }
        block()
    }

suspend fun HttpClient.getWithToken(path: String, token: String, block: HttpRequestBuilder.() -> Unit) =
    get(path) {
        headers {
            append(HttpHeaders.Authorization, "Bearer $token")
        }
        block()
    }

suspend fun HttpClient.patchWithToken(path: String, token: String, block: HttpRequestBuilder.() -> Unit) =
    patch(path) {
        headers {
            append(HttpHeaders.Authorization, "Bearer $token")
        }
        block()
    }

inline fun defaultTestApplication(crossinline handle: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
    application {
        module(true)
    }

    handle(this)
}
