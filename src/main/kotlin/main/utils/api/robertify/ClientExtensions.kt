package main.utils.api.robertify

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single
import org.slf4j.LoggerFactory
import java.net.ConnectException

private val logger = LoggerFactory.getLogger("ClientExtensions")

suspend inline fun <reified T> HttpClient.post(
    path: String,
    contentType: ContentType = ContentType.Application.Json,
    crossinline block: HttpRequestBuilder.() -> Unit,
    noinline errorHandler: (suspend ResponseException.() -> Unit)? = null,
): T? = handleGenericMethodWithError(
    block = {
        val response = post(path) {
            contentType(contentType)
            block()
        }

        return@handleGenericMethodWithError if (response is T) {
            response
        } else response.body<T>()
    },
    errorHandler = {
        errorHandler?.invoke(this)
        val response = this.response
        return@handleGenericMethodWithError if (response is T) response else null
    }
)

suspend inline fun <reified T> HttpClient.postWithToken(
    path: String,
    token: String,
    contentType: ContentType = ContentType.Application.Json,
    crossinline block: HttpRequestBuilder.() -> Unit,
    noinline errorHandler: (suspend ResponseException.() -> T)? = null,
) = handleGenericMethodWithError(
    block = {
        val response = post(path) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
            contentType(contentType)
            block()
        }

        return@handleGenericMethodWithError if (response is T) {
            response
        } else response.body<T>()
    },
    errorHandler = errorHandler
)

suspend inline fun <reified T> HttpClient.postWithToken(
    path: String,
    flowToken: Flow<String>,
    contentType: ContentType = ContentType.Application.Json,
    crossinline block: HttpRequestBuilder.() -> Unit,
    noinline errorHandler: (suspend ResponseException.() -> T)? = null,
) = handleGenericMethodWithError(
    block = {
        val token = flowToken.single()

        val response = post(path) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
            contentType(contentType)
            block()
        }

        return@handleGenericMethodWithError if (response is T) {
            response
        } else response.body<T>()
    },
    errorHandler = errorHandler
)

suspend fun <T> HttpClient.handleGenericMethodWithError(
    block: suspend HttpClient.() -> T,
    errorHandler: (suspend ResponseException.() -> T)? = null
): T? = runCatching {
    block(this)
}.getOrElse {
    when (it) {
        is ResponseException -> if (errorHandler != null) errorHandler(it) else throw it
        is ConnectException -> {
            logger.error("I couldn't make a request because the connection doesn't exist!")
            null
        }

        else -> throw it
    }
}


suspend fun HttpClient.deleteWithToken(
    path: String,
    token: String,
    contentType: ContentType = ContentType.Application.Json,
    block: HttpRequestBuilder.() -> Unit
) = delete(path) {
    headers {
        append(HttpHeaders.Authorization, "Bearer $token")
    }
    contentType(contentType)
    block()
}

suspend inline fun <reified T> HttpClient.deleteWithToken(
    path: String,
    token: String,
    contentType: ContentType = ContentType.Application.Json,
    crossinline block: HttpRequestBuilder.() -> Unit,
    noinline errorHandler: (suspend ResponseException.() -> T)? = null,
) = handleGenericMethodWithError(
    block = {
        val response = delete(path) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
            contentType(contentType)
            block()
        }

        return@handleGenericMethodWithError if (response is T) {
            response
        } else response.body<T>()
    },
    errorHandler = errorHandler
)

suspend fun HttpClient.getWithToken(
    path: String,
    token: String,
    contentType: ContentType = ContentType.Application.Json,
    block: HttpRequestBuilder.() -> Unit
) = get(path) {
    headers {
        append(HttpHeaders.Authorization, "Bearer $token")
    }
    contentType(contentType)
    block()
}

suspend inline fun <reified T> HttpClient.getWithToken(
    path: String,
    token: String,
    contentType: ContentType = ContentType.Application.Json,
    crossinline block: HttpRequestBuilder.() -> Unit,
    noinline errorHandler: (suspend ResponseException.() -> T)? = null,
) = handleGenericMethodWithError(
    block = {
        val response = get(path) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
            contentType(contentType)
            block()
        }

        return@handleGenericMethodWithError if (response is T) {
            response
        } else response.body<T>()
    },
    errorHandler = errorHandler
)

suspend fun HttpClient.patchWithToken(
    path: String,
    token: String,
    contentType: ContentType = ContentType.Application.Json,
    block: HttpRequestBuilder.() -> Unit,
) =
    patch(path) {
        headers {
            append(HttpHeaders.Authorization, "Bearer $token")
        }
        contentType(contentType)
        block()
    }

suspend inline fun <reified T> HttpClient.patchWithToken(
    path: String,
    token: String,
    contentType: ContentType = ContentType.Application.Json,
    crossinline block: HttpRequestBuilder.() -> Unit,
    noinline errorHandler: (suspend ResponseException.() -> T)? = null,
) = handleGenericMethodWithError(
    block = {
        val response = patch(path) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
            contentType(contentType)
            block()
        }

        return@handleGenericMethodWithError if (response is T) {
            response
        } else response.body<T>()
    },
    errorHandler = errorHandler
)

fun httpClient(host: String, timeout: Long = 5000): HttpClient = HttpClient(CIO) {
    install(HttpTimeout) {
        requestTimeoutMillis = timeout
        connectTimeoutMillis = timeout
        socketTimeoutMillis = timeout
    }

    install(ContentNegotiation) {
        json()
    }

    defaultRequest {
        url(host)
    }
}