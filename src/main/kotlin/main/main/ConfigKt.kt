package main.main

import io.github.cdimascio.dotenv.Dotenv
import lombok.SneakyThrows
import main.constants.ENV
import main.constants.JSONConfigFile
import main.utils.GeneralUtils
import main.utils.lavalink.LavaNode
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.util.*

class ConfigKt {

    companion object {
        private val logger = LoggerFactory.getLogger(Config::class.java)
        private var dotenv = Dotenv.load()

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
        fun get(key: ENV, defaultValue: String?): String {
            return dotenv[key.toString().uppercase(Locale.getDefault()), defaultValue]
        }

        /**
         * Get a specific path from the .env file
         * @param dir The directory of the path
         * @param file The specific file to get in the directory
         * @return The path of the key
         */
        fun getPath(dir: ENV, file: JSONConfigFile): Path =
            Path.of(dotenv[dir.toString().uppercase(Locale.getDefault())] + "/" + file.toString())

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

        fun getShardCount(): Int = getInt(ENV.SHARD_COUNT)
        fun getBotToken(): String = get(ENV.BOT_TOKEN)
        fun getOwnerID(): Long = getLong(ENV.OWNER_ID)
        fun isPremiumBot(): Boolean = getBoolean(ENV.PREMIUM_BOT)
        fun loadCommands(): Boolean = getBoolean(ENV.LOAD_COMMANDS)
        fun loadNeededCommands(): Boolean = getBoolean(ENV.LOAD_NEEDED_COMMANDS)
        fun getGatewayUrl(): String = get(ENV.GATEWAY_URL)
        fun hasGatewayUrl(): Boolean = hasValue(ENV.GATEWAY_URL)

        fun hasValue(value: ENV): Boolean {
            val valueString = get(value)
            return valueString.isNotEmpty() && valueString.isNotBlank()
        }

        fun isYoutubeEnabled(): Boolean = hasValue(ENV.YOUTUBE_ENABLED) && getBoolean(ENV.YOUTUBE_ENABLED)
        fun getEnvironment(): String = get(ENV.ENVIRONMENT)

        fun getSentryEnvironment(): String = when (getEnvironment().lowercase(Locale.getDefault())) {
            "dev", "development" -> {
                "development"
            }
            "prod", "production" -> {
                "production"
            }
            "staging" -> {
                "staging"
            }
            else -> throw IllegalArgumentException("\"" + getEnvironment() + "\" isn't a valid environment!")
        }

        fun isProdEnv(): Boolean = getEnvironment().equals("prod", ignoreCase = true)
        fun isStagingEnv(): Boolean = getEnvironment().equals("staging", ignoreCase = true)
        fun isDevEnv(): Boolean = getEnvironment().equals("dev", ignoreCase = true)

        fun getInt(key: ENV): Int = get(key, "-1").toInt()
        fun getLong(key: ENV): Long = get(key, "-1").toLong()
        fun getBoolean(key: ENV): Boolean = get(key, "false").toBoolean()

        @SneakyThrows
        fun getLavaNodes(): List<LavaNode> {
            return try {
                val ret = ArrayList<LavaNode>()
                val jsonObject = getConfigJSON()
                for (obj in jsonObject.getJSONArray("nodes")) {
                    val actualObj = obj as JSONObject
                    ret.add(
                        LavaNode(
                            actualObj.getString("host"),
                            actualObj.getString("port"),
                            actualObj.getString("password")
                        )
                    )
                }
                ret
            } catch (e: NullPointerException) {
                ArrayList()
            }
        }

        @SneakyThrows
        private fun getConfigJSON(): JSONObject {
            val file = File("./json/config.json")
            if (!file.exists()) {
                file.createNewFile()
                logger.warn("config.json didn't exist, so I created one.")
            }
            return JSONObject(GeneralUtils.getFileContent(file.path))
        }
    }
}