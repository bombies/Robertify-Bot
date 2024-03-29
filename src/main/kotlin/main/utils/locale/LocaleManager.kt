package main.utils.locale

import kotlinx.coroutines.runBlocking
import main.utils.GeneralUtils
import main.utils.GeneralUtils.setContent
import main.utils.json.locale.LocaleConfig
import main.utils.locale.messages.RobertifyLocaleMessageKt.Companion.getMessageTypes
import net.dv8tion.jda.api.entities.Guild
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.system.exitProcess

class LocaleManager private constructor(private val guild: Guild?, _locale: RobertifyLocale) {

    companion object {
        private val log = LoggerFactory.getLogger(Companion::class.java)
        private val localeManagers = Collections.synchronizedMap(emptyMap<Long, LocaleManager>().toMutableMap())
        private val locales = Collections.synchronizedMap(emptyMap<RobertifyLocale, Map<String, String>>().toMutableMap())

        fun globalManager(): LocaleManager = LocaleManager(null, RobertifyLocale.ENGLISH)

        fun getLocaleManager(guild: Guild?): LocaleManager {
            if (guild == null)
                return globalManager()
            return localeManagers.computeIfAbsent(guild.idLong) {
                LocaleManager(guild, RobertifyLocale.ENGLISH)
            }
        }

        operator fun get(guild: Guild?): LocaleManager {
            if (guild == null)
                return globalManager()
            return localeManagers.computeIfAbsent(guild.idLong) {
                LocaleManager(guild, RobertifyLocale.ENGLISH)
            }
        }

        private fun retrieveLocaleFile(locale: RobertifyLocale): Map<String, String> {
            if (!GeneralUtils.directoryExists("./locale")) {
                GeneralUtils.createDirectory("./locale")
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

        private fun createLocaleFile(locale: RobertifyLocale) {
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
            RobertifyLocale.values().forEach { createLocaleFile(it) }

        fun reloadLocales() {
            locales.clear()
            RobertifyLocale.availableLanguages
                .forEach { locale -> locales[locale] = retrieveLocaleFile(locale) }
        }

    }

    fun getLocale(): RobertifyLocale {
        if (guild == null)
            return RobertifyLocale.ENGLISH
        return LocaleConfig(guild).getLocale()
    }

    fun setLocale(locale: RobertifyLocale) {
        locales.putIfAbsent(locale, retrieveLocaleFile(locale))

        if (guild != null)
            LocaleConfig(guild).setLocale(locale)
    }

    fun getLocaleFile(): Map<String, String> {
        return locales.computeIfAbsent(getLocale()) {
            retrieveLocaleFile(it)
        }
    }

    fun getMessage(message: LocaleMessage, vararg placeholders: Pair<String, String>): String {
        var msg = getLocaleFile()[message.name.lowercase()]
            ?: throw NullPointerException("There was no such message found in the mapping with key: ${message.name}")

        placeholders.forEach { placeholder ->
            msg = msg.replace(placeholder.first, placeholder.second)
        }

        return msg
    }
}