package api.routes.auth

import api.defaultTestApplication
import api.module
import api.routes.auth.models.AccessTokenDto
import api.routes.auth.models.LoginDto
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import main.main.ConfigKt
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AuthKtTest {

    @Test
    fun testPostAuthLogin() = defaultTestApplication {
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginDto("user", ConfigKt.KTOR_API_KEY))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertDoesNotThrow {
                body<AccessTokenDto>()
            }
        }

        client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginDto("user", "Blatantly incorrect key"))
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
            assertFailsWith<NoTransformationFoundException> {
                body<AccessTokenDto>()
            }
        }
    }
}