package main.utils.api.robertify

import api.routes.auth.models.AccessTokenDto
import api.routes.auth.models.LoginDto
import dev.minn.jda.ktx.util.SLF4J
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import main.main.ConfigKt
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class RobertifyApi {

    companion object {
        private val logger by SLF4J
        private val TOKEN_REFRESH_INTERVAL = 12.hours.inWholeMilliseconds
        private val DEFAULT_TIMEOUT = 5.seconds.inWholeMilliseconds
        private val HOST_NAME = ConfigKt.ROBERTIFY_API_HOSTNAME
        private val executorService = Executors.newSingleThreadScheduledExecutor()
    }

    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = DEFAULT_TIMEOUT
            connectTimeoutMillis = DEFAULT_TIMEOUT
            socketTimeoutMillis = DEFAULT_TIMEOUT
        }

        install(ContentNegotiation) {
            json()
        }

        defaultRequest {
            url(HOST_NAME)
        }
    }

    private var accessToken: Flow<String> = flow {
        emit(getAccessToken())
    }

    init {
        executorService.scheduleAtFixedRate(
            {
                accessToken = flow {
                    emit(getAccessToken())
                }
            },
            TOKEN_REFRESH_INTERVAL,
            TOKEN_REFRESH_INTERVAL,
            TimeUnit.MILLISECONDS
        )
    }

    private suspend fun getAccessToken(): String {
        val dto = client.get("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginDto("bombies", ConfigKt.ROBERTIFY_API_PASSWORD))
        }.body<AccessTokenDto>()
        return "Bearer ${dto.access_token}"
    }

    suspend fun postCommandInfo(commandInfo: JSONObject) =
        client.postWithToken<HttpResponse>(
            "commands",
            accessToken,
            block = {
                setBody(commandInfo.toString())
            },
            errorHandler = {
                logger.error("API error", this)
                response
            }
        )

}