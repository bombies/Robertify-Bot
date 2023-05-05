package main.utils.locale

import main.utils.GeneralUtilsKt
import main.utils.GeneralUtilsKt.setContent
import main.utils.json.locale.LocaleConfigKt
import main.utils.locale.messages.RobertifyLocaleMessageKt.Companion.getMessageTypes
import net.dv8tion.jda.api.entities.Guild
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.system.exitProcess

class LocaleManagerKt private constructor(private val guild: Guild?, _locale: RobertifyLocaleKt) {

    companion object {
        private val log = LoggerFactory.getLogger(Companion::class.java)
        private val localeManagers = Collections.synchronizedMap(emptyMap<Long, LocaleManagerKt>().toMutableMap())
        private val locales = Collections.synchronizedMap(emptyMap<RobertifyLocaleKt, Map<String, String>>().toMutableMap())

        fun globalManager(): LocaleManagerKt = LocaleManagerKt(null, RobertifyLocaleKt.ENGLISH)

        fun getLocaleManager(guild: Guild?): LocaleManagerKt {
            if (guild == null)
                return globalManager()
            return localeManagers.computeIfAbsent(guild.idLong) {
                LocaleManagerKt(guild, RobertifyLocaleKt.ENGLISH)
            }
        }

        operator fun get(guild: Guild?): LocaleManagerKt {
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

            for (fieldSection in getMessageTypes().values)
                for (field in fieldSection)
                    content.append(field.name.lowercase())
                        .append(": ")
                        .append("\"Fill me out\"")
            file.setContent(content.toString())
        }

        private fun createAllLocaleFiles() =
            RobertifyLocaleKt.values().forEach { createLocaleFile(it) }

        fun reloadLocales() {
            locales.clear()
            RobertifyLocaleKt.availableLanguages
                .forEach { locale -> locales[locale] = retrieveLocaleFile(locale) }
        }

    }

    var locale: RobertifyLocaleKt = _locale
        set(value) {
            if (value == field)
                return

            locales.putIfAbsent(value, retrieveLocaleFile(value))
            field = value

            if (guild != null)
                LocaleConfigKt(guild).locale = value
        }

    private val localeFile: Map<String, String>
        get() = locales.computeIfAbsent(locale) {
            retrieveLocaleFile(it)
        }

    operator fun get(message: LocaleMessageKt): String =
        localeFile[message.name.lowercase()]
            ?: throw NullPointerException("There was no such message found in the mapping with key: ${message.name}")

    fun getMessage(message: LocaleMessageKt): String = get(message)

    operator fun get(message: LocaleMessageKt, vararg placeholders: Pair<String, String>): String {
        var msg = localeFile[message.name.lowercase()]
            ?: throw NullPointerException("There was no such message found in the mapping with key: ${message.name}")

        placeholders.forEach { placeholder ->
            msg = msg.replace(placeholder.first, placeholder.second)
        }

        return msg
    }

    fun getMessage(message: LocaleMessageKt, vararg placeholders: Pair<String, String>): String =
        get(message, *placeholders)
}