package main.utils.api.robertify.imagebuilders

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import main.main.Config
import main.utils.api.robertify.imagebuilders.models.ImageQueryField
import okhttp3.OkHttpClient
import org.apache.http.client.utils.URIBuilder
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.UUID
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeUnit

abstract class AbstractImageBuilder protected constructor(imageType: ImageType) {
    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
        private const val DEFAULT_TIMEOUT = 2L
        val RANDOM_FILE_NAME: String
            get() = "${UUID.randomUUID()}.png"
    }

    private val httpClient = HttpClient(CIO) {
        engine {
            requestTimeout = DEFAULT_TIMEOUT * 1000
        }
    }
    private var uri: URIBuilder

    init {
        val segments = listOf("api", "images").toMutableList()
        segments.addAll(imageType.getSegments())
        uri = URIBuilder(Config.ROBERTIFY_WEB_HOSTNAME)
            .setPathSegments(segments)
    }

    protected fun addQuery(key: ImageQueryField, value: String) =
        uri.addParameter(key.toString(), value)

    protected fun findQuery(key: ImageQueryField) =
        uri.queryParams
            .find { it.name.equals(key.toString(), true) }
            ?.value

    open suspend fun build(): InputStream? {
        val url = uri.build().toURL().toString()

        return try {
            httpClient.get(url).body<InputStream>()
        } catch (e: SocketTimeoutException) {
            throw ImageBuilderException(cause = e)
        } catch (e: ConnectException) {
            throw ImageBuilderException(cause = e)
        } catch (e: CompletionException) {
            throw ImageBuilderException(cause = e)
        } catch (e: IOException) {
            logger.error("Unexpected error", e)
            null
        }
    }
}