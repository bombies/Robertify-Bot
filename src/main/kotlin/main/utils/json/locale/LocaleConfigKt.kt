package main.utils.json.locale

import main.utils.json.AbstractGuildConfigKt
import main.utils.locale.RobertifyLocaleKt
import net.dv8tion.jda.api.entities.Guild

class LocaleConfigKt(guild: Guild) : AbstractGuildConfigKt(guild) {

    var locale: RobertifyLocaleKt
        get() {
            val guildObject = getGuildObject()
            if (guildObject.has(Field.LOCALE.toString()))
                return RobertifyLocaleKt.parse(guildObject.getString(Field.LOCALE.toString()))
            return RobertifyLocaleKt.ENGLISH
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