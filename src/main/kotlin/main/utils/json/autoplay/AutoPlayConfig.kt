package main.utils.json.autoplay

import main.utils.json.AbstractGuildConfig
import net.dv8tion.jda.api.entities.Guild
import org.bson.Document
import org.json.JSONException
import java.util.*

class AutoPlayConfig(private val guild: Guild) : AbstractGuildConfig(guild) {

    fun getStatus(): Boolean {
        return getGuildModel().autoplay ?: false
    }

    fun setStatus(status: Boolean) {
        cache.updateGuild(guild.id) {
            autoplay = status
        }
    }

    override fun update() {
        val guildObject = getGuildModel().toJsonObject()
        if (!guildObject.has(Field.AUTOPLAY.name.lowercase(Locale.getDefault())))
            guildObject.put(Field.AUTOPLAY.name.lowercase(), false)
        cache.updateCache(guild.idLong.toString(), Document.parse(guildObject.toString()))
    }


    enum class Field {
        AUTOPLAY
    }

}