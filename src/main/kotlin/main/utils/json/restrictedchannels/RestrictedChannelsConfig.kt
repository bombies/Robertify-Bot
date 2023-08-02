package main.utils.json.restrictedchannels

import main.utils.database.mongodb.databases.GuildDB
import main.utils.json.AbstractGuildConfig
import main.utils.json.getIndexOfObjectInArray
import net.dv8tion.jda.api.entities.Guild

class RestrictedChannelsConfig(private val guild: Guild) : AbstractGuildConfig(guild) {

    suspend fun addChannel(channelID: Long, type: ChannelType?) {
        val configField: GuildDB.Field = when (type) {
            ChannelType.TEXT_CHANNEL -> GuildDB.Field.RESTRICTED_CHANNELS_TEXT
            ChannelType.VOICE_CHANNEL -> GuildDB.Field.RESTRICTED_CHANNELS_VOICE
            else -> throw IllegalArgumentException("Invalid type!")
        }

        check(!isRestrictedChannel(channelID, type)) { "This is already a restricted voice channel!" }

        cache.updateGuild(guild.id) {
            restricted_channels {
                when (type) {
                    ChannelType.TEXT_CHANNEL -> text_channels.add(channelID)
                    ChannelType.VOICE_CHANNEL -> voice_channels.add(channelID)
                    else -> throw IllegalArgumentException("Invalid type!")
                }
            }
        }
    }

    suspend fun removeChannel(channelID: Long, type: ChannelType?) {
        val obj = getGuildModel().toJsonObject()
        val configField: GuildDB.Field = when (type) {
            ChannelType.TEXT_CHANNEL -> GuildDB.Field.RESTRICTED_CHANNELS_TEXT
            ChannelType.VOICE_CHANNEL -> GuildDB.Field.RESTRICTED_CHANNELS_VOICE
            else -> throw IllegalArgumentException("Invalid type!")
        }

        check(isRestrictedChannel(channelID, type)) { "This isn't a restricted channel!" }

        try {
            val arr = obj.getJSONObject(GuildDB.Field.RESTRICTED_CHANNELS_OBJECT.toString())
                .getJSONArray(configField.toString())
            arr.remove(getIndexOfObjectInArray(arr, channelID))
            cache.updateCache(obj, GuildDB.Field.GUILD_ID, guild.idLong)
        } catch (e: NullPointerException) {
            throw NullPointerException("This channel ID already isn't a restricted channel")
        }
    }

    suspend fun getRestrictedChannels(type: ChannelType?): List<Long> {
        val obj = getGuildModel().restricted_channels
        return when (type) {
            ChannelType.TEXT_CHANNEL -> obj?.text_channels ?: emptyList()
            ChannelType.VOICE_CHANNEL -> obj?.voice_channels ?: emptyList()
            else -> throw IllegalArgumentException("Invalid type!")
        }
    }

    suspend fun isRestrictedChannel(vcID: Long, type: ChannelType?): Boolean =
        getRestrictedChannels(type).contains(vcID)

    suspend fun restrictedChannelsToString(type: ChannelType?): String =
        getRestrictedChannels(type).joinToString(", ") { channel ->
            "<#$channel>"
        }


    enum class ChannelType(private val str: String) {
        VOICE_CHANNEL("voice"),
        TEXT_CHANNEL("text"),
        ALL("all");

        override fun toString(): String {
            return str
        }
    }


    override suspend fun update() {}
}