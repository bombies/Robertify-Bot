package extensions.authorization

import api.createClient
import api.defaultTestApplication
import api.routes.auth.models.AccessTokenDto
import api.routes.auth.models.LoginDto
import dev.minn.jda.ktx.util.SLF4J
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import main.main.ConfigKt
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Store

class AuthorizedRouteExtension : BeforeAllCallback {
    companion object {
        var ACCESS_TOKEN: String = ""
        val logger by SLF4J
    }

    override fun beforeAll(context: ExtensionContext) {
        val enabled = context.requiredTestClass
            .annotations
            .filterIsInstance<Authorized>()
            .isNotEmpty()

        if (enabled)
            defaultTestApplication {
                val client = createClient()
                val dto = client.post("/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(LoginDto("user", ConfigKt.KTOR_API_KEY))
                }.body<AccessTokenDto>()
                ACCESS_TOKEN = dto.access_token
                getStore(context).put("ACCESS_TOKEN", dto.access_token)
            }
    }

    private fun getStore(context: ExtensionContext): Store =
        context.getStore(ExtensionContext.Namespace.create(javaClass, context.requiredTestClass))

}