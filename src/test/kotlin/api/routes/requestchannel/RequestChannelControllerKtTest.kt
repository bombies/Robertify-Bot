package api.routes.requestchannel

import api.TEST_SERVER_ID
import api.createClient
import api.defaultTestApplication
import api.module
import api.routes.auth.models.AccessTokenDto
import api.routes.auth.models.LoginDto
import api.routes.requestchannel.dto.CreateRequestChannelDto
import api.routes.requestchannel.dto.ToggleRequestChannelButtonDto
import api.routes.requestchannel.dto.ToggleRequestChannelButtonsDto
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.request
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import main.main.ConfigKt
import kotlin.test.Test
import kotlin.test.assertEquals

class RequestChannelControllerKtTest {

    @Test
    fun testCreateNewChannel() = defaultTestApplication {
        val client = createClient()

        client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginDto("user", ConfigKt.KTOR_API_KEY))
        }.apply {
            val accessTokenDto = body<AccessTokenDto>()

            // Create the channel
            client.post("/reqchannel") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${accessTokenDto.access_token}")
                }
                contentType(ContentType.Application.Json)
                setBody(CreateRequestChannelDto(TEST_SERVER_ID))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
            }

            // Toggle some buttons
            client.post("/reqchannel/button") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${accessTokenDto.access_token}")
                }
                contentType(ContentType.Application.Json)
                setBody(ToggleRequestChannelButtonDto(TEST_SERVER_ID, "pnp"))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
            }

            client.post("/reqchannel/button") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${accessTokenDto.access_token}")
                }
                contentType(ContentType.Application.Json)
                setBody(ToggleRequestChannelButtonDto(TEST_SERVER_ID, "invalidbutton"))
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, status)
            }

            client.post("/reqchannel/buttons") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${accessTokenDto.access_token}")
                }
                contentType(ContentType.Application.Json)
                setBody(ToggleRequestChannelButtonsDto(TEST_SERVER_ID, listOf("pnp", "skip", "shuffle")))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
            }

            // Delete the channel
            client.delete("/reqchannel/$TEST_SERVER_ID") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${accessTokenDto.access_token}")
                }
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
            }
        }

    }
}