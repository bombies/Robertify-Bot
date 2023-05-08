package api.routes.requestchannel

import api.*
import api.routes.requestchannel.dto.CreateRequestChannelDto
import api.routes.requestchannel.dto.ToggleRequestChannelButtonDto
import api.routes.requestchannel.dto.ToggleRequestChannelButtonsDto
import extensions.authorization.Authorized
import extensions.authorization.AuthorizedRouteExtension
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import main.utils.api.robertify.deleteWithToken
import main.utils.api.robertify.patchWithToken
import main.utils.api.robertify.postWithToken
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import kotlin.test.Test
import kotlin.test.assertEquals

@Authorized
@TestMethodOrder(OrderAnnotation::class)
class RequestChannelControllerKtTest {
    private val accessToken = AuthorizedRouteExtension.ACCESS_TOKEN

    @Test
    @Order(1)
    fun testCreateNewChannel() = defaultTestApplication {
        val client = createClient()

        // Create the channel
        client.postWithToken("/reqchannel", accessToken) {
            contentType(ContentType.Application.Json)
            setBody(CreateRequestChannelDto(TEST_SERVER_ID))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    @Order(2)
    fun testButtonUpdates() = defaultTestApplication {
        val client = createClient()

        // Toggle some buttons
        client.patchWithToken("/reqchannel/button", accessToken) {
            contentType(ContentType.Application.Json)
            setBody(ToggleRequestChannelButtonDto(TEST_SERVER_ID, "pnp"))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        client.patchWithToken("/reqchannel/button", accessToken) {
            contentType(ContentType.Application.Json)
            setBody(ToggleRequestChannelButtonDto(TEST_SERVER_ID, "invalidbutton"))
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }

        client.patchWithToken("/reqchannel/buttons", accessToken) {
            contentType(ContentType.Application.Json)
            setBody(ToggleRequestChannelButtonsDto(TEST_SERVER_ID, listOf("pnp", "skip", "shuffle")))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    @Order(3)
    fun testDeleteChannel() = defaultTestApplication {
        val client = createClient()

        // Delete the channel
        client.deleteWithToken("/reqchannel/$TEST_SERVER_ID", accessToken) {
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
}