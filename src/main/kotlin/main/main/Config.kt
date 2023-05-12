package main.main

import io.github.cdimascio.dotenv.Dotenv
import lombok.SneakyThrows
import main.constants.ENV
import main.utils.GeneralUtilsKt
import main.utils.lavalink.LavaNodeKt
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.awt.Color
import java.io.File
import java.io.IOException
import java.util.*

class Config {

    companion object {
        private val logger = LoggerFactory.getLogger(Config::class.java)
        private var dotenv: Dotenv = Dotenv.load()

        /**
         * Get a string value from its specific key from the .env file
         * @param key .env key to retrieve.
         * @return The string attached to the key
         */
        operator fun get(key: ENV): String {
            return dotenv[key.toString().uppercase(Locale.getDefault()), ""]
        }

        /**
         * Get a string value from its specific key from the .env file
         * @param key .env key to retrieve.
         * @param defaultValue The value to return if the key specified doesn't have a value.
         * @return The string attached to the key
         */
        operator fun get(key: ENV, defaultValue: String?): String {
            return dotenv[key.toString().uppercase(Locale.getDefault()), defaultValue]
        }

        /**
         * Reload the .env file to use the new values if it was updated after compilation and execution
         */
        fun reload() {
            dotenv = Dotenv.load()
        }

        val providers: Array<String>
            get() {
                return try {
                    val config = getConfigJSON()
                    val providers = config.getJSONArray("providers")
                    providers.map { obj: Any -> obj.toString() }.toTypedArray()
                } catch (e: IOException) {
                    logger.warn("IOException thrown. Is config.yml missing?")
                    arrayOf()
                }
            }

        val SHARD_COUNT: Int
            get() = getInt(ENV.SHARD_COUNT)
        val BOT_TOKEN: String
            get() = get(ENV.BOT_TOKEN)

        val LAVA_NODES: List<LavaNodeKt>
            get() = try {
                val ret = mutableListOf<LavaNodeKt>()
                val jsonObject = getConfigJSON()
                jsonObject.getJSONArray("nodes")
                    .forEach { obj ->
                        if (obj is JSONObject) {
                            ret.add(
                                LavaNodeKt(
                                    name = obj.getString("name"),
                                    host = obj.getString("host"),
                                    port = obj.getString("port"),
                                    password = obj.getString("password")
                                )
                            )
                        }
                    }
                ret
            } catch (e: NullPointerException) {
                emptyList()
            }

        val YOUTUBE_ENABLED: Boolean
            get() =
                hasValue(ENV.YOUTUBE_ENABLED) && getBoolean(ENV.YOUTUBE_ENABLED)

        val OWNER_ID: Long
            get() = getLong(ENV.OWNER_ID)

        val PREMIUM_BOT: Boolean
            get() = getBoolean(ENV.PREMIUM_BOT)

        val AUDIO_DIR: String
            get() = get(ENV.AUDIO_DIR)

        val LOAD_COMMANDS: Boolean
            get() = getBoolean(ENV.LOAD_COMMANDS)

        val LOAD_NEEDED_COMMANDS: Boolean
            get() = getBoolean(ENV.LOAD_NEEDED_COMMANDS)

        val GATEWAY_URL: String
            get() = get(ENV.GATEWAY_URL)

        val HAS_GATEWAY_URL: Boolean
            get() = hasValue(ENV.GATEWAY_URL)

        val ENVIRONMENT: String
            get() = get(ENV.ENVIRONMENT, "dev")

        val BOT_NAME: String
            get() = get(ENV.BOT_NAME)

        val BOT_COLOR: Color
            get() = Color.decode(get(ENV.BOT_COLOR))

        val SUPPORT_SERVER: String
            get() = get(ENV.BOT_SUPPORT_SERVER)

        val ICON_URL: String
            get() = get(ENV.ICON_URL)

        val RANDOM_MESSAGE_CHANCE: Double
            get() = get(ENV.RANDOM_MESSAGE_CHANCE).toDouble()

        val MESSAGE_CONTENT_ENABLED: Boolean
            get() = getBoolean(ENV.MESSAGE_CONTENT_INTENT_ENABLED)

        val SPOTIFY_CLIENT_ID: String
            get() = get(ENV.SPOTIFY_CLIENT_ID)

        val SPOTIFY_CLIENT_SECRET: String
            get() = get(ENV.SPOTIFY_CLIENT_SECRET)

        val DEEZER_ACCESS_TOKEN: String
            get() = get(ENV.DEEZER_ACCESS_TOKEN)

        val GENIUS_API_KEY: String
            get() = get(ENV.GENIUS_API_KEY)

        val MONGO_USERNAME: String
            get() = get(ENV.MONGO_USERNAME)

        val MONGO_PASSWORD: String
            get() = get(ENV.MONGO_PASSWORD)

        val MONGO_HOSTNAME: String
            get() = get(ENV.MONGO_HOSTNAME)

        val MONGO_CLUSTER_NAME: String
            get() = get(ENV.MONGO_CLUSTER_NAME)
        
        val MONGO_DATABASE_NAME: String
            get() = get(ENV.MONGO_DATABASE_NAME)

        val INFLUX_HOST: String
            get() = get(ENV.INFLUX_HOST)

        val INFLUX_PORT: String
            get() = get(ENV.INFLUX_PORT)

        val INFLUX_TOKEN: String
            get() = get(ENV.INFLUX_TOKEN)

        val INFLUX_ORG_ID: String
            get() = get(ENV.INFLUX_ORG_ID)
        
        val REDIS_HOSTNAME: String
            get() = get(ENV.REDIS_HOSTNAME)
        
        val REDIS_PORT: String
            get() = get(ENV.REDIS_PORT)
        
        val REDIS_PASSWORD: String
            get() = get(ENV.REDIS_PASSWORD)
        
        val TOP_GG_TOKEN: String
            get() = get(ENV.TOP_GG_TOKEN)
        
        val VOTE_REMINDER_CHANCE: Double
            get() = get(ENV.VOTE_REMINDER_CHANCE).toDouble()
        
        val ROBERTIFY_WEB_HOSTNAME: String
            get() = get(ENV.ROBERTIFY_WEB_HOSTNAME)
        
        val ROBERTIFY_API_HOSTNAME: String
            get() = get(ENV.ROBERTIFY_API_HOSTNAME)
        
        val ROBERTIFY_API_PASSWORD: String
            get() = get(ENV.ROBERTIFY_API_PASSWORD)

        val KTOR_API_KEY: String
            get() = get(ENV.KTOR_API_SECRET_KEY)

        val KTOR_API_PORT: Int
            get() = get(ENV.KTOR_API_PORT, "8080").toInt()

        val SENTRY_DSN: String
            get() = get(ENV.SENTRY_DSN, "")

        fun hasValue(value: ENV): Boolean {
            val valueString = get(value)
            return valueString.isNotEmpty() && valueString.isNotBlank()
        }

        fun getSentryEnvironment(): String = when (ENVIRONMENT.lowercase(Locale.getDefault())) {
            "dev", "development" -> {
                "development"
            }

            "prod", "production" -> {
                "production"
            }

            "staging" -> {
                "staging"
            }

            else -> throw IllegalArgumentException("\"$ENVIRONMENT\" isn't a valid environment!")
        }

        fun isProdEnv(): Boolean = ENVIRONMENT.equals("prod", ignoreCase = true)
        fun isStagingEnv(): Boolean = ENVIRONMENT.equals("staging", ignoreCase = true)
        fun isDevEnv(): Boolean = ENVIRONMENT.equals("dev", ignoreCase = true)

        fun getInt(key: ENV): Int = get(key, "-1").toInt()
        fun getLong(key: ENV): Long = get(key, "-1").toLong()
        fun getBoolean(key: ENV): Boolean = get(key, "false").toBoolean()

        @SneakyThrows
        private fun getConfigJSON(): JSONObject {
            val file = File("./json/config.json")
            if (!file.exists()) {
                file.createNewFile()
                logger.warn("config.json didn't exist, so I created one.")
            }
            return JSONObject(GeneralUtilsKt.getFileContent(file.path))
        }
    }
}