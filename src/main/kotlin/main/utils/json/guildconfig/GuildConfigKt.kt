package main.utils.json.guildconfig

import main.constants.RobertifyThemeKt
import main.utils.database.mongodb.databases.GuildDBKt
import main.utils.json.AbstractGuildConfigKt
import main.utils.json.autoplay.AutoPlayConfigKt
import main.utils.json.reminders.RemindersConfigKt
import net.dv8tion.jda.api.entities.Guild
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.*

class GuildConfigKt(private val guild: Guild) : AbstractGuildConfigKt(guild) {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
    }

    var twentyFourSevenMode: Boolean
        get() {
            if (!guildHasInfo()) loadGuild()
            if (!cache.hasField(guild.idLong, GuildDBKt.Field.TWENTY_FOUR_SEVEN)) {
                cache.setField(guild.idLong, GuildDBKt.Field.TWENTY_FOUR_SEVEN, false)
                return false
            }
            return cache.getField(guild.idLong, GuildDBKt.Field.TWENTY_FOUR_SEVEN) as Boolean
        }
        set(value) {
            if (!guildHasInfo()) loadGuild()
            cache.setField(guild.idLong, GuildDBKt.Field.TWENTY_FOUR_SEVEN, value)
        }

    fun addGuild() {
        require(!guildHasInfo()) { "This guild is already added!" }
        getDatabase().addGuild(guild.idLong)
    }

    fun removeGuild() {
        getDatabase().removeGuild(guild.idLong)
        if (!guildHasInfo()) logger.warn(
            "There is no information for guild with ID {} in the cache.",
            guild.idLong
        ) else unloadGuild()
    }

    fun getPrefix(): String {
        if (!guildHasInfo()) loadGuild()
        return cache.getField(guild.idLong, GuildDBKt.Field.GUILD_PREFIX) as String
    }

    fun setPrefix(prefix: String) {
        if (!guildHasInfo()) loadGuild()
        require(prefix.length <= 4) { "The prefix must be 4 or less characters!" }
        cache.setField(guild.idLong, GuildDBKt.Field.GUILD_PREFIX, prefix)
    }

    fun setManyFields(builder: ConfigBuilder.() -> ConfigBuilder) {
        cache.setFields(guild.idLong, builder(ConfigBuilder()).build())
    }

    fun getBannedUsers(): List<BannedUser> {
        if (!guildHasInfo()) loadGuild()
        val bannedUsers = cache.getField(guild.idLong, GuildDBKt.Field.BANNED_USERS_ARRAY) as JSONArray
        val ret: MutableList<BannedUser> = ArrayList()
        for (i in 0 until bannedUsers.length()) {
            val jsonObject = bannedUsers.getJSONObject(i)
            val bannedUser = BannedUser(
                jsonObject.getLong(GuildDBKt.Field.BANNED_USER.toString()),
                jsonObject.getLong(GuildDBKt.Field.BANNED_BY.toString()),
                jsonObject.getLong(GuildDBKt.Field.BANNED_AT.toString()),
                jsonObject.getLong(GuildDBKt.Field.BANNED_UNTIL.toString())
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
        val guildObj = cache.getGuildInfo(guild.idLong) ?: return
        val bannedUsers = guildObj.getJSONArray(GuildDBKt.Field.BANNED_USERS_ARRAY.toString())
        bannedUsers.put(
            JSONObject()
                .put(GuildDBKt.Field.BANNED_USER.toString(), uid)
                .put(GuildDBKt.Field.BANNED_BY.toString(), modId)
                .put(GuildDBKt.Field.BANNED_AT.toString(), bannedAt)
                .put(GuildDBKt.Field.BANNED_UNTIL.toString(), bannedUntil)
        )
        cache.updateGuild(guildObj)
    }

    fun unbanUser(uid: Long) {
        if (!guildHasInfo()) loadGuild()
        require(isBannedUser(uid)) { "This user isn't banned!" }
        val guildObj = cache.getGuildInfo(guild.idLong) ?: return
        val bannedUsers = guildObj.getJSONArray(GuildDBKt.Field.BANNED_USERS_ARRAY.toString())
        bannedUsers.remove(getIndexOfObjectInArray(bannedUsers, GuildDBKt.Field.BANNED_USER, uid))
        cache.updateGuild(guildObj)
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
        private var theme: RobertifyThemeKt? = null
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

        fun setTheme(theme: RobertifyThemeKt): ConfigBuilder {
            this.theme = theme
            return this
        }

        fun build(): JSONObject {
            val obj = JSONObject()
            if (reminders != null) obj.put(
                RemindersConfigKt.Fields.REMINDERS.name.lowercase(Locale.getDefault()),
                reminders
            )
            if (dedicatedChannel != null) obj.put(GuildDBKt.Field.REQUEST_CHANNEL_OBJECT.toString(), dedicatedChannel)
            if (restrictedChannels != null) obj.put(
                GuildDBKt.Field.RESTRICTED_CHANNELS_OBJECT.toString(),
                restrictedChannels
            )
            if (permissions != null) obj.put(GuildDBKt.Field.PERMISSIONS_OBJECT.toString(), permissions)
            if (toggles != null) obj.put(GuildDBKt.Field.TOGGLES_OBJECT.toString(), toggles)
            if (eightBall != null) obj.put(GuildDBKt.Field.EIGHT_BALL_ARRAY.toString(), eightBall)
            if (theme != null) obj.put(GuildDBKt.Field.THEME.toString(), theme!!.name.lowercase(Locale.getDefault()))
            if (bannedUsers != null) obj.put(GuildDBKt.Field.BANNED_USERS_ARRAY.toString(), bannedUsers)
            if (twentyFourSeven != null) obj.put(GuildDBKt.Field.TWENTY_FOUR_SEVEN.toString(), twentyFourSeven)
            if (autoPlay != null) obj.put(AutoPlayConfigKt.Field.AUTOPLAY.name.lowercase(Locale.getDefault()), autoPlay)
            return obj
        }
    }

}