package main.utils.json.requestchannel

import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import main.audiohandlers.RobertifyAudioManagerKt
import main.constants.RobertifyEmojiKt
import main.main.RobertifyKt
import main.utils.GeneralUtilsKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.component.interactions.selectionmenu.StringSelectMenuOptionKt
import main.utils.component.interactions.selectionmenu.StringSelectionMenuBuilderKt
import main.utils.database.mongodb.databases.GuildDBKt
import main.utils.json.AbstractGuildConfigKt
import main.utils.json.themes.ThemesConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageHistory
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.MessageEditAction
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class RequestChannelConfigKt(private val guild: Guild) : AbstractGuildConfigKt(guild) {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)

        fun updateAllButtons() {
            for (g: Guild in RobertifyKt.shardManager.guilds) {
                val config = RequestChannelConfigKt(g)
                if (!config.isChannelSet()) continue
                val msgRequest: RestAction<Message> = config.getMessageRequest() ?: continue
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
            for (g: Guild in RobertifyKt.shardManager.guilds) {
                val config = RequestChannelConfigKt(g)
                if (!config.isChannelSet()) continue
                val channel: TextChannel = config.getTextChannel() ?: continue
                config.channelTopicUpdateRequest(channel)?.queue()
            }
        }
    }

    @Synchronized
    fun setMessage(mid: Long) {
        val obj: JSONObject = getGuildObject()
        val dediChannelObj: JSONObject = obj.getJSONObject(GuildDBKt.Field.REQUEST_CHANNEL_OBJECT.toString())
        dediChannelObj.put(GuildDBKt.Field.REQUEST_CHANNEL_MESSAGE_ID.toString(), mid)
        cache.setField(guild.idLong, GuildDBKt.Field.REQUEST_CHANNEL_OBJECT, dediChannelObj)
    }

    @Synchronized
    fun setChannelAndMessage(cid: Long, mid: Long) {
        val obj: JSONObject = getGuildObject()
        val dediChannelObject: JSONObject = obj.getJSONObject(GuildDBKt.Field.REQUEST_CHANNEL_OBJECT.toString())
        dediChannelObject.put(GuildDBKt.Field.REQUEST_CHANNEL_ID.toString(), cid)
        dediChannelObject.put(GuildDBKt.Field.REQUEST_CHANNEL_MESSAGE_ID.toString(), mid)
        cache.setField(guild.idLong, GuildDBKt.Field.REQUEST_CHANNEL_OBJECT, dediChannelObject)
    }

    @Synchronized
    fun setChannel(cid: Long) {
        val obj: JSONObject = getGuildObject()
        val dediChannelObject: JSONObject = obj.getJSONObject(GuildDBKt.Field.REQUEST_CHANNEL_OBJECT.toString())
        dediChannelObject.put(GuildDBKt.Field.REQUEST_CHANNEL_ID.toString(), cid)
        cache.setField(guild.idLong, GuildDBKt.Field.REQUEST_CHANNEL_OBJECT, dediChannelObject)
    }

    @Synchronized
    fun setOriginalAnnouncementToggle(toggle: Boolean) {
        val obj: JSONObject = getGuildObject()
        val dedicatedChannelObj: JSONObject = obj.getJSONObject(GuildDBKt.Field.REQUEST_CHANNEL_OBJECT.toString())
        dedicatedChannelObj.put(RequestChannelConfigFieldKt.ORIGINAL_ANNOUNCEMENT_TOGGLE.toString(), toggle)
        cache.setField(guild.idLong, GuildDBKt.Field.REQUEST_CHANNEL_OBJECT, dedicatedChannelObj)
    }

    @Synchronized
    fun getOriginalAnnouncementToggle(): Boolean {
        return getGuildObject().getJSONObject(GuildDBKt.Field.REQUEST_CHANNEL_OBJECT.toString())
            .getBoolean(RequestChannelConfigFieldKt.ORIGINAL_ANNOUNCEMENT_TOGGLE.toString())
    }

    @Synchronized
    fun removeChannel() {
        if (!isChannelSet())
            throw IllegalArgumentException(
                "${RobertifyKt.shardManager.getGuildById(guild.idLong)?.name} (${guild.idLong}) doesn't have a request channel set."
            )
        val textChannel: TextChannel? = getTextChannel()
        textChannel?.delete()?.queue(null, ErrorHandler().ignore(ErrorResponse.MISSING_PERMISSIONS))

        val obj: JSONObject = getGuildObject().getJSONObject(GuildDBKt.Field.REQUEST_CHANNEL_OBJECT.toString())
        obj.put(GuildDBKt.Field.REQUEST_CHANNEL_ID.toString(), -1)
        obj.put(GuildDBKt.Field.REQUEST_CHANNEL_MESSAGE_ID.toString(), -1)
        cache.setField(guild.idLong, GuildDBKt.Field.REQUEST_CHANNEL_OBJECT, obj)
    }

    @Synchronized
    fun isChannelSet(): Boolean {
        val reqChannelObj: JSONObject =
            getGuildObject().getJSONObject(GuildDBKt.Field.REQUEST_CHANNEL_OBJECT.toString())
        return try {
            !reqChannelObj.isNull(GuildDBKt.Field.REQUEST_CHANNEL_ID.toString()) && reqChannelObj
                .getLong(GuildDBKt.Field.REQUEST_CHANNEL_ID.toString()) != -1L
        } catch (e: JSONException) {
            if (e.message!!.contains("is not a ")) {
                reqChannelObj.getString(GuildDBKt.Field.REQUEST_CHANNEL_ID.toString()) != "-1"
            } else throw e
        }
    }

    @Synchronized
    fun getChannelID(): Long {
        if (!isChannelSet()) throw IllegalArgumentException(
            RobertifyKt.shardManager.getGuildById(guild.idLong)
                ?.name + "(" + guild.idLong + ") doesn't have a channel set"
        )
        return getGuildObject().getJSONObject(GuildDBKt.Field.REQUEST_CHANNEL_OBJECT.toString())
            .getLong(GuildDBKt.Field.REQUEST_CHANNEL_ID.toString())
    }

    @Synchronized
    fun getMessageID(): Long {
        if (!isChannelSet()) throw IllegalArgumentException(
            RobertifyKt.shardManager.getGuildById(guild.idLong)
                ?.name + "(" + guild.idLong + ") doesn't have a channel set"
        )
        return getGuildObject().getJSONObject(GuildDBKt.Field.REQUEST_CHANNEL_OBJECT.toString())
            .getLong(GuildDBKt.Field.REQUEST_CHANNEL_MESSAGE_ID.toString())
    }

    @Synchronized
    fun getTextChannel(): TextChannel? {
        return RobertifyKt.shardManager.getTextChannelById(getChannelID())
    }

    fun getConfig(): ChannelConfig =
        ChannelConfig(this)

    @Synchronized
    fun getMessageRequest(): RestAction<Message>? {
        return try {
            getTextChannel()!!.retrieveMessageById(getMessageID())
        } catch (e: InsufficientPermissionException) {
            val channel: GuildMessageChannel? = RobertifyAudioManagerKt.ins
                .getMusicManager(guild)
                .scheduler
                .announcementChannel
            channel?.sendMessageEmbeds(
                RobertifyEmbedUtilsKt.embedMessage(
                    channel.guild,
                    "I don't have access to the requests channel anymore! I cannot update it."
                ).build()
            )?.queue(null, ErrorHandler().ignore(ErrorResponse.MISSING_PERMISSIONS))
            null
        }
    }

    @Synchronized
    fun updateMessage(): CompletableFuture<Void?>? {
        if (!isChannelSet()) return null
        return CompletableFuture.runAsync {
            val msgRequest: RestAction<Message> = getMessageRequest() ?: return@runAsync
            val musicManager = RobertifyAudioManagerKt.ins.getMusicManager(guild)
            val audioPlayer = musicManager.player
            val playingTrack: AudioTrack? = audioPlayer.playingTrack
            val queueHandler = musicManager.scheduler.queueHandler
            val queueAsList = ArrayList(queueHandler.contents)

            val theme = ThemesConfigKt(guild).theme
            val localeManager = LocaleManagerKt.getLocaleManager(guild)
            val eb = EmbedBuilder()

            if (playingTrack == null) {
                eb.setColor(theme.color)
                eb.setTitle(localeManager.getMessage(RobertifyLocaleMessageKt.DedicatedChannelMessages.DEDICATED_CHANNEL_NOTHING_PLAYING))
                eb.setImage(theme.idleBanner)
                val scheduler = musicManager.scheduler
                val announcementChannel: GuildMessageChannel? = scheduler.announcementChannel
                try {
                    msgRequest.queue(
                        { msg: Message ->
                            msg.editMessage(localeManager.getMessage(RobertifyLocaleMessageKt.DedicatedChannelMessages.DEDICATED_CHANNEL_QUEUE_NOTHING_PLAYING))
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
                val trackInfo: AudioTrackInfo = playingTrack.info
                eb.setColor(theme.color)
                eb.setTitle(
                    localeManager.getMessage(
                        RobertifyLocaleMessageKt.DedicatedChannelMessages.DEDICATED_CHANNEL_PLAYING_EMBED_TITLE,
                        Pair(
                            "{title}",
                            trackInfo.title
                        ),
                        Pair(
                            "{author}",
                            trackInfo.author
                        ),

                        Pair(
                            "{duration}",
                            GeneralUtilsKt.formatTime(playingTrack.info.length)
                        )
                    )
                )
                val requester: String = musicManager.scheduler.getRequesterAsMention(playingTrack)
                eb.setDescription(
                    localeManager.getMessage(
                        RobertifyLocaleMessageKt.NowPlayingMessages.NP_ANNOUNCEMENT_REQUESTER,
                        Pair("{requester}", requester)
                    )
                )
                if (playingTrack is MirroringAudioTrack) eb.setImage(playingTrack.artworkURL) else eb.setImage(
                    theme.nowPlayingBanner
                )
                eb.setFooter(
                    localeManager.getMessage(
                        RobertifyLocaleMessageKt.DedicatedChannelMessages.DEDICATED_CHANNEL_PLAYING_EMBED_FOOTER,
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
                    for (track: AudioTrack in queueAsList.subList(
                        0,
                        10
                    )) nextTenSongs.append(index++).append(". → ").append(track.info.title)
                        .append(" - ").append(track.info.author)
                        .append(" [").append(GeneralUtilsKt.formatTime(track.info.length))
                        .append("]\n")
                } else {
                    if (queueHandler.isEmpty) nextTenSongs.append(localeManager.getMessage(RobertifyLocaleMessageKt.DedicatedChannelMessages.DEDICATED_CHANNEL_QUEUE_NO_SONGS)) else {
                        var index = 1
                        for (track: AudioTrack in queueAsList) nextTenSongs.append(
                            index++
                        ).append(". → ").append(track.info.title).append(" - ").append(track.info.author)
                            .append(" [").append(GeneralUtilsKt.formatTime(track.info.length))
                            .append("]\n")
                    }
                }
                nextTenSongs.append("```")
                msgRequest.queue(
                    { msg: Message ->
                        msg.editMessage(
                            localeManager.getMessage(
                                RobertifyLocaleMessageKt.DedicatedChannelMessages.DEDICATED_CHANNEL_QUEUE_PLAYING,
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
            RobertifyLocaleMessageKt.DedicatedChannelMessages.DEDICATED_CHANNEL_SELF_INSUFFICIENT_PERMS_EDIT
        )
    }

    private fun sendErrorMessage(
        messageChannel: GuildMessageChannel?,
        message: RobertifyLocaleMessageKt.DedicatedChannelMessages
    ) {
        messageChannel?.sendMessageEmbeds(
            RobertifyEmbedUtilsKt.embedMessage(
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
        val msgRequest: RestAction<Message> = getMessageRequest() ?: return null
        return msgRequest.submit()
            .thenCompose { msg: Message ->
                buttonUpdateRequest(
                    msg
                ).submit()
            }
    }

    fun buttonUpdateRequest(msg: Message): MessageEditAction {
        val config = getConfig()
        val localeManager = LocaleManagerKt.getLocaleManager(msg.guild)
        val firstRow = ActionRow.of(
            RequestChannelButtonKt.firstRow.stream()
                .filter { config.getState(it) }
                .map { field ->
                    Button.of(
                        ButtonStyle.PRIMARY,
                        field.id,
                        field.emoji
                    )
                }
                .toList()
        )
        val secondRow: ActionRow = ActionRow.of(
            RequestChannelButtonKt.secondRow.stream()
                .filter { field: RequestChannelButtonKt -> config.getState(field) }
                .map { field: RequestChannelButtonKt ->
                    Button.of(
                        if ((field == RequestChannelButtonKt.DISCONNECT)) ButtonStyle.DANGER else ButtonStyle.SECONDARY,
                        field.id,
                        field.emoji
                    )
                }
                .toList()
        )
        val thirdRow: ActionRow = ActionRow.of(
            StringSelectionMenuBuilderKt(
                _name = RequestChannelButtonKt.FILTERS.id,
                placeholder = LocaleManagerKt.getLocaleManager(msg.guild)
                    .getMessage(RobertifyLocaleMessageKt.FilterMessages.FILTER_SELECT_PLACEHOLDER),
                range = Pair(0, 5),
                _options = listOf(
                    StringSelectMenuOptionKt(
                        localeManager.getMessage(RobertifyLocaleMessageKt.FilterMessages.EIGHT_D),
                        RequestChannelButtonKt.FILTERS.id + ":8d"
                    ),
                    StringSelectMenuOptionKt(
                        localeManager.getMessage(RobertifyLocaleMessageKt.FilterMessages.KARAOKE),
                        RequestChannelButtonKt.FILTERS.id + ":karaoke"
                    ),
                    StringSelectMenuOptionKt(
                        localeManager.getMessage(RobertifyLocaleMessageKt.FilterMessages.NIGHTCORE),
                        RequestChannelButtonKt.FILTERS.id + ":nightcore"
                    ),
                    StringSelectMenuOptionKt(
                        localeManager.getMessage(RobertifyLocaleMessageKt.FilterMessages.TREMOLO),
                        RequestChannelButtonKt.FILTERS.id + ":tremolo"
                    ),
                    StringSelectMenuOptionKt(
                        localeManager.getMessage(RobertifyLocaleMessageKt.FilterMessages.VIBRATO),
                        RequestChannelButtonKt.FILTERS.id + ":vibrato"
                    )
                )
            ).build()
        )
        return if (config.getState(RequestChannelButtonKt.FILTERS)) msg.editMessageComponents(
            firstRow,
            secondRow,
            thirdRow
        ) else msg.editMessageComponents(firstRow, secondRow)
    }

    fun updateTopic(): CompletableFuture<Void?>? {
        if (!isChannelSet()) return null
        val channel: TextChannel? = getTextChannel()
        return channelTopicUpdateRequest(channel)!!.submit()
    }

    @Synchronized
    fun channelTopicUpdateRequest(channel: TextChannel?): TextChannelManager? {
        if (channel == null) return null
        val localeManager = LocaleManagerKt.getLocaleManager(channel.guild)
        return channel.manager.setTopic(
            (RobertifyEmojiKt.PREVIOUS_EMOJI.toString() + " " + localeManager.getMessage(RobertifyLocaleMessageKt.DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_PREVIOUS) +
                    RobertifyEmojiKt.REWIND_EMOJI + " " + localeManager.getMessage(RobertifyLocaleMessageKt.DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_REWIND) +
                    RobertifyEmojiKt.PLAY_AND_PAUSE_EMOJI + " " + localeManager.getMessage(RobertifyLocaleMessageKt.DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_PLAY_AND_PAUSE) +
                    RobertifyEmojiKt.STOP_EMOJI + " " + localeManager.getMessage(RobertifyLocaleMessageKt.DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_STOP) +
                    RobertifyEmojiKt.END_EMOJI + " " + localeManager.getMessage(RobertifyLocaleMessageKt.DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_END) +
                    RobertifyEmojiKt.STAR_EMOJI + " " + localeManager.getMessage(RobertifyLocaleMessageKt.DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_STAR) +
                    RobertifyEmojiKt.LOOP_EMOJI + " " + localeManager.getMessage(RobertifyLocaleMessageKt.DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_LOOP) +
                    RobertifyEmojiKt.SHUFFLE_EMOJI + " " + localeManager.getMessage(RobertifyLocaleMessageKt.DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_SHUFFLE) +
                    RobertifyEmojiKt.QUIT_EMOJI + " " + localeManager.getMessage(RobertifyLocaleMessageKt.DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_QUIT))
        )
    }

    protected fun updateConfig(config: JSONObject) {
        cache.setField(guild.idLong, GuildDBKt.Field.REQUEST_CHANNEL_OBJECT, config)
    }

    fun cleanChannel() {
        if (!isChannelSet()) return
        if (!guild.selfMember.hasPermission(Permission.MESSAGE_HISTORY)) return
        val channel = getTextChannel()!!
        MessageHistory.getHistoryAfter((channel), getMessageID().toString())
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


    class ChannelConfig internal constructor(private val mainConfig: RequestChannelConfigKt) {
        fun getState(field: RequestChannelButtonKt): Boolean {
            if (!hasField(field)) initConfig()
            val config: JSONObject = config
            return config.getBoolean(field.name.lowercase(Locale.getDefault()))
        }

        fun setState(field: RequestChannelButtonKt, state: Boolean) {
            if (!hasField(field)) initConfig()
            val config: JSONObject = config
            config.put(field.name.lowercase(Locale.getDefault()), state)
            val fullConfig: JSONObject = fullConfig.put(GuildDBKt.Field.REQUEST_CHANNEL_CONFIG.toString(), config)
            mainConfig.updateConfig(fullConfig)
        }

        private fun initConfig() {
            val config: JSONObject = mainConfig.getGuildObject()
                .getJSONObject(GuildDBKt.Field.REQUEST_CHANNEL_OBJECT.toString())
            if (!config.has(GuildDBKt.Field.REQUEST_CHANNEL_CONFIG.toString())) config.put(
                GuildDBKt.Field.REQUEST_CHANNEL_CONFIG.toString(),
                JSONObject()
            )
            val configObj: JSONObject = config.getJSONObject(GuildDBKt.Field.REQUEST_CHANNEL_CONFIG.toString())
            for (field: RequestChannelButtonKt in RequestChannelButtonKt.values()) {
                if (!configObj.has(field.name.lowercase(Locale.getDefault()))) configObj.put(
                    field.name.lowercase(Locale.getDefault()),
                    field != RequestChannelButtonKt.FILTERS
                )
            }
            mainConfig.updateConfig(config)
        }

        private fun hasField(field: RequestChannelButtonKt): Boolean {
            return config.has(field.name.lowercase(Locale.getDefault()))
        }

        val config: JSONObject
            get() {
                var dedicatedChannelObj: JSONObject = fullConfig
                if (!dedicatedChannelObj.has(GuildDBKt.Field.REQUEST_CHANNEL_CONFIG.toString())) {
                    initConfig()
                    dedicatedChannelObj = fullConfig
                }
                return dedicatedChannelObj.getJSONObject(GuildDBKt.Field.REQUEST_CHANNEL_CONFIG.toString())
            }
        private val fullConfig: JSONObject
            get() = mainConfig.getGuildObject()
                .getJSONObject(GuildDBKt.Field.REQUEST_CHANNEL_OBJECT.toString())
    }


    override fun update() {
        // Nothing
    }
}