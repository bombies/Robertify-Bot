package main.utils.database.influxdb

import dev.minn.jda.ktx.util.SLF4J
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import main.main.Config
import main.utils.api.robertify.httpClient
import main.utils.api.robertify.post

private val logger by SLF4J

private val HOST_NAME = "${Config.INFLUX_HOST}${if (Config.INFLUX_PORT.isNotEmpty()) ":${Config.INFLUX_PORT}" else ""}"

suspend fun createBucket(bucketName: String) =
    httpClient(HOST_NAME).post<HttpResponse>(
        path = "/api/v2/buckets",
        block = {
            headers {
                set(HttpHeaders.Authorization, "Token ${Config.INFLUX_TOKEN}")
            }
            setBody(CreateBucketDto(name = bucketName, orgID = Config.INFLUX_ORG_ID))
        },
        errorHandler = {
            logger.error("I couldn't create a new InfluxDB bucket with name \"$bucketName\"", this)
        }
    )

@Serializable
private data class CreateBucketDto(
    val name: String,
    val orgID: String,
    val retentionRules: List<RetentionRules> = emptyList()
)

@Serializable
private data class RetentionRules(
    val everySeconds: Int,
    val shardGroupDurationSeconds: Int,
    val type: String = "expire"
)