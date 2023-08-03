package main.utils.database.mongodb.cache.redis.guild

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import main.constants.RobertifyTheme
import main.utils.database.mongodb.databases.GuildDB
import main.utils.locale.RobertifyLocale
import org.json.JSONObject

@Serializable
data class GuildDatabaseModel(
    val server_id: Long,
    var prefix: String = "+",
    var announcement_channel: Long = -1,
    var banned_users: MutableList<BannedUserModel> = emptyList<BannedUserModel>().toMutableList(),
    var dedicated_channel: RequestChannelModel = RequestChannelModel(),
    var permissions: PermissionsModel = PermissionsModel(),
    var restricted_channels: RestrictedChannelsModel = RestrictedChannelsModel(),
    var toggles: TogglesModel = TogglesModel(),
    var reminders: RemindersModel = RemindersModel(),
    var eight_ball: MutableList<String> = mutableListOf(),
    var theme: String = RobertifyTheme.GREEN.name.lowercase(),
    var autoplay: Boolean = false,
    var twenty_four_seven_mode: Boolean = false,
    var log_channel: Long = -1,
    var locale: String = RobertifyLocale.ENGLISH.name.lowercase()
) : JsonObjectTransferable {

    fun dedicated_channel(block: RequestChannelModel.() -> Unit) {
        val dediChannelStandIn = RequestChannelModel(null, null, null, null)
        block(dediChannelStandIn)
        dedicated_channel = RequestChannelModel(
            dediChannelStandIn.message_id ?: dedicated_channel.message_id,
            dediChannelStandIn.channel_id ?: dedicated_channel.channel_id,
            dediChannelStandIn.config ?: dedicated_channel.config,
            dediChannelStandIn.og_announcement_toggle ?: dedicated_channel.og_announcement_toggle
        )
    }

    fun toggles(block: TogglesModelOptional.() -> Unit) {
        val togglesStandIn = TogglesModelOptional()
        block(togglesStandIn)
        toggles = TogglesModel(
            togglesStandIn.restricted_text_channels ?: toggles.restricted_text_channels,
            togglesStandIn.restricted_voice_channels ?: toggles.restricted_voice_channels,
            togglesStandIn.announce_changelogs ?: toggles.announce_changelogs,
            togglesStandIn.`8ball` ?: toggles.`8ball`,
            togglesStandIn.show_requester ?: toggles.show_requester,
            togglesStandIn.vote_skips ?: toggles.vote_skips,
            togglesStandIn.announce_messages ?: toggles.announce_messages,
            togglesStandIn.polls ?: toggles.polls,
            togglesStandIn.tips ?: toggles.tips,
            togglesStandIn.global_announcements ?: toggles.global_announcements,
            togglesStandIn.reminders ?: toggles.reminders,
            togglesStandIn.log_toggles ?: toggles.log_toggles,
            togglesStandIn.dj_toggles ?: toggles.dj_toggles,
        )
    }

    fun restricted_channels(block: RestrictedChannelsModel.() -> Unit) {
        block(restricted_channels)
    }

    fun reminders(block: RemindersModel.() -> Unit) {
        block(reminders)
    }

    fun eight_ball(block: MutableList<String>.() -> Unit) {
        block(eight_ball)
    }

    override fun toJsonObject(): JSONObject {
        return JSONObject()
            .put(GuildDB.Field.GUILD_ID.toString(), server_id)
            .put(GuildDB.Field.GUILD_PREFIX.toString(), prefix)
            .put(GuildDB.Field.ANNOUNCEMENT_CHANNEL.toString(), announcement_channel)
            .put(GuildDB.Field.BANNED_USERS_ARRAY.toString(), banned_users.map { it.toJsonObject()})
            .put(GuildDB.Field.REQUEST_CHANNEL_OBJECT.toString(), dedicated_channel.toJsonObject())
            .put(GuildDB.Field.PERMISSIONS_OBJECT.toString(), permissions.toJsonObject())
            .put(GuildDB.Field.RESTRICTED_CHANNELS_OBJECT.toString(), restricted_channels.toJsonObject())
            .put(GuildDB.Field.TOGGLES_OBJECT.toString(), toggles.toJsonObject())
            .put("reminders", reminders.toJsonObject())
            .put(GuildDB.Field.EIGHT_BALL_ARRAY.toString(), eight_ball)
            .put(GuildDB.Field.THEME.toString(), theme)
            .put("autoplay", autoplay)
            .put("twenty_four_seven_mode", twenty_four_seven_mode)
            .put(GuildDB.Field.LOG_CHANNEL.toString(), log_channel)
            .put("locale", locale)
    }
}

@Serializable
data class BannedUserModel(
    val banned_id: Long,
    val banned_by: Long,
    val banned_until: Long,
    val banned_at: Long
) : JsonObjectTransferable {

    override fun toJsonObject(): JSONObject {
        return JSONObject()
            .put(GuildDB.Field.BANNED_USER.toString(), banned_id)
            .put(GuildDB.Field.BANNED_BY.toString(), banned_by)
            .put(GuildDB.Field.BANNED_UNTIL.toString(), banned_until)
            .put(GuildDB.Field.BANNED_AT.toString(), banned_at)
    }
}

@Serializable
data class RequestChannelModel(
    var message_id: Long? = -1,
    var channel_id: Long? = -1,
    var config: RequestChannelConfigModel? = RequestChannelConfigModel(),
    var og_announcement_toggle: Boolean? = true
) : JsonObjectTransferable {

    override fun toJsonObject(): JSONObject {
        return JSONObject()
            .put(GuildDB.Field.REQUEST_CHANNEL_ID.toString(), channel_id)
            .put(GuildDB.Field.REQUEST_CHANNEL_MESSAGE_ID.toString(), message_id)
            .put(GuildDB.Field.REQUEST_CHANNEL_CONFIG.toString(), config?.toJsonObject())
            .put("og_announcement_toggle", og_announcement_toggle)
    }
}

@Serializable
data class RequestChannelConfigModel(
    var disconnect: Boolean = true,
    var play_pause: Boolean = true,
    var previous: Boolean = true,
    var rewind: Boolean = true,
    var stop: Boolean = true,
    var loop: Boolean = true,
    var skip: Boolean = true,
    var filters: Boolean = false,
    var favourite: Boolean = true,
    var shuffle: Boolean = true,
) : JsonObjectTransferable {

    override fun toJsonObject(): JSONObject {
        return JSONObject()
            .put("disconnect", disconnect)
            .put("play_pause", play_pause)
            .put("previous", previous)
            .put("rewind", rewind)
            .put("stop", stop)
            .put("loop", loop)
            .put("skip", skip)
            .put("filters", filters)
            .put("shuffle", shuffle)
            .put("favourite", favourite)
    }
}

@Serializable
data class PermissionsModel(
    var `0`: MutableList<Long>? = null,
    var `1`: MutableList<Long>? = null,
    var `2`: MutableList<Long>? = null,
    var `3`: MutableList<Long>? = null,
    var `4`: MutableList<Long>? = null,
    var `5`: MutableList<Long>? = null,
    var users: JsonObject? = null
) : JsonObjectTransferable {

    override fun toJsonObject(): JSONObject {
        return JSONObject()
            .put("0", `0`)
            .put("1", `1`)
            .put("2", `2`)
            .put("3", `3`)
            .put("4", `4`)
            .put("5", `5`)
            .put("users", users)
    }
}

@Serializable
data class RestrictedChannelsModel(
    var voice_channels: MutableList<Long> = mutableListOf(),
    var text_channels: MutableList<Long> = mutableListOf()
) : JsonObjectTransferable {

    override fun toJsonObject(): JSONObject {
        return JSONObject()
            .put("voice_channels", voice_channels)
            .put("text_channels", text_channels)
    }
}

@Serializable
data class TogglesModel(
    var restricted_text_channels: Boolean = false,
    var restricted_voice_channels: Boolean = false,
    var announce_changelogs: Boolean = false,
    var `8ball`: Boolean = true,
    var show_requester: Boolean = true,
    var vote_skips: Boolean = false,
    var announce_messages: Boolean = true,
    var polls: Boolean = true,
    var tips: Boolean = true,
    var global_announcements: Boolean = true,
    var reminders: Boolean = true,
    var log_toggles: LogTogglesModel = LogTogglesModel(),
    var dj_toggles: DJTogglesModel = DJTogglesModel(),
) : JsonObjectTransferable {

    override fun toJsonObject(): JSONObject {
        return JSONObject()
            .put("restricted_text_channels", restricted_text_channels)
            .put("restricted_voice_channels", restricted_voice_channels)
            .put("announce_changelogs", announce_changelogs)
            .put("8ball", `8ball`)
            .put("show_requester", show_requester)
            .put("vote_skips", vote_skips)
            .put("announce_messages", announce_messages)
            .put("polls", polls)
            .put("tips", tips)
            .put("global_announcements", global_announcements)
            .put("reminders", reminders)
            .put("log_toggles", log_toggles.toJsonObject())
            .put("dj_toggles", dj_toggles.toJsonObject())
    }

    fun log_toggles(block: LogTogglesModelOptional.() -> Unit) {
        val logTogglesStandIn = LogTogglesModelOptional()
        block(logTogglesStandIn)
        log_toggles = LogTogglesModel(
            logTogglesStandIn.queue_add ?: log_toggles.queue_add,
            logTogglesStandIn.track_move ?: log_toggles.track_move,
            logTogglesStandIn.track_loop ?: log_toggles.track_loop,
            logTogglesStandIn.player_pause ?: log_toggles.player_pause,
            logTogglesStandIn.track_vote_skip ?: log_toggles.track_vote_skip,
            logTogglesStandIn.queue_shuffle ?: log_toggles.queue_shuffle,
            logTogglesStandIn.player_resume ?: log_toggles.player_resume,
            logTogglesStandIn.volume_change ?: log_toggles.volume_change,
            logTogglesStandIn.track_seek ?: log_toggles.track_seek,
            logTogglesStandIn.track_previous ?: log_toggles.track_previous,
            logTogglesStandIn.track_skip ?: log_toggles.track_skip,
            logTogglesStandIn.track_rewind ?: log_toggles.track_rewind,
            logTogglesStandIn.bot_disconnected ?: log_toggles.bot_disconnected,
            logTogglesStandIn.queue_remove ?: log_toggles.queue_remove,
            logTogglesStandIn.filter_toggle ?: log_toggles.filter_toggle,
            logTogglesStandIn.player_stop ?: log_toggles.player_stop,
            logTogglesStandIn.queue_loop ?: log_toggles.queue_loop,
            logTogglesStandIn.queue_clear ?: log_toggles.queue_clear,
            logTogglesStandIn.track_jump ?: log_toggles.track_jump
        )
    }

    fun dj_toggles(block: DJTogglesModelOptional.() -> Unit) {
        val djTogglesStandIn = DJTogglesModelOptional()
        block(djTogglesStandIn)
        dj_toggles = DJTogglesModel(
            djTogglesStandIn.`247` ?: dj_toggles.`247`,
            djTogglesStandIn.play ?: dj_toggles.play,
            djTogglesStandIn.disconnect ?: dj_toggles.disconnect,
            djTogglesStandIn.favouritetracks ?: dj_toggles.favouritetracks,
            djTogglesStandIn.skip ?: dj_toggles.skip,
            djTogglesStandIn.seek ?: dj_toggles.seek,
            djTogglesStandIn.remove ?: dj_toggles.remove,
            djTogglesStandIn.play ?: dj_toggles.play,
            djTogglesStandIn.tremolo ?: dj_toggles.tremolo,
            djTogglesStandIn.search ?: dj_toggles.search,
            djTogglesStandIn.loop ?: dj_toggles.loop,
            djTogglesStandIn.nightcore ?: dj_toggles.nightcore,
            djTogglesStandIn.join ?: dj_toggles.join,
            djTogglesStandIn.lyrics ?: dj_toggles.lyrics,
            djTogglesStandIn.jump ?: dj_toggles.jump,
            djTogglesStandIn.vibrato ?: dj_toggles.vibrato,
            djTogglesStandIn.resume ?: dj_toggles.resume,
            djTogglesStandIn.move ?: dj_toggles.move,
            djTogglesStandIn.nowplaying ?: dj_toggles.nowplaying,
            djTogglesStandIn.previous ?: dj_toggles.previous,
            djTogglesStandIn.clear ?: dj_toggles.clear,
            djTogglesStandIn.skipto ?: dj_toggles.skipto,
            djTogglesStandIn.`8d` ?: dj_toggles.`8d`,
            djTogglesStandIn.pause ?: dj_toggles.pause,
            djTogglesStandIn.autoplay ?: dj_toggles.autoplay,
            djTogglesStandIn.volume ?: dj_toggles.volume,
            djTogglesStandIn.lofi ?: dj_toggles.lofi,
            djTogglesStandIn.rewind ?: dj_toggles.rewind,
            djTogglesStandIn.stop ?: dj_toggles.stop,
            djTogglesStandIn.shuffleplay ?: dj_toggles.shuffleplay,
            djTogglesStandIn.queue ?: dj_toggles.queue,
            djTogglesStandIn.history ?: dj_toggles.history,
            djTogglesStandIn.searchqueue ?: dj_toggles.searchqueue,
        )
    }
}

@Serializable
data class TogglesModelOptional(
    var restricted_text_channels: Boolean? = null,
    var restricted_voice_channels: Boolean? = null,
    var announce_changelogs: Boolean? = null,
    var `8ball`: Boolean? = null,
    var show_requester: Boolean? = null,
    var vote_skips: Boolean? = null,
    var announce_messages: Boolean? = null,
    var polls: Boolean? = null,
    var tips: Boolean? = null,
    var global_announcements: Boolean? = null,
    var reminders: Boolean? = null,
    var log_toggles: LogTogglesModel? = null,
    var dj_toggles: DJTogglesModel? = null,
) : JsonObjectTransferable {
    override fun toJsonObject(): JSONObject {
        return JSONObject(Json.encodeToString(this))
    }
}

@Serializable
data class LogTogglesModel(
    var queue_add: Boolean = true,
    var track_move: Boolean = true,
    var track_loop: Boolean = true,
    var player_pause: Boolean = true,
    var track_vote_skip: Boolean = true,
    var queue_shuffle: Boolean = true,
    var player_resume: Boolean = true,
    var volume_change: Boolean = true,
    var track_seek: Boolean = true,
    var track_previous: Boolean = true,
    var track_skip: Boolean = true,
    var track_rewind: Boolean = true,
    var bot_disconnected: Boolean = true,
    var queue_remove: Boolean = true,
    var filter_toggle: Boolean = true,
    var player_stop: Boolean = true,
    var queue_loop: Boolean = true,
    var queue_clear: Boolean = true,
    var track_jump: Boolean = true
) : JsonObjectTransferable {

    override fun toJsonObject(): JSONObject {
        return JSONObject()
            .put("queue_add", queue_add)
            .put("track_move", track_move)
            .put("track_loop", track_loop)
            .put("player_pause", player_pause)
            .put("track_vote_skip", track_vote_skip)
            .put("queue_shuffle", queue_shuffle)
            .put("player_resume", player_resume)
            .put("volume_change", volume_change)
            .put("track_seek", track_seek)
            .put("track_previous", track_previous)
            .put("track_skip", track_skip)
            .put("track_rewind", track_rewind)
            .put("bot_disconnected", bot_disconnected)
            .put("queue_remove", queue_remove)
            .put("filter_toggle", filter_toggle)
            .put("player_stop", player_stop)
            .put("queue_loop", queue_loop)
            .put("queue_clear", queue_clear)
            .put("track_jump", track_jump)
    }
}

@Serializable
data class LogTogglesModelOptional(
    var queue_add: Boolean? = null,
    var track_move: Boolean? = null,
    var track_loop: Boolean? = null,
    var player_pause: Boolean? = null,
    var track_vote_skip: Boolean? = null,
    var queue_shuffle: Boolean? = null,
    var player_resume: Boolean? = null,
    var volume_change: Boolean? = null,
    var track_seek: Boolean? = null,
    var track_previous: Boolean? = null,
    var track_skip: Boolean? = null,
    var track_rewind: Boolean? = null,
    var bot_disconnected: Boolean? = null,
    var queue_remove: Boolean? = null,
    var filter_toggle: Boolean? = null,
    var player_stop: Boolean? = null,
    var queue_loop: Boolean? = null,
    var queue_clear: Boolean? = null,
    var track_jump: Boolean? = null,
) : JsonObjectTransferable {

    override fun toJsonObject(): JSONObject {
        return JSONObject(Json.encodeToString(this))
    }
}

@Serializable
data class DJTogglesModel(
    var `247`: Boolean = true,
    var play: Boolean = true,
    var disconnect: Boolean = true,
    var favouritetracks: Boolean = true,
    var skip: Boolean = true,
    var seek: Boolean = true,
    var remove: Boolean = true,
    var karaoke: Boolean = true,
    var tremolo: Boolean = true,
    var search: Boolean = true,
    var loop: Boolean = true,
    var nightcore: Boolean = true,
    var join: Boolean = true,
    var lyrics: Boolean = true,
    var jump: Boolean = true,
    var vibrato: Boolean = true,
    var resume: Boolean = true,
    var move: Boolean = true,
    var nowplaying: Boolean = true,
    var previous: Boolean = true,
    var clear: Boolean = true,
    var skipto: Boolean = true,
    var `8d`: Boolean = true,
    var pause: Boolean = true,
    var autoplay: Boolean = true,
    var volume: Boolean = true,
    var lofi: Boolean = true,
    var rewind: Boolean = true,
    var stop: Boolean = true,
    var shuffleplay: Boolean = true,
    var shuffle: Boolean = true,
    var queue: Boolean = true,
    var history: Boolean = true,
    var searchqueue: Boolean = true,
    var removedupes: Boolean = true,
) : JsonObjectTransferable {

    override fun toJsonObject(): JSONObject {
        return JSONObject()
            .put("247", `247`)
            .put("play", play)
            .put("disconnect", disconnect)
            .put("favouritetracks", favouritetracks)
            .put("skip", skip)
            .put("seek", seek)
            .put("remove", remove)
            .put("karaoke", karaoke)
            .put("tremolo", tremolo)
            .put("search", search)
            .put("loop", loop)
            .put("nightcore", nightcore)
            .put("join", join)
            .put("lyrics", lyrics)
            .put("jump", jump)
            .put("vibrato", vibrato)
            .put("resume", resume)
            .put("move", move)
            .put("nowplaying", nowplaying)
            .put("previous", previous)
            .put("clear", clear)
            .put("skipto", skipto)
            .put("8d", `8d`)
            .put("pause", pause)
            .put("autoplay", autoplay)
            .put("volume", volume)
            .put("lofi", lofi)
            .put("rewind", rewind)
            .put("stop", stop)
            .put("shuffleplay", shuffleplay)
            .put("shuffle", shuffle)
            .put("queue", queue)
            .put("history", history)
            .put("searchqueue", searchqueue)
            .put("removedupes", removedupes)
    }
}

@Serializable
data class DJTogglesModelOptional(
    var `247`: Boolean? = null,
    var play: Boolean? = null,
    var disconnect: Boolean? = null,
    var favouritetracks: Boolean? = null,
    var skip: Boolean? = null,
    var seek: Boolean? = null,
    var remove: Boolean? = null,
    var karaoke: Boolean? = null,
    var tremolo: Boolean? = null,
    var search: Boolean? = null,
    var loop: Boolean? = null,
    var nightcore: Boolean? = null,
    var join: Boolean? = null,
    var lyrics: Boolean? = null,
    var jump: Boolean? = null,
    var vibrato: Boolean? = null,
    var resume: Boolean? = null,
    var move: Boolean? = null,
    var nowplaying: Boolean? = null,
    var previous: Boolean? = null,
    var clear: Boolean? = null,
    var skipto: Boolean? = null,
    var `8d`: Boolean? = null,
    var pause: Boolean? = null,
    var autoplay: Boolean? = null,
    var volume: Boolean? = null,
    var lofi: Boolean? = null,
    var rewind: Boolean? = null,
    var stop: Boolean? = null,
    var shuffleplay: Boolean? = null,
    var queue: Boolean? = null,
    var history: Boolean? = null,
    var searchqueue: Boolean? = null,
) : JsonObjectTransferable {

    override fun toJsonObject(): JSONObject {
        return JSONObject(Json.encodeToString(this))
    }
}

@Serializable
data class RemindersModel(
    val banned_channels: MutableList<Long> = mutableListOf(),
    val users: MutableList<ReminderUserModel> = mutableListOf()
) : JsonObjectTransferable {

    override fun toJsonObject(): JSONObject {
        return JSONObject()
            .put("banned_channels", banned_channels)
            .put("users", users.map { it.toJsonObject() })
    }
}

@Serializable
data class ReminderUserModel(
    val user_id: Long,
    var is_banned: Boolean,
    val user_reminders: MutableList<ReminderModel> = mutableListOf()
) : JsonObjectTransferable {

    override fun toJsonObject(): JSONObject {
        return JSONObject()
            .put("user_id", user_id)
            .put("is_banned", is_banned)
            .put("user_reminders", user_reminders.map { it.toJsonObject() })
    }
}

@Serializable
data class ReminderModel(
    var reminder_time: Long,
    var reminder: String,
    var reminder_timezone: String? = null,
    var reminder_channel: Long,
) : JsonObjectTransferable {

    override fun toJsonObject(): JSONObject {
        return JSONObject()
            .put("reminder_time", reminder_time)
            .put("reminder", reminder)
            .put("reminder_timezone", reminder_timezone)
            .put("reminder_channel", reminder_channel)
    }
}

interface JsonObjectTransferable {
    fun toJsonObject(): JSONObject
}


