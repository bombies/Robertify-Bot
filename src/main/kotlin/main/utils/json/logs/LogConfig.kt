package main.utils.json.logs

import main.main.Robertify
import main.utils.json.AbstractGuildConfig
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.bson.Document
import org.json.JSONObject
import java.util.*

class LogConfig(private val guild: Guild) : AbstractGuildConfig(guild) {

    suspend fun getChannelId(): Long {
        if (!channelIsSet())
            throw NullPointerException("There is no channel for this guild! (ID=${guild.id})")
        return getGuildModel().log_channel!!
    }

    suspend fun setChannelId(cid: Long) {
        cache.updateGuild(guild.id) {
            log_channel = cid
        }
    }

    suspend fun getChannel(): TextChannel? {
        return Robertify.shardManager.getTextChannelById(getChannelId())
    }

    suspend fun channelIsSet(): Boolean {
        return getGuildModel().log_channel?.let {
            it != -1L
        } ?: false
    }

    suspend fun removeChannel() {
        cache.updateGuild(guild.id) {
            log_channel = -1L
        }
    }

    override suspend fun update() {
        val guildObject: JSONObject = getGuildModel().toJsonObject()
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