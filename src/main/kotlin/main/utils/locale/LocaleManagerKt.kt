package main.utils.locale

import main.utils.GeneralUtilsKt
import net.dv8tion.jda.api.entities.Guild
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.system.exitProcess

class LocaleManagerKt private constructor(val guild: Guild?, var locale: RobertifyLocaleKt) {

    companion object {
        private val log = LoggerFactory.getLogger(Companion::class.java)
        private val localeManagers = emptyMap<Long, LocaleManagerKt>().toMutableMap()
        private val locales = emptyMap<RobertifyLocaleKt, Map<String, String>>().toMutableMap()

        fun globalManager(): LocaleManagerKt = LocaleManagerKt(null, RobertifyLocaleKt.ENGLISH)

        fun getLocaleManager(guild: Guild?): LocaleManagerKt {
            if (guild == null)
                return globalManager()
            return localeManagers.computeIfAbsent(guild.idLong) {
                LocaleManagerKt(guild, RobertifyLocaleKt.ENGLISH)
            }
        }

        private fun retrieveLocaleFile(locale: RobertifyLocaleKt): Map<String, String> {
            if (!GeneralUtilsKt.directoryExists("./locale")) {
                GeneralUtilsKt.createDirectory("./locale")
                log.error("There was no file found for locale: ${locale.code.uppercase()}")
                createAllLocaleFiles()
                exitProcess(-1)
            }

            val localeFile = File("./locale/messages.${locale.code.lowercase()}.yml")
            if (!localeFile.exists()) {
                log.warn("${locale.code} locale didn't exist. Creating it.")
                createLocaleFile(locale)
            }

            return Yaml().load(FileInputStream(localeFile))
        }

        private fun createLocaleFile(locale: RobertifyLocaleKt) {
            val file = File("./locale/messages.${locale.code.lowercase()}.yml")
            val content = StringBuilder()

            // TODO: Convert RobertifyLocaleMessage to Kotlin
            for (fieldSection in RobertifyLocaleMessage.getMessageTypes().values)
                for (field in fieldSection)
                    content.append(field.name().lowercase())
                        .append(": ")
                        .append("\"Fill me out\"")
            GeneralUtilsKt.setFileContent(file, content.toString())
        }

        private fun createAllLocaleFiles() =
            RobertifyLocaleKt.values().forEach { createLocaleFile(it) }

        fun reloadLocales() {
            locales.clear()
            RobertifyLocaleKt.getAvailableLanguages()
                .forEach { locale ->
                    locales.put(locale, retrieveLocaleFile(locale))
                }

            // TODO: Reloading locale for all guilds through the ShardManager
        }

    }

    private var localeFile: Map<String, String> = locales.computeIfAbsent(locale) {
        retrieveLocaleFile(it)
    }

    fun setLocale(locale: RobertifyLocaleKt) {
        locales.putIfAbsent(locale, retrieveLocaleFile(locale))

        this.locale = locale
        localeFile = retrieveLocaleFile()

        // TODO: LocaleConfig handling
    }

    fun getMessage(message: LocaleMessageKt): String {
        return localeFile[message.name().lowercase()] ?:
        throw NullPointerException("There was no such message found in the mapping with key: ${message.name()}")
    }

    fun getMessage(message: LocaleMessageKt, vararg placeholders: Pair<String, String>): String {
        var msg = localeFile[message.name().lowercase()]
            ?: throw NullPointerException("There was no such message found in the mapping with key: ${message.name()}")

        placeholders.forEach { placeholder ->
            msg = msg.replace(Pattern.quote(placeholder.first), Matcher.quoteReplacement(placeholder.second))
        }
        return msg
    }

    private fun retrieveLocaleFile(): Map<String, String> = Companion.retrieveLocaleFile(locale)
}