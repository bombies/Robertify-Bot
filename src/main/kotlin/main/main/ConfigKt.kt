package main.main

import io.github.cdimascio.dotenv.Dotenv
import lombok.SneakyThrows
import main.constants.ENVKt
import main.utils.GeneralUtilsKt
import main.utils.lavalink.LavaNodeKt
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.util.*

class ConfigKt {

    companion object {
        private val logger = LoggerFactory.getLogger(ConfigKt::class.java)
        private var dotenv: Dotenv = Dotenv.load()

        /**
         * Get a string value from its specific key from the .env file
         * @param key .env key to retrieve.
         * @return The string attached to the key
         */
        operator fun get(key: ENVKt): String {
            return dotenv[key.toString().uppercase(Locale.getDefault()), ""]
        }

        /**
         * Get a string value from its specific key from the .env file
         * @param key .env key to retrieve.
         * @param defaultValue The value to return if the key specified doesn't have a value.
         * @return The string attached to the key
         */
        fun get(key: ENVKt, defaultValue: String?): String {
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
                val config = getConfigJSON()
                val providers = config.getJSONArray("providers")
                return providers.map { obj: Any -> obj.toString() }
                    .toTypedArray()
            }

        val shardCount: Int
            get() = getInt(ENVKt.SHARD_COUNT)
        val botToken: String
            get() = get(ENVKt.BOT_TOKEN)

        val lavaNodes: List<LavaNodeKt>
            get() = try {
                val ret = mutableListOf<LavaNodeKt>()
                val jsonObject = getConfigJSON()
                jsonObject.getJSONArray("nodes")
                    .forEach { obj ->
                        if (obj is JSONObject) {
                            ret.add(
                                LavaNodeKt(
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

        val youtubeEnabled: Boolean
            get() =
                hasValue(ENVKt.YOUTUBE_ENABLED) && getBoolean(ENVKt.YOUTUBE_ENABLED)

        val ownerId: Long
            get() = getLong(ENVKt.OWNER_ID)

        val premiumBot: Boolean
            get() = getBoolean(ENVKt.PREMIUM_BOT)

        val loadCommands: Boolean
            get() = getBoolean(ENVKt.LOAD_COMMANDS)

        val loadNeededCommands: Boolean
            get() = getBoolean(ENVKt.LOAD_NEEDED_COMMANDS)

        val gatewayUrl: String
            get() = get(ENVKt.GATEWAY_URL)

        val hasGatewayUrl: Boolean
            get() = hasValue(ENVKt.GATEWAY_URL)

        val environment: String
            get() = get(ENVKt.ENVIRONMENT)

        fun hasValue(value: ENVKt): Boolean {
            val valueString = get(value)
            return valueString.isNotEmpty() && valueString.isNotBlank()
        }

        fun getSentryEnvironment(): String = when (environment.lowercase(Locale.getDefault())) {
            "dev", "development" -> {
                "development"
            }

            "prod", "production" -> {
                "production"
            }

            "staging" -> {
                "staging"
            }

            else -> throw IllegalArgumentException("\"$environment\" isn't a valid environment!")
        }

        fun isProdEnv(): Boolean = environment.equals("prod", ignoreCase = true)
        fun isStagingEnv(): Boolean = environment.equals("staging", ignoreCase = true)
        fun isDevEnv(): Boolean = environment.equals("dev", ignoreCase = true)

        fun getInt(key: ENVKt): Int = get(key, "-1").toInt()
        fun getLong(key: ENVKt): Long = get(key, "-1").toLong()
        fun getBoolean(key: ENVKt): Boolean = get(key, "false").toBoolean()

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