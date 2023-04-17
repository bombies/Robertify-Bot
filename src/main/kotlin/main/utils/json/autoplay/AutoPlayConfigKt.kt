package main.utils.json.autoplay

import main.utils.json.AbstractGuildConfigKt
import net.dv8tion.jda.api.entities.Guild
import org.bson.Document
import org.json.JSONException
import java.util.*

class AutoPlayConfigKt(private val guild: Guild) : AbstractGuildConfigKt(guild) {

    var status: Boolean
        get() = try {
            getGuildObject().getBoolean(Field.AUTOPLAY.name.lowercase(Locale.getDefault()))
        } catch (e: JSONException) {
            update()
            getGuildObject().getBoolean(Field.AUTOPLAY.name.lowercase(Locale.getDefault()))
        }
        set(value) {
            val guildObject = getGuildObject()
            guildObject.put(Field.AUTOPLAY.name.lowercase(Locale.getDefault()), value)
            cache.updateGuild(guildObject, guild.idLong)
        }

    override fun update() {
        val guildObject = getGuildObject()
        if (!guildObject.has(Field.AUTOPLAY.name.lowercase(Locale.getDefault())))
            guildObject.put(Field.AUTOPLAY.name.lowercase(), false)
        cache.updateCache(guild.idLong.toString(), Document.parse(guildObject.toString()))
    }


    enum class Field {
        AUTOPLAY
    }

}