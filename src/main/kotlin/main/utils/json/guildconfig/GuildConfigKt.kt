package main.utils.json.guildconfig

import main.constants.RobertifyTheme
import main.utils.database.mongodb.databases.GuildDB
import main.utils.json.AbstractGuildConfig
import main.utils.json.autoplay.AutoPlayConfig
import main.utils.json.reminders.RemindersConfig
import net.dv8tion.jda.api.entities.Guild
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.*

class GuildConfigKt(private val guild: Guild) : AbstractGuildConfig(guild) {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
    }

    var twentyFourSevenMode: Boolean
        get() {
            if (!guildHasInfo()) loadGuild()
            if (!getCache().hasField(guild.idLong, GuildDB.Field.TWENTY_FOUR_SEVEN)) {
                getCache().setField(guild.idLong, GuildDB.Field.TWENTY_FOUR_SEVEN, false)
                return false
            }
            return getCache().getField(guild.idLong, GuildDB.Field.TWENTY_FOUR_SEVEN) as Boolean
        }
        set(value) {
            if (!guildHasInfo()) loadGuild()
            getCache().setField(guild.idLong, GuildDB.Field.TWENTY_FOUR_SEVEN, value)
        }

    fun addGuild() {
        require(!guildHasInfo()) { "This guild is already added!" }
        database.addGuild(guild.idLong)
    }

    fun removeGuild() {
        database.removeGuild(guild.idLong)
        if (!guildHasInfo()) logger.warn(
            "There is no information for guild with ID {} in the cache.",
            guild.idLong
        ) else unloadGuild()
    }

    fun getPrefix(): String {
        if (!guildHasInfo()) loadGuild()
        return getCache().getField(guild.idLong, GuildDB.Field.GUILD_PREFIX) as String
    }

    fun setPrefix(prefix: String) {
        if (!guildHasInfo()) loadGuild()
        require(prefix.length <= 4) { "The prefix must be 4 or less characters!" }
        getCache().setField(guild.idLong, GuildDB.Field.GUILD_PREFIX, prefix)
    }

    fun setManyFields(builder: ConfigBuilder) {
        getCache().setFields(guild.idLong, builder.build())
    }

    fun getBannedUsers(): List<BannedUser> {
        if (!guildHasInfo()) loadGuild()
        val bannedUsers = getCache().getField(guild.idLong, GuildDB.Field.BANNED_USERS_ARRAY) as JSONArray
        val ret: MutableList<BannedUser> = ArrayList()
        for (i in 0 until bannedUsers.length()) {
            val jsonObject = bannedUsers.getJSONObject(i)
            val bannedUser = BannedUser(
                jsonObject.getLong(GuildDB.Field.BANNED_USER.toString()),
                jsonObject.getLong(GuildDB.Field.BANNED_BY.toString()),
                jsonObject.getLong(GuildDB.Field.BANNED_AT.toString()),
                jsonObject.getLong(GuildDB.Field.BANNED_UNTIL.toString())
            )
            ret.add(bannedUser)
        }
        return ret
    }

    fun getBannedUsersWithUnbanTimes(): HashMap<Long, Long> {
        if (!guildHasInfo()) loadGuild()
        val bannedUsers = getBannedUsers()
        val ret = HashMap<Long, Long>()
        for (bannedUser in bannedUsers) ret[bannedUser.user] = bannedUser.bannedUntil
        return ret
    }

    fun banUser(uid: Long, modId: Long, bannedAt: Long, bannedUntil: Long) {
        if (!guildHasInfo()) loadGuild()
        require(!isBannedUser(uid)) { "This user is already banned!" }
        val guildObj = getCache().getGuildInfo(guild.idLong)
        val bannedUsers = guildObj.getJSONArray(GuildDB.Field.BANNED_USERS_ARRAY.toString())
        bannedUsers.put(
            JSONObject()
                .put(GuildDB.Field.BANNED_USER.toString(), uid)
                .put(GuildDB.Field.BANNED_BY.toString(), modId)
                .put(GuildDB.Field.BANNED_AT.toString(), bannedAt)
                .put(GuildDB.Field.BANNED_UNTIL.toString(), bannedUntil)
        )
        getCache().updateGuild(guildObj)
    }

    fun unbanUser(uid: Long) {
        if (!guildHasInfo()) loadGuild()
        require(isBannedUser(uid)) { "This user isn't banned!" }
        val guildObj = getCache().getGuildInfo(guild.idLong)
        val bannedUsers = guildObj.getJSONArray(GuildDB.Field.BANNED_USERS_ARRAY.toString())
        bannedUsers.remove(getIndexOfObjectInArray(bannedUsers, GuildDB.Field.BANNED_USER, uid))
        getCache().updateGuild(guildObj)
    }

    fun getTimeUntilUnban(uid: Long): Long {
        require(isBannedUser(uid)) { "This user isn't banned!" }
        val (_, _, bannedAt, bannedUntil) = getBannedUsers()
            .filter { user: BannedUser -> user.user == uid }[0]
        return bannedUntil - bannedAt
    }

    fun isBannedUser(uid: Long): Boolean {
        if (!guildHasInfo()) loadGuild()
        for (bannedUser in getBannedUsers())
            if (bannedUser.user == uid) return true
        return false
    }

    fun isPremium(): Boolean {
        return true
//        return Robertify.getRobertifyAPI().guildIsPremium(guild.idLong);
    }

    override fun update() {
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

        fun setTheme(theme: RobertifyTheme?): ConfigBuilder {
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