package main.utils.json.guildconfig

import main.constants.RobertifyTheme
import main.utils.database.mongodb.cache.redis.guild.BannedUserModel
import main.utils.database.mongodb.cache.redis.guild.GuildDatabaseModel
import main.utils.database.mongodb.databases.GuildDB
import main.utils.json.AbstractGuildConfig
import main.utils.json.autoplay.AutoPlayConfig
import main.utils.json.reminders.RemindersConfig
import net.dv8tion.jda.api.entities.Guild
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.*

class GuildConfig(private val guild: Guild) : AbstractGuildConfig(guild) {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
    }

    suspend fun getTwentyFourSevenMode(): Boolean {
        val guildModel = getGuildModel()
        val value = guildModel.twenty_four_seven_mode
        return if (value == null) {
            cache.updateGuild(guild.id) {
                twenty_four_seven_mode = false
            }
            false
        } else value
    }

    suspend fun setTwentyFourSevenMode(value: Boolean) {
        cache.updateGuild(guild.id) {
            twenty_four_seven_mode = value
        }
    }

    suspend fun addGuild() {
        require(guildHasInfo()) { "This guild is already added!" }
        getDatabase().addGuild(guild.idLong)
    }

    suspend fun removeGuild() {
        getDatabase().removeGuild(guild.idLong)
        if (!guildHasInfo()) logger.warn(
            "There is no information for guild with ID {} in the cache.",
            guild.idLong
        ) else unloadGuild()
    }

    suspend fun setFields(block: GuildDatabaseModel.() -> Unit) {
        cache.updateGuild(guild.id, block)
    }

    private suspend fun getBannedUsers(): List<BannedUser> {
        return getGuildModel().banned_users
            ?.map { user ->
                BannedUser(
                    user.banned_id,
                    user.banned_by,
                    user.banned_at,
                    user.banned_until
                )
            } ?: emptyList()
    }

    suspend fun getBannedUsersWithUnbanTimes(): HashMap<Long, Long> {
        val bannedUsers = getBannedUsers()
        val ret = HashMap<Long, Long>()
        for (bannedUser in bannedUsers) ret[bannedUser.user] = bannedUser.bannedUntil
        return ret
    }

    suspend fun banUser(uid: Long, modId: Long, bannedAt: Long, bannedUntil: Long) {
        require(!isBannedUser(uid)) { "This user is already banned!" }

        setFields {
            banned_users?.add(
                BannedUserModel(
                    uid,
                    modId,
                    bannedAt,
                    bannedUntil
                )
            )
        }
    }

    suspend fun unbanUser(uid: Long) {
        require(isBannedUser(uid)) { "This user isn't banned!" }

        setFields {
            banned_users?.removeIf { user -> user.banned_id == uid }
        }
    }

    suspend fun getTimeUntilUnban(uid: Long): Long {
        require(isBannedUser(uid)) { "This user isn't banned!" }
        val (_, _, bannedAt, bannedUntil) = getBannedUsers()
            .filter { user: BannedUser -> user.user == uid }[0]
        return bannedUntil - bannedAt
    }

    suspend fun isBannedUser(uid: Long): Boolean {
        return getBannedUsers()
            .any { user: BannedUser -> user.user == uid }
    }

    fun isPremium(): Boolean {
        return true
//        return Robertify.getRobertifyAPI().guildIsPremium(guild.idLong);
    }

    override suspend fun update() {
        // Nothing
    }


    data class BannedUser(val user: Long, val bannedBy: Long, val bannedAt: Long, val bannedUntil: Long) {
        override fun toString(): String {
            return user.toString()
        }
    }


    class ConfigBuilder {
        private val reminders: JSONObject? = null
        private val dedicatedChannel: JSONObject? = null
        private val restrictedChannels: JSONObject? = null
        private val permissions: JSONObject? = null
        private val toggles: JSONObject? = null
        private val eightBall: JSONArray? = null
        private var theme: RobertifyTheme? = null
        private val bannedUsers: JSONArray? = null
        private var twentyFourSeven: Boolean? = null
        private var autoPlay: Boolean? = null
        fun set247(status: Boolean): ConfigBuilder {
            twentyFourSeven = status
            return this
        }

        fun setAutoPlay(status: Boolean): ConfigBuilder {
            autoPlay = status
            return this
        }

        fun setTheme(theme: RobertifyTheme): ConfigBuilder {
            this.theme = theme
            return this
        }

        fun build(): JSONObject {
            val obj = JSONObject()
            if (reminders != null) obj.put(
                RemindersConfig.Fields.REMINDERS.name.lowercase(Locale.getDefault()),
                reminders
            )
            if (dedicatedChannel != null) obj.put(GuildDB.Field.REQUEST_CHANNEL_OBJECT.toString(), dedicatedChannel)
            if (restrictedChannels != null) obj.put(
                GuildDB.Field.RESTRICTED_CHANNELS_OBJECT.toString(),
                restrictedChannels
            )
            if (permissions != null) obj.put(GuildDB.Field.PERMISSIONS_OBJECT.toString(), permissions)
            if (toggles != null) obj.put(GuildDB.Field.TOGGLES_OBJECT.toString(), toggles)
            if (eightBall != null) obj.put(GuildDB.Field.EIGHT_BALL_ARRAY.toString(), eightBall)
            if (theme != null) obj.put(GuildDB.Field.THEME.toString(), theme!!.name.lowercase(Locale.getDefault()))
            if (bannedUsers != null) obj.put(GuildDB.Field.BANNED_USERS_ARRAY.toString(), bannedUsers)
            if (twentyFourSeven != null) obj.put(GuildDB.Field.TWENTY_FOUR_SEVEN.toString(), twentyFourSeven)
            if (autoPlay != null) obj.put(AutoPlayConfig.Field.AUTOPLAY.name.lowercase(Locale.getDefault()), autoPlay)
            return obj
        }
    }

}