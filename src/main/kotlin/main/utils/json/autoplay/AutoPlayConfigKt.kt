package main.utils.json.autoplay

import main.utils.json.AbstractGuildConfig
import main.utils.json.AbstractGuildConfigKt
import net.dv8tion.jda.api.entities.Guild
import org.bson.Document
import org.json.JSONException
import java.util.*

class AutoPlayConfigKt(private val guild: Guild) : AbstractGuildConfigKt(guild) {

    fun getStatus(): Boolean {
        return try {
            getGuildObject().getBoolean(Field.AUTOPLAY.name.lowercase(Locale.getDefault()))
        } catch (e: JSONException) {
            update()
            getGuildObject().getBoolean(Field.AUTOPLAY.name.lowercase(Locale.getDefault()))
        }
    }

    fun setStatus(status: Boolean) {
        val guildObject = getGuildObject()
        guildObject.put(Field.AUTOPLAY.name.lowercase(Locale.getDefault()), status)
        AbstractGuildConfig.getCache().updateGuild(guildObject, guild.idLong)
    }

    override fun update() {
        val guildObject = getGuildObject()
        if (!guildObject.has(Field.AUTOPLAY.name.lowercase(Locale.getDefault())))
            guildObject.put(Field.AUTOPLAY.name.lowercase(), false)
        AbstractGuildConfig.getCache().updateCache(guild.idLong.toString(), Document.parse(guildObject.toString()))
    }


    enum class Field {
        AUTOPLAY
    }

}