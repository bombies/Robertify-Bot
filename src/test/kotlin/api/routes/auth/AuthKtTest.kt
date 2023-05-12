package api.routes.auth

import api.defaultTestApplication
import api.routes.auth.models.AccessTokenDto
import api.routes.auth.models.LoginDto
import api.routes.themes.dto.ThemeDto
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import main.main.Config
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
            setBody(LoginDto("user", Config.KTOR_API_KEY))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertDoesNotThrow {
                runBlocking {
                    body<AccessTokenDto>()
                }
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

        // Test no auth
        client.post("/themes") {
            contentType(ContentType.Application.Json)
            setBody(ThemeDto("000000000000000000", "green"))
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }
}