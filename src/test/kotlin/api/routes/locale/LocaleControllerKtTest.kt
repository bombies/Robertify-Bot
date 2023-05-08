package api.routes.locale

import api.*
import api.TEST_SERVER_ID
import api.routes.auth.models.AccessTokenDto
import api.routes.auth.models.LoginDto
import dev.minn.jda.ktx.util.SLF4J
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import main.main.ConfigKt
import org.junit.jupiter.api.BeforeAll
import kotlin.test.Test
import kotlin.test.assertEquals

class LocaleControllerKtTest {

    companion object {
        private lateinit var ACCESS_TOKEN: String
        private val logger by SLF4J

        @JvmStatic
        @BeforeAll
        fun setAccessToken() = defaultTestApplication {
            val client = createClient()
            val dto = client.post("/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginDto("user", ConfigKt.KTOR_API_KEY))
            }.body<AccessTokenDto>()
            ACCESS_TOKEN = dto.access_token
        }
    }

    @Test
    fun testLocalePostRoute() = defaultTestApplication {
        val client = createClient()

        client.postWithToken("/locale", ACCESS_TOKEN) {
            contentType(ContentType.Application.Json)
            setBody(LocaleDto(TEST_SERVER_ID, "spanish"))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        client.postWithToken("/locale", ACCESS_TOKEN) {
            contentType(ContentType.Application.Json)
            setBody(LocaleDto(TEST_SERVER_ID, "en"))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        client.postWithToken("/locale", ACCESS_TOKEN) {
            contentType(ContentType.Application.Json)
            setBody(LocaleDto("000000000000000000", "en"))
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }

        client.postWithToken("/locale", ACCESS_TOKEN) {
            contentType(ContentType.Application.Json)
            setBody(LocaleDto("invalid_id", "en"))
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }

        client.postWithToken("/locale", ACCESS_TOKEN) {
            contentType(ContentType.Application.Json)
            setBody(LocaleDto("000000000000000000", "invalid_locale"))
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }
}