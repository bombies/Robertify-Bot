package api.routes.locale

import api.TEST_SERVER_ID
import api.createClient
import api.defaultTestApplication
import io.ktor.client.request.*
import io.ktor.http.*
import extensions.authorization.Authorized
import extensions.authorization.AuthorizedRouteExtension
import main.utils.api.robertify.postWithToken
import kotlin.test.Test
import kotlin.test.assertEquals

@Authorized
class LocaleControllerKtTest {
    private val accessToken = AuthorizedRouteExtension.ACCESS_TOKEN

    @Test
    fun testLocalePostRoute() = defaultTestApplication {
        val client = createClient()

        client.postWithToken("/locale", accessToken) {
            contentType(ContentType.Application.Json)
            setBody(LocaleDto(TEST_SERVER_ID, "spanish"))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        client.postWithToken("/locale", accessToken) {
            contentType(ContentType.Application.Json)
            setBody(LocaleDto(TEST_SERVER_ID, "en"))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        client.postWithToken("/locale", accessToken) {
            contentType(ContentType.Application.Json)
            setBody(LocaleDto("000000000000000000", "en"))
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }

        client.postWithToken("/locale", accessToken) {
            contentType(ContentType.Application.Json)
            setBody(LocaleDto("invalid_id", "en"))
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }

        client.postWithToken("/locale", accessToken) {
            contentType(ContentType.Application.Json)
            setBody(LocaleDto("000000000000000000", "invalid_locale"))
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }
}