package api.routes.themes

import api.TEST_SERVER_ID
import api.createClient
import api.defaultTestApplication
import api.module
import api.routes.auth.models.AccessTokenDto
import api.routes.auth.models.LoginDto
import api.routes.themes.dto.ThemeDto
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import main.main.ConfigKt
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemesControllerKtTest {

    @Test
    fun testPostTheme() = defaultTestApplication {
        val client = createClient()

        // Test no auth
        client.post("/themes") {
            contentType(ContentType.Application.Json)
            setBody(ThemeDto("000000000000000000", "green"))
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }

        client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginDto("user", ConfigKt.KTOR_API_KEY))
        }.apply {
            val accessTokenDto = body<AccessTokenDto>()

            // Testing a guild that doesn't exist.
            client.post("/themes") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${accessTokenDto.access_token}")
                }
                contentType(ContentType.Application.Json)
                setBody(ThemeDto("000000000000000000", "green"))
            }.apply {
                assertEquals(HttpStatusCode.NotFound, status)
            }

            // Testing a colour that's invalid
            client.post("/themes") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${accessTokenDto.access_token}")
                }
                contentType(ContentType.Application.Json)
                setBody(ThemeDto("000000000000000000", "aquamarine"))
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, status)
            }

            // Testing a guild that does exist.
            client.post("/themes") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${accessTokenDto.access_token}")
                }
                contentType(ContentType.Application.Json)
                setBody(ThemeDto(TEST_SERVER_ID, "green"))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
            }
        }
    }

}