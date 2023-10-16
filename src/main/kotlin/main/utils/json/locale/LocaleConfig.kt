package main.utils.json.locale

import main.utils.json.AbstractGuildConfig
import main.utils.locale.RobertifyLocale
import net.dv8tion.jda.api.entities.Guild

class LocaleConfig(private val guild: Guild) : AbstractGuildConfig(guild) {

    fun getLocale(): RobertifyLocale {
        return RobertifyLocale.parse(getGuildModel().locale)
    }

    fun setLocale(locale: RobertifyLocale) {
        cache.updateGuild(guild.id) {
            this.locale = locale.name.lowercase()
        }
    }

    override fun update() {
        TODO("Not yet implemented")
    }


    enum class Field {
        LOCALE;

        override fun toString(): String = name.lowercase()
    }

}