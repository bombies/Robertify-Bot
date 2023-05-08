package api.routes.themes

import api.TEST_SERVER_ID
import api.createClient
import api.defaultTestApplication
import api.postWithToken
import api.routes.auth.models.AccessTokenDto
import api.routes.auth.models.LoginDto
import api.routes.themes.dto.ThemeDto
import extensions.authorization.Authorized
import extensions.authorization.AuthorizedRouteExtension
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import main.main.ConfigKt
import kotlin.test.Test
import kotlin.test.assertEquals

@Authorized
class ThemesControllerKtTest {
    private val accessToken = AuthorizedRouteExtension.ACCESS_TOKEN

    @Test
    fun testPostTheme() = defaultTestApplication {
        val client = createClient()

        // Testing a guild that doesn't exist.
        client.postWithToken("/themes", accessToken) {
            contentType(ContentType.Application.Json)
            setBody(ThemeDto("000000000000000000", "green"))
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }

        // Testing a colour that's invalid
        client.postWithToken("/themes", accessToken) {
            contentType(ContentType.Application.Json)
            setBody(ThemeDto("000000000000000000", "aquamarine"))
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }

        // Testing a guild that does exist.
        client.postWithToken("/themes", accessToken) {
            contentType(ContentType.Application.Json)
            setBody(ThemeDto(TEST_SERVER_ID, "green"))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

}