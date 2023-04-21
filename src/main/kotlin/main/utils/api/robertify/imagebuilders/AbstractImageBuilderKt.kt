package main.utils.api.robertify.imagebuilders

import main.main.ConfigKt
import main.utils.api.robertify.imagebuilders.models.ImageQueryFieldKt
import me.duncte123.botcommons.web.WebUtils
import okhttp3.OkHttpClient
import org.apache.http.client.utils.URIBuilder
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.UUID
import java.util.concurrent.TimeUnit

abstract class AbstractImageBuilderKt protected constructor(imageType: ImageTypeKt) {
    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
        private const val DEFAULT_TIMEOUT = 2L

        fun getRandomFileName(): String = "${UUID.randomUUID()}.png"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
        .build();
    private val webUtils = WebUtils.ins
    private var uri: URIBuilder

    init {
        val segments = listOf("api", "images").toMutableList()
        segments.addAll(imageType.getSegments())
        uri = URIBuilder(ConfigKt.ROBERTIFY_API_HOSTNAME)
            .setPathSegments(segments)
    }

    protected fun addQuery(key: ImageQueryFieldKt, value: String) =
        uri.addParameter(key.toString(), value)

    protected fun findQuery(key: ImageQueryFieldKt) =
        uri.queryParams
            .find { it.name.equals(key.toString(), true) }
            ?.value

    open fun build(): InputStream? {
        val url = uri.build().toURL()

        return try {
            httpClient.newCall(webUtils.prepareGet(url.toString()).build())
                .execute()
                .body()
                ?.byteStream()
        } catch (e: SocketTimeoutException) {
            throw ImageBuilderExceptionKt(cause = e)
        } catch (e: ConnectException) {
            throw ImageBuilderExceptionKt(cause = e)
        } catch (e: IOException) {
            logger.error("Unexpected error", e)
            null
        }
    }
}