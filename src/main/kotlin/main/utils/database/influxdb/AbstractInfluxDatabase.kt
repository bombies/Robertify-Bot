package main.utils.database.influxdb

import com.influxdb.client.InfluxDBClientOptions
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import com.influxdb.client.write.Point
import com.influxdb.exceptions.NotFoundException
import com.influxdb.query.FluxRecord
import com.influxdb.query.dsl.Flux
import dev.minn.jda.ktx.util.SLF4J
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import main.main.Config

abstract class AbstractInfluxDatabase(private val database: InfluxDatabase) {

    companion object {
        private val logger by SLF4J
    }

    val client by lazy {
        InfluxDBClientKotlinFactory.create(
            InfluxDBClientOptions.builder()
                .url("${Config.INFLUX_HOST}${if (Config.INFLUX_PORT.isNotEmpty()) ":${Config.INFLUX_PORT}" else ""}")
                .authenticateToken(Config.INFLUX_TOKEN.toCharArray())
                .org("robertify")
                .bucket(database.toString())
                .build()
        )
    }

    protected suspend fun writePoint(builder: PointBuilder.() -> PointBuilder) {
        val writeApi = client.getWriteKotlinApi()
        try {
            writeApi.writePoint(builder(PointBuilder()).build())
        } catch (e: NotFoundException) {
            createBucket(database.toString())!!.apply {
                if (status == HttpStatusCode.Created)
                    writeApi.writePoint(builder(PointBuilder()).build())
                else logger.warn("Couldn't create a new bucket with name: \"$database\"\nResponse status: $status\nResponse body: ${bodyAsText()}")
            }
        }
    }

    protected suspend fun <M> writeMeasurement(measurement: M) {
        val writeApi = client.getWriteKotlinApi()
        try {
            writeApi.writeMeasurement(measurement, WritePrecision.NS)
        } catch (e: NotFoundException) {
            createBucket(database.toString()).apply {
                if (this == null)
                    return@apply
                if (status == HttpStatusCode.Created)
                    writeApi.writeMeasurement(measurement, WritePrecision.NS)
                else logger.warn("Couldn't create a new bucket with name: \"$database\"\nResponse status: $status\nResponse body: ${bodyAsText()}")
            }
        }
    }

    protected suspend fun query(vararg query: String): Channel<FluxRecord>? =
        try {
            client.getQueryKotlinApi().query(
                """
            from(bucket: $database)
                ${query.joinToString("\t") { "|> $it" }}
        """.trimIndent()
            )
        } catch (e: NotFoundException) {
            createBucket(database.toString()).apply {
                if (this == null)
                    return@apply
                if (status != HttpStatusCode.Created)
                    logger.warn("Couldn't create a new bucket with name: \"$database\"\nResponse status: $status\nResponse body: ${bodyAsText()}")
            }
            null
        }


    protected suspend fun query(queries: List<String>): Channel<FluxRecord>? =
        query(*queries.toTypedArray())

    protected suspend fun query(query: FluxBuilder.() -> Flux): Channel<FluxRecord>? {
        val flux = query(FluxBuilder(database))
        return try {
            client.getQueryKotlinApi().query(flux.toString())
        } catch (e: NotFoundException) {
            createBucket(database.toString()).apply {
                if (this == null)
                    return@apply
                if (status != HttpStatusCode.Created)
                    logger.warn("Couldn't create a new bucket with name: \"$database\"\nResponse status: $status\nResponse body: ${bodyAsText()}")
            }
            null
        }
    }

    protected class FluxBuilder(database: InfluxDatabase) {
        val flux = Flux.from(database.toString())
    }

    protected open class PointBuilder {
        private lateinit var measurement: String
        private lateinit var tag: Tag
        private lateinit var fields: MutableMap<String, String>

        fun setMeasurement(measurement: String): PointBuilder {
            this.measurement = measurement
            return this
        }

        fun setTag(key: String, value: String): PointBuilder {
            this.tag = Tag(key, value)
            return this
        }

        fun addField(key: String, value: String): PointBuilder {
            fields[key] = value
            return this
        }

        data class Tag(val key: String, val value: String)

        fun build(): Point =
            Point.measurement(measurement)

                .addTag(tag.key, tag.value)
                .addFields(fields.toMap())

    }

}