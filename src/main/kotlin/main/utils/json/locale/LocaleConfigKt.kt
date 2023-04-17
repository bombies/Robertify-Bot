package main.utils.json.locale

import main.utils.json.AbstractGuildConfigKt
import main.utils.locale.RobertifyLocaleKt
import net.dv8tion.jda.api.entities.Guild

class LocaleConfigKt(private val guild: Guild) : AbstractGuildConfigKt(guild) {

    fun setLocale(locale: RobertifyLocaleKt) {
        val guildObject = getGuildObject()
        guildObject.put(Field.LOCALE.toString(), locale.name.lowercase())
        cache.updateGuild(guildObject)
    }

    fun getLocale(): RobertifyLocaleKt? {
        val guildObject = getGuildObject()
        if (guildObject.has(Field.LOCALE.toString()))
            return RobertifyLocaleKt.parse(guildObject.getString(Field.LOCALE.toString()))
        return null
    }

    override fun update() {
        TODO("Not yet implemented")
    }


    enum class Field {
        LOCALE;

        override fun toString(): String = name.lowercase()
    }

}