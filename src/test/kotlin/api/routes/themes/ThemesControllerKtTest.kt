package api.routes.themes

import api.TEST_SERVER_ID
import api.createClient
import api.defaultTestApplication
import api.routes.themes.dto.ThemeDto
import extensions.authorization.Authorized
import extensions.authorization.AuthorizedRouteExtension
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import main.utils.api.robertify.postWithToken
import kotlin.test.Test
import kotlin.test.assertEquals

@Authorized
class ThemesControllerKtTest {
    private val accessToken = AuthorizedRouteExtension.ACCESS_TOKEN

    @Test
    fun testPostTheme() = defaultTestApplication {
        val client = createClient()

        // Testing a guild that doesn't exist.
        client.postWithToken<HttpResponse>(
            path = "/themes",
            token = accessToken,
            block = {
                setBody(ThemeDto("000000000000000000", "green"))
            }
        ).apply {
            assertEquals(HttpStatusCode.NotFound, this!!.status)
        }

        // Testing a colour that's invalid
        client.postWithToken<HttpResponse>(
            path = "/themes",
            token = accessToken,
            block = {
                setBody(ThemeDto("000000000000000000", "aquamarine"))
            }
        ).apply {
            assertEquals(HttpStatusCode.BadRequest, this!!.status)
        }

        // Testing a guild that does exist.
        client.postWithToken<HttpResponse>(
            path = "/themes",
            token = accessToken,
            block = {
                setBody(ThemeDto(TEST_SERVER_ID, "green"))

            }
        ).apply {
            assertEquals(HttpStatusCode.OK, this!!.status)
        }
    }

}