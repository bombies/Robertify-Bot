package api.routes.locale

import api.TEST_SERVER_ID
import api.createClient
import api.defaultTestApplication
import io.ktor.client.request.*
import io.ktor.http.*
import extensions.authorization.Authorized
import extensions.authorization.AuthorizedRouteExtension
import io.ktor.client.statement.*
import main.utils.api.robertify.postWithToken
import kotlin.test.Test
import kotlin.test.assertEquals

@Authorized
class LocaleControllerKtTest {
    private val accessToken = AuthorizedRouteExtension.ACCESS_TOKEN

    @Test
    fun testLocalePostRoute() = defaultTestApplication {
        val client = createClient()

        client.postWithToken<HttpResponse>(
            path = "/locale",
            token = accessToken,
            block = {
                setBody(LocaleDto(TEST_SERVER_ID, "spanish"))
            }
        ).apply {
            assertEquals(HttpStatusCode.OK, this!!.status)
        }

        client.postWithToken<HttpResponse>(
            path = "/locale",
            token = accessToken,
            block = {
                setBody(LocaleDto(TEST_SERVER_ID, "en"))

            }
        ).apply {
            assertEquals(HttpStatusCode.OK, this!!.status)
        }

        client.postWithToken<HttpResponse>(
            path = "/locale",
            token = accessToken,
            block = {
                setBody(LocaleDto("000000000000000000", "en"))
            }
        ).apply {
            assertEquals(HttpStatusCode.NotFound, this!!.status)
        }

        client.postWithToken<HttpResponse>(
            path = "/locale",
            token = accessToken,
            block = {
                setBody(LocaleDto("invalid_id", "en"))
            }
        ).apply {
            assertEquals(HttpStatusCode.BadRequest, this!!.status)
        }

        client.postWithToken<HttpResponse>(
            path = "/locale",
            token = accessToken,
            block = {
                setBody(LocaleDto("000000000000000000", "invalid_locale"))

            }
        ).apply {
            assertEquals(HttpStatusCode.BadRequest, this!!.status)
        }
    }
}