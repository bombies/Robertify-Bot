package main.utils.json.locale

import main.utils.json.AbstractGuildConfig
import main.utils.locale.RobertifyLocale
import net.dv8tion.jda.api.entities.Guild

class LocaleConfig(private val guild: Guild) : AbstractGuildConfig(guild) {

    suspend fun getLocale(): RobertifyLocale {
        return RobertifyLocale.parse(getGuildModel().locale)
    }

    suspend fun setLocale(locale: RobertifyLocale) {
        cache.updateGuild(guild.id) {
            this.locale = locale.name.lowercase()
        }
    }

    override suspend fun update() {
        TODO("Not yet implemented")
    }


    enum class Field {
        LOCALE;

        override fun toString(): String = name.lowercase()
    }

}