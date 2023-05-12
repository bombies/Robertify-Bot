package main.utils.json.requestchannel

import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack
import main.audiohandlers.RobertifyAudioManager
import main.audiohandlers.utils.author
import main.audiohandlers.utils.length
import main.audiohandlers.utils.title
import main.constants.RobertifyEmoji
import main.main.Robertify
import main.utils.GeneralUtils
import main.utils.RobertifyEmbedUtils
import main.utils.component.interactions.selectionmenu.StringSelectMenuOption
import main.utils.component.interactions.selectionmenu.StringSelectionMenuBuilder
import main.utils.database.mongodb.databases.GuildDB
import main.utils.json.AbstractGuildConfig
import main.utils.json.themes.ThemesConfig
import main.utils.locale.LocaleManager
import main.utils.locale.messages.DedicatedChannelMessages
import main.utils.locale.messages.FilterMessages
import main.utils.locale.messages.NowPlayingMessages
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageHistory
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.MessageEditAction
import net.dv8tion.jda.api.sharding.ShardManager
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class RequestChannelConfig(private val guild: Guild, private val shardManager: ShardManager = Robertify.shardManager) : AbstractGuildConfig(guild) {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)

        fun updateAllButtons() {
            for (g: Guild in Robertify.shardManager.guilds) {
                val config = RequestChannelConfig(g)
                if (!config.isChannelSet()) continue
                val msgRequest: RestAction<Message> = config.messageRequest ?: continue
                msgRequest.queue { msg: Message ->
                    config.buttonUpdateRequest(msg).queue(
                        null,
                        ErrorHandler()
                            .handle(
                                ErrorResponse.MISSING_PERMISSIONS
                            ) {
                                logger.warn(
                                    "Couldn't update buttons in {}",
                                    g.name
                                )
                            }
                    )
                }
            }
        }

        fun updateAllTopics() {
            for (g: Guild in Robertify.shardManager.guilds) {
                val config = RequestChannelConfig(g)
                if (!config.isChannelSet()) continue
                val channel: TextChannel = config.textChannel ?: continue
                config.channelTopicUpdateRequest(channel)?.queue()
            }
        }
    }

    var messageId: Long
        get() {
            if (!isChannelSet()) throw IllegalArgumentException(
                shardManager.getGuildById(guild.idLong)
                    ?.name + "(" + guild.idLong + ") doesn't have a channel set"
            )
            return getGuildObject().getJSONObject(GuildDB.Field.REQUEST_CHANNEL_OBJECT.toString())
                .getLong(GuildDB.Field.REQUEST_CHANNEL_MESSAGE_ID.toString())
        }
        set(value) {
            val obj: JSONObject = getGuildObject()
            val dediChannelObj: JSONObject = obj.getJSONObject(GuildDB.Field.REQUEST_CHANNEL_OBJECT.toString())
            dediChannelObj.put(GuildDB.Field.REQUEST_CHANNEL_MESSAGE_ID.toString(), value)
            cache.setField(guild.idLong, GuildDB.Field.REQUEST_CHANNEL_OBJECT, dediChannelObj)
        }

    var channelId: Long
        get() {
            if (!isChannelSet()) throw IllegalArgumentException(
                shardManager.getGuildById(guild.idLong)
                    ?.name + "(" + guild.idLong + ") doesn't have a channel set"
            )
            return getGuildObject().getJSONObject(GuildDB.Field.REQUEST_CHANNEL_OBJECT.toString())
                .getLong(GuildDB.Field.REQUEST_CHANNEL_ID.toString())
        }
        set(value) {
            val obj: JSONObject = getGuildObject()
            val dediChannelObject: JSONObject = obj.getJSONObject(GuildDB.Field.REQUEST_CHANNEL_OBJECT.toString())
            dediChannelObject.put(GuildDB.Field.REQUEST_CHANNEL_ID.toString(), value)
            cache.setField(guild.idLong, GuildDB.Field.REQUEST_CHANNEL_OBJECT, dediChannelObject)
        }

    var originalAnnouncementToggle: Boolean
        get() {
            return getGuildObject().getJSONObject(GuildDB.Field.REQUEST_CHANNEL_OBJECT.toString())
                .getBoolean(RequestChannelConfigField.ORIGINAL_ANNOUNCEMENT_TOGGLE.toString())
        }
        set(value) {
            val obj: JSONObject = getGuildObject()
            val dedicatedChannelObj: JSONObject = obj.getJSONObject(GuildDB.Field.REQUEST_CHANNEL_OBJECT.toString())
            dedicatedChannelObj.put(RequestChannelConfigField.ORIGINAL_ANNOUNCEMENT_TOGGLE.toString(), value)
            cache.setField(guild.idLong, GuildDB.Field.REQUEST_CHANNEL_OBJECT, dedicatedChannelObj)
        }

    val textChannel: TextChannel?
        get() = shardManager.getTextChannelById(channelId)

    val config: ChannelConfig
        get() = ChannelConfig(this)

    val messageRequest: RestAction<Message>?
        get() = try {
            textChannel!!.retrieveMessageById(messageId)
        } catch (e: InsufficientPermissionException) {
            val channel: GuildMessageChannel? = RobertifyAudioManager
                .getMusicManager(guild)
                .scheduler
                .announcementChannel
            channel?.sendMessageEmbeds(
                RobertifyEmbedUtils.embedMessage(
                    channel.guild,
                    "I don't have access to the requests channel anymore! I cannot update it."
                ).build()
            )?.queue(null, ErrorHandler().ignore(ErrorResponse.MISSING_PERMISSIONS))
            null
        }

    @Synchronized
    fun setChannelAndMessage(cid: Long, mid: Long) {
        val obj: JSONObject = getGuildObject()
        val dediChannelObject: JSONObject = obj.getJSONObject(GuildDB.Field.REQUEST_CHANNEL_OBJECT.toString())
        dediChannelObject.put(GuildDB.Field.REQUEST_CHANNEL_ID.toString(), cid)
        dediChannelObject.put(GuildDB.Field.REQUEST_CHANNEL_MESSAGE_ID.toString(), mid)
        cache.setField(guild.idLong, GuildDB.Field.REQUEST_CHANNEL_OBJECT, dediChannelObject)
    }

    @Synchronized
    fun removeChannel() {
        if (!isChannelSet())
            throw IllegalArgumentException(
                "${shardManager.getGuildById(guild.idLong)?.name} (${guild.idLong}) doesn't have a request channel set."
            )

        textChannel?.delete()?.queue(null, ErrorHandler().ignore(ErrorResponse.MISSING_PERMISSIONS))

        val obj: JSONObject = getGuildObject().getJSONObject(GuildDB.Field.REQUEST_CHANNEL_OBJECT.toString())
        obj.put(GuildDB.Field.REQUEST_CHANNEL_ID.toString(), -1)
        obj.put(GuildDB.Field.REQUEST_CHANNEL_MESSAGE_ID.toString(), -1)
        cache.setField(guild.idLong, GuildDB.Field.REQUEST_CHANNEL_OBJECT, obj)
    }

    @Synchronized
    fun isChannelSet(): Boolean {
        val reqChannelObj: JSONObject =
            getGuildObject().getJSONObject(GuildDB.Field.REQUEST_CHANNEL_OBJECT.toString())
        return try {
            !reqChannelObj.isNull(GuildDB.Field.REQUEST_CHANNEL_ID.toString()) && reqChannelObj
                .getLong(GuildDB.Field.REQUEST_CHANNEL_ID.toString()) != -1L
        } catch (e: JSONException) {
            if (e.message!!.contains("is not a ")) {
                reqChannelObj.getString(GuildDB.Field.REQUEST_CHANNEL_ID.toString()) != "-1"
            } else throw e
        }
    }

    fun isRequestChannel(channel: GuildMessageChannel): Boolean = when {
        !isChannelSet() -> false
        else -> channelId == channel.idLong
    }

    @Synchronized
    fun updateMessage(): CompletableFuture<Void?>? {
        if (!isChannelSet()) return null
        return CompletableFuture.runAsync {
            val msgRequest: RestAction<Message> = messageRequest ?: return@runAsync
            val musicManager = RobertifyAudioManager[guild]
            val audioPlayer = musicManager.player
            val playingTrack = audioPlayer.playingTrack
            val queueHandler = musicManager.scheduler.queueHandler
            val queueAsList = ArrayList(queueHandler.contents)

            val theme = ThemesConfig(guild).theme
            val localeManager = LocaleManager[guild]
            val eb = EmbedBuilder()

            if (playingTrack == null) {
                eb.setColor(theme.color)
                eb.setTitle(localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_NOTHING_PLAYING))
                eb.setImage(theme.idleBanner)
                val scheduler = musicManager.scheduler
                val announcementChannel: GuildMessageChannel? = scheduler.announcementChannel
                try {
                    msgRequest.queue(
                        { msg: Message ->
                            msg.editMessage(localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_QUEUE_NOTHING_PLAYING))
                                .setEmbeds(eb.build())
                                .queue(
                                    null,
                                    ErrorHandler()
                                        .handle(
                                            ErrorResponse.MISSING_PERMISSIONS
                                        ) {
                                            sendEditErrorMessage(
                                                announcementChannel
                                            )
                                        }
                                )
                        },
                        ErrorHandler()
                            .handle(
                                ErrorResponse.UNKNOWN_MESSAGE
                            ) { removeChannel() }
                            .handle(
                                ErrorResponse.MISSING_PERMISSIONS
                            ) {
                                sendEditErrorMessage(
                                    announcementChannel
                                )
                            }
                    )
                } catch (e: InsufficientPermissionException) {
                    if (e.message!!.contains("MESSAGE_SEND")) sendEditErrorMessage(announcementChannel)
                }
            } else {
                eb.setColor(theme.color)
                eb.setTitle(
                    localeManager.getMessage(
                        DedicatedChannelMessages.DEDICATED_CHANNEL_PLAYING_EMBED_TITLE,
                        Pair(
                            "{title}",
                            playingTrack.title
                        ),
                        Pair(
                            "{author}",
                            playingTrack.author
                        ),

                        Pair(
                            "{duration}",
                            GeneralUtils.formatTime(playingTrack.length)
                        )
                    )
                )
                val requester: String = musicManager.scheduler.getRequesterAsMention(playingTrack)
                eb.setDescription(
                    localeManager.getMessage(
                        NowPlayingMessages.NP_ANNOUNCEMENT_REQUESTER,
                        Pair("{requester}", requester)
                    )
                )

                if (playingTrack is MirroringAudioTrack) eb.setImage(playingTrack.artworkURL) else eb.setImage(
                    theme.nowPlayingBanner
                )
                eb.setFooter(
                    localeManager.getMessage(
                        DedicatedChannelMessages.DEDICATED_CHANNEL_PLAYING_EMBED_FOOTER,
                        Pair(
                            "{numSongs}",
                            queueAsList.size.toString()
                        ),
                        Pair(
                            "{volume}",
                            ((audioPlayer.filters.volume * 100).toInt()).toString()
                        )
                    )
                )
                val nextTenSongs: StringBuilder = StringBuilder()
                nextTenSongs.append("```")
                if (queueAsList.size > 10) {
                    var index = 1
                    for (track in queueAsList.subList(
                        0,
                        10
                    )) nextTenSongs.append(index++).append(". → ").append(track.title)
                        .append(" - ").append(track.author)
                        .append(" [").append(GeneralUtils.formatTime(track.length))
                        .append("]\n")
                } else {
                    if (queueHandler.isEmpty) nextTenSongs.append(localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_QUEUE_NO_SONGS)) else {
                        var index = 1
                        for (track in queueAsList) nextTenSongs.append(
                            index++
                        ).append(". → ").append(track.title).append(" - ").append(track.author)
                            .append(" [").append(GeneralUtils.formatTime(track.length))
                            .append("]\n")
                    }
                }
                nextTenSongs.append("```")
                msgRequest.queue(
                    { msg: Message ->
                        msg.editMessage(
                            localeManager.getMessage(
                                DedicatedChannelMessages.DEDICATED_CHANNEL_QUEUE_PLAYING,
                                Pair(
                                    "{songs}",
                                    nextTenSongs.toString()
                                )
                            )
                        )
                            .setEmbeds(eb.build())
                            .queue()
                    },
                    ErrorHandler()
                        .handle(
                            ErrorResponse.UNKNOWN_MESSAGE
                        ) { removeChannel() }
                )
            }
        }
    }

    private fun sendEditErrorMessage(messageChannel: GuildMessageChannel?) {
        sendErrorMessage(
            messageChannel,
            DedicatedChannelMessages.DEDICATED_CHANNEL_SELF_INSUFFICIENT_PERMS_EDIT
        )
    }

    private fun sendErrorMessage(
        messageChannel: GuildMessageChannel?,
        message: DedicatedChannelMessages
    ) {
        messageChannel?.sendMessageEmbeds(
            RobertifyEmbedUtils.embedMessage(
                guild,
                message
            ).build()
        )?.queue { errMsg: Message ->
            errMsg.delete().queueAfter(
                10,
                TimeUnit.SECONDS,
                null,
                ErrorHandler()
                    .ignore(ErrorResponse.UNKNOWN_MESSAGE)
            )
        }
    }

    fun updateAll() {
        try {
            updateMessage()
                ?.thenComposeAsync { updateButtons() }
                ?.thenComposeAsync { updateTopic() }
        } catch (e: InsufficientPermissionException) {
            logger.error(
                "I didn't have enough permissions to update the dedicated channel in {}",
                guild.name
            )
        }
    }

    fun updateButtons(): CompletableFuture<Message>? {
        if (!isChannelSet()) return null
        val msgRequest: RestAction<Message> = messageRequest ?: return null
        return msgRequest.submit()
            .thenCompose { msg: Message ->
                buttonUpdateRequest(
                    msg
                ).submit()
            }
    }

    fun buttonUpdateRequest(msg: Message): MessageEditAction {
        val localeManager = LocaleManager.getLocaleManager(msg.guild)
        val firstRow = ActionRow.of(
            RequestChannelButton.firstRow.stream()
                .filter { config.getState(it) }
                .map { field ->
                    Button.of(
                        ButtonStyle.PRIMARY,
                        field.id.toString(),
                        field.emoji
                    )
                }
                .toList()
        )
        val secondRow: ActionRow = ActionRow.of(
            RequestChannelButton.secondRow.stream()
                .filter { field: RequestChannelButton -> config.getState(field) }
                .map { field: RequestChannelButton ->
                    Button.of(
                        if ((field == RequestChannelButton.DISCONNECT)) ButtonStyle.DANGER else ButtonStyle.SECONDARY,
                        field.id.toString(),
                        field.emoji
                    )
                }
                .toList()
        )
        val thirdRow: ActionRow = ActionRow.of(
            StringSelectionMenuBuilder(
                _name = RequestChannelButton.FILTERS.id.toString(),
                placeholder = LocaleManager.getLocaleManager(msg.guild)
                    .getMessage(FilterMessages.FILTER_SELECT_PLACEHOLDER),
                range = Pair(0, 5),
                _options = listOf(
                    StringSelectMenuOption(
                        localeManager.getMessage(FilterMessages.EIGHT_D),
                        "${RequestChannelButton.FILTERS.id}:8d"
                    ),
                    StringSelectMenuOption(
                        localeManager.getMessage(FilterMessages.KARAOKE),
                        "${RequestChannelButton.FILTERS.id}:karaoke"
                    ),
                    StringSelectMenuOption(
                        localeManager.getMessage(FilterMessages.NIGHTCORE),
                        "${RequestChannelButton.FILTERS.id}:nightcore"
                    ),
                    StringSelectMenuOption(
                        localeManager.getMessage(FilterMessages.TREMOLO),
                        "${RequestChannelButton.FILTERS.id}:tremolo"
                    ),
                    StringSelectMenuOption(
                        localeManager.getMessage(FilterMessages.VIBRATO),
                        "${RequestChannelButton.FILTERS.id}:vibrato"
                    )
                )
            ).build()
        )
        return if (config.getState(RequestChannelButton.FILTERS)) msg.editMessageComponents(
            firstRow,
            secondRow,
            thirdRow
        ) else msg.editMessageComponents(firstRow, secondRow)
    }

    fun updateTopic(): CompletableFuture<Void?>? {
        if (!isChannelSet()) return null
        val channel: TextChannel? = textChannel
        return channelTopicUpdateRequest(channel)!!.submit()
    }

    @Synchronized
    fun channelTopicUpdateRequest(channel: TextChannel?): TextChannelManager? {
        if (channel == null) return null
        val localeManager = LocaleManager.getLocaleManager(channel.guild)
        return channel.manager.setTopic(
            (RobertifyEmoji.PREVIOUS_EMOJI.toString() + " " + localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_PREVIOUS) +
                    RobertifyEmoji.REWIND_EMOJI + " " + localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_REWIND) +
                    RobertifyEmoji.PLAY_AND_PAUSE_EMOJI + " " + localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_PLAY_AND_PAUSE) +
                    RobertifyEmoji.STOP_EMOJI + " " + localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_STOP) +
                    RobertifyEmoji.END_EMOJI + " " + localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_END) +
                    RobertifyEmoji.STAR_EMOJI + " " + localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_STAR) +
                    RobertifyEmoji.LOOP_EMOJI + " " + localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_LOOP) +
                    RobertifyEmoji.SHUFFLE_EMOJI + " " + localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_SHUFFLE) +
                    RobertifyEmoji.QUIT_EMOJI + " " + localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_QUIT))
        )
    }

    protected fun updateConfig(config: JSONObject) {
        cache.setField(guild.idLong, GuildDB.Field.REQUEST_CHANNEL_OBJECT, config)
    }

    fun cleanChannel() {
        if (!isChannelSet()) return
        if (!guild.selfMember.hasPermission(Permission.MESSAGE_HISTORY)) return
        val channel = textChannel!!
        MessageHistory.getHistoryAfter((channel), messageId.toString())
            .queue { messages: MessageHistory ->
                val validMessages: List<Message> =
                    messages.retrievedHistory
                        .stream()
                        .filter { msg: Message ->
                            msg.timeCreated.toEpochSecond() > ((Date()
                                .time / 1000).toInt()) - (14 * 24 * 60 * 60)
                        }
                        .toList()
                if (validMessages.size >= 2) channel.deleteMessages(
                    validMessages.subList(
                        0,
                        validMessages.size.coerceAtMost(100)
                    )
                ).queue()
            }
    }


    class ChannelConfig internal constructor(private val mainConfig: RequestChannelConfig) {
        fun getState(field: RequestChannelButton): Boolean {
            if (!hasField(field)) initConfig()
            val config: JSONObject = config
            return config.getBoolean(field.name.lowercase(Locale.getDefault()))
        }

        fun setState(field: RequestChannelButton, state: Boolean) {
            if (!hasField(field)) initConfig()
            val config: JSONObject = config
            config.put(field.name.lowercase(Locale.getDefault()), state)
            val fullConfig: JSONObject = fullConfig.put(GuildDB.Field.REQUEST_CHANNEL_CONFIG.toString(), config)
            mainConfig.updateConfig(fullConfig)
        }

        private fun initConfig() {
            val config: JSONObject = mainConfig.getGuildObject()
                .getJSONObject(GuildDB.Field.REQUEST_CHANNEL_OBJECT.toString())
            if (!config.has(GuildDB.Field.REQUEST_CHANNEL_CONFIG.toString())) config.put(
                GuildDB.Field.REQUEST_CHANNEL_CONFIG.toString(),
                JSONObject()
            )
            val configObj: JSONObject = config.getJSONObject(GuildDB.Field.REQUEST_CHANNEL_CONFIG.toString())
            for (field: RequestChannelButton in RequestChannelButton.values()) {
                if (!configObj.has(field.name.lowercase(Locale.getDefault()))) configObj.put(
                    field.name.lowercase(Locale.getDefault()),
                    field != RequestChannelButton.FILTERS
                )
            }
            mainConfig.updateConfig(config)
        }

        private fun hasField(field: RequestChannelButton): Boolean {
            return config.has(field.name.lowercase(Locale.getDefault()))
        }

        val config: JSONObject
            get() {
                var dedicatedChannelObj: JSONObject = fullConfig
                if (!dedicatedChannelObj.has(GuildDB.Field.REQUEST_CHANNEL_CONFIG.toString())) {
                    initConfig()
                    dedicatedChannelObj = fullConfig
                }
                return dedicatedChannelObj.getJSONObject(GuildDB.Field.REQUEST_CHANNEL_CONFIG.toString())
            }
        private val fullConfig: JSONObject
            get() = mainConfig.getGuildObject()
                .getJSONObject(GuildDB.Field.REQUEST_CHANNEL_OBJECT.toString())
    }


    override fun update() {
        // Nothing
    }
}