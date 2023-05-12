package main.utils.json.locale

import main.utils.json.AbstractGuildConfig
import main.utils.locale.RobertifyLocale
import net.dv8tion.jda.api.entities.Guild

class LocaleConfig(guild: Guild) : AbstractGuildConfig(guild) {

    var locale: RobertifyLocale
        get() {
            val guildObject = getGuildObject()
            if (guildObject.has(Field.LOCALE.toString()))
                return RobertifyLocale.parse(guildObject.getString(Field.LOCALE.toString()))
            return RobertifyLocale.ENGLISH
        }
        set(value) {
            val guildObject = getGuildObject()
            guildObject.put(Field.LOCALE.toString(), value.name.lowercase())
            cache.updateGuild(guildObject)
        }

    override fun update() {
        TODO("Not yet implemented")
    }


    enum class Field {
        LOCALE;

        override fun toString(): String = name.lowercase()
    }

}