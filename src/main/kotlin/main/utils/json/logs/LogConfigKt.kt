package main.utils.json.logs

import main.main.RobertifyKt
import main.utils.json.AbstractGuildConfigKt
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.bson.Document
import org.json.JSONObject
import java.util.*

class LogConfigKt(private val guild: Guild) : AbstractGuildConfigKt(guild) {

    fun getChannelID(): Long {
        if (!channelIsSet()) throw NullPointerException("There is no channel for this guild! (ID=${guild.id})")
        return getGuildObject().getLong(Field.LOG_CHANNEL.name.lowercase(Locale.getDefault()))
    }

    fun getChannel(): TextChannel? {
        return RobertifyKt.shardManager.getTextChannelById(getChannelID())
    }

    fun channelIsSet(): Boolean {
        val guildObject: JSONObject = getGuildObject()
        if (!guildObject.has(Field.LOG_CHANNEL.name.lowercase(Locale.getDefault()))) return false
        return if (JSONObject.NULL == guildObject[Field.LOG_CHANNEL.name.lowercase(Locale.getDefault())]) false else guildObject.getLong(
            Field.LOG_CHANNEL.name.lowercase(Locale.getDefault())
        ) != -1L
    }

    fun setChannel(cid: Long) {
        val guildObject: JSONObject = getGuildObject()
        guildObject.put(Field.LOG_CHANNEL.name.lowercase(Locale.getDefault()), cid)
        cache.updateGuild(guildObject, guild.idLong)
    }

    fun removeChannel() {
        val guildObject: JSONObject = getGuildObject()
        guildObject.put(Field.LOG_CHANNEL.name.lowercase(Locale.getDefault()), -1L)
        cache.updateGuild(guildObject, guild.idLong)
    }

    override fun update() {
        val guildObject: JSONObject = getGuildObject()
        if (!guildObject.has(Field.LOG_CHANNEL.name.lowercase(Locale.getDefault()))) guildObject.put(
            Field.LOG_CHANNEL.name.lowercase(
                Locale.getDefault()
            ), -1L
        )
        cache.updateCache(guild.idLong.toString(), Document.parse(guildObject.toString()))
    }


    enum class Field {
        LOG_CHANNEL
    }

}