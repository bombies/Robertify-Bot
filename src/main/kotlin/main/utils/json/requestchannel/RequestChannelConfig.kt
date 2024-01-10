package main.utils.json.requestchannel

import dev.arbjerg.lavalink.protocol.v4.ifPresent
import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import main.audiohandlers.RobertifyAudioManager
import main.audiohandlers.utils.artworkUrl
import main.audiohandlers.utils.author
import main.audiohandlers.utils.length
import main.audiohandlers.utils.title
import main.constants.RobertifyEmoji
import main.main.Robertify
import main.utils.GeneralUtils
import main.utils.RobertifyEmbedUtils
import main.utils.component.interactions.selectionmenu.StringSelectMenuOption
import main.utils.component.interactions.selectionmenu.StringSelectionMenuBuilder
import main.utils.database.mongodb.cache.redis.guild.RequestChannelConfigModel
import main.utils.database.mongodb.cache.redis.guild.RequestChannelModel
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
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.MessageEditAction
import net.dv8tion.jda.api.sharding.ShardManager
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class RequestChannelConfig(private val guild: Guild, private val shardManager: ShardManager = Robertify.shardManager) :
    AbstractGuildConfig(guild) {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)

        fun updateAllButtons() {
            for (g: Guild in Robertify.shardManager.guilds) {
                val config = RequestChannelConfig(g)
                if (!config.isChannelSet()) continue
                config.getMessageRequest()?.queue { msg ->
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
                val channel: TextChannel = config.getTextChannel() ?: continue
                config.channelTopicUpdateRequest(channel)?.queue()
            }
        }
    }

    fun getMessageId(): Long {
        if (!isChannelSet()) throw IllegalArgumentException(
            shardManager.getGuildById(guild.idLong)
                ?.name + "(" + guild.idLong + ") doesn't have a channel set"
        )

        return getGuildModel().dedicated_channel!!.message_id ?: -1
    }

    fun setMessageId(id: Long) {
        cache.updateGuild(guild.id) {
            dedicated_channel {
                message_id = id
            }
        }
    }

    fun getChannelId(): Long {
        if (!isChannelSet()) throw IllegalArgumentException(
            shardManager.getGuildById(guild.idLong)
                ?.name + "(" + guild.idLong + ") doesn't have a channel set"
        )

        return getGuildModel().dedicated_channel!!.channel_id ?: -1
    }

    fun setChannelId(id: Long) {
        cache.updateGuild(guild.id) {
            dedicated_channel {
                channel_id = id
            }
        }
    }

    fun getOriginalAnnouncementToggle(): Boolean {
        return getGuildModel().dedicated_channel?.og_announcement_toggle ?: true
    }

    fun setOriginalAnnouncementToggle(value: Boolean) {
        cache.updateGuild(guild.id) {
            dedicated_channel {
                og_announcement_toggle = value
            }
        }
    }

    fun getTextChannel(): TextChannel? =
        shardManager.getTextChannelById(getChannelId())

    fun getMessageRequest(): RestAction<Message>? = try {
        getTextChannel()?.retrieveMessageById(getMessageId())
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

    val config: ChannelConfig
        get() = ChannelConfig(this)

    fun setChannelAndMessage(cid: Long, mid: Long) {
        cache.updateGuild(guild.id) {
            this.dedicated_channel = RequestChannelModel(
                mid,
                cid,
                this.dedicated_channel.config,
                this.dedicated_channel.og_announcement_toggle,
            )
        }
    }

    fun removeChannel() {
        if (!isChannelSet())
            throw IllegalArgumentException(
                "${shardManager.getGuildById(guild.idLong)?.name} (${guild.idLong}) doesn't have a request channel set."
            )
        try {
            getTextChannel()
                ?.delete()
                ?.queue(null, ErrorHandler().ignore(ErrorResponse.MISSING_PERMISSIONS))
        } catch (e: InsufficientPermissionException) {
            logger.warn("Could not delete Discord request channel in ${guild.name} (${guild.idLong})")
        }

        cache.updateGuild(guild.id) {
            dedicated_channel {
                channel_id = -1L
                message_id = -1L
            }
        }
    }

    fun isChannelSet(): Boolean {
        val obj = getGuildModel().dedicated_channel
        return obj.channel_id != -1L
    }

    fun isRequestChannel(channel: GuildMessageChannel): Boolean = when {
        !isChannelSet() -> false
        else -> getChannelId() == channel.idLong
    }

    fun updateMessage() {
        logger.debug("Channel set in ${guild.name} (${guild.idLong}): ${isChannelSet()}")
        if (!isChannelSet()) return

        val msgRequest: RestAction<Message> = getMessageRequest() ?: return
        val musicManager = RobertifyAudioManager[guild]

        musicManager.usePlayer { audioPlayer ->
            val playingTrack = audioPlayer?.track
            val queueHandler = musicManager.scheduler.queueHandler
            val queueAsList = ArrayList(queueHandler.contents)

            val theme = ThemesConfig(guild).getTheme()
            val localeManager = LocaleManager[guild]
            val eb = EmbedBuilder()

            if (playingTrack == null) {
                eb.setColor(theme.color)
                eb.setTitle(localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_NOTHING_PLAYING))
                eb.setImage(theme.idleBanner)
                val scheduler = musicManager.scheduler
                val announcementChannel: GuildMessageChannel? = scheduler.announcementChannel
                try {
                    msgRequest.queue { msg ->
                        msg.editMessage(localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_QUEUE_NOTHING_PLAYING))
                            .setEmbeds(eb.build())
                            .queue()
                    }
                } catch (e: InsufficientPermissionException) {
                    if (e.message!!.contains("MESSAGE_SEND")) sendEditErrorMessage(announcementChannel)
                } catch (e: ErrorResponseException) {
                    when (e.errorResponse) {
                        ErrorResponse.MISSING_PERMISSIONS -> sendEditErrorMessage(announcementChannel)
                        ErrorResponse.UNKNOWN_MESSAGE -> removeChannel()
                        else -> {}
                    }
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

                eb.setImage(playingTrack.artworkUrl ?: theme.nowPlayingBanner)
                eb.setFooter(
                    localeManager.getMessage(
                        DedicatedChannelMessages.DEDICATED_CHANNEL_PLAYING_EMBED_FOOTER,
                        Pair(
                            "{numSongs}",
                            queueAsList.size.toString()
                        ),
                        Pair(
                            "{volume}",
                            ((audioPlayer.filters.volume.ifPresent { it.times(100) } ?: 100).toInt()).toString()
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
                    )) nextTenSongs.append(index++).append(". → ").append(track.info.title)
                        .append(" - ").append(track.info.author)
                        .append(" [").append(GeneralUtils.formatTime(track.length))
                        .append("]\n")
                } else {
                    if (queueHandler.isEmpty) nextTenSongs.append(localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_QUEUE_NO_SONGS)) else {
                        var index = 1
                        for (track in queueAsList) nextTenSongs.append(
                            index++
                        ).append(". → ").append(track.info.title).append(" - ").append(track.info.author)
                            .append(" [").append(GeneralUtils.formatTime(track.length))
                            .append("]\n")
                    }
                }
                nextTenSongs.append("```")
                msgRequest.queue { msg ->
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
                }

                // TODO: Handle unknown message error properly
//                msgRequest.queue(
//                    { msg: Message ->
//
//                    },
//                    ErrorHandler()
//                        .handle(
//                            ErrorResponse.UNKNOWN_MESSAGE
//                        ) { removeChannel() }
//                )

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
        if (!isChannelSet())
            return

        try {
            updateMessage()
            val buttonUpdate = updateButtons()
            val topicUpdate = updateTopic()

            if (buttonUpdate == null || topicUpdate == null) {
                logger.error(
                    "Could not update request channel in {} because one or more of the updates resulted in a null job!",
                    guild.name
                )
            } else {
                CompletableFuture.allOf(
                    buttonUpdate,
                    topicUpdate
                )
            }
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
            .thenApply { msg ->
                buttonUpdateRequest(msg).submit()
            }.join()
    }

    fun buttonUpdateRequest(msg: Message): MessageEditAction {
        val localeManager = LocaleManager.getLocaleManager(msg.guild)

        val validFirstRowItems = RequestChannelButton.firstRow.filter { config.getState(it) }
        val firstRow: ActionRow? = if (validFirstRowItems.isNotEmpty())
            ActionRow.of(
                validFirstRowItems
                    .map { field ->
                        Button.of(
                            ButtonStyle.PRIMARY,
                            field.id.toString(),
                            field.emoji
                        )
                    }
                    .toList()
            ) else null

        val validSecondRowItems = RequestChannelButton.secondRow.filter { config.getState(it) }
        val secondRow: ActionRow? = if (validSecondRowItems.isNotEmpty())
            ActionRow.of(
                validSecondRowItems
                    .map { field: RequestChannelButton ->
                        Button.of(
                            if ((field == RequestChannelButton.DISCONNECT)) ButtonStyle.DANGER else ButtonStyle.SECONDARY,
                            field.id.toString(),
                            field.emoji
                        )
                    }
                    .toList()
            ) else null

        val thirdRow: ActionRow? = if (config.getState(RequestChannelButton.FILTERS))
            ActionRow.of(
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
            ) else null

        return msg.editMessageComponents(
            listOfNotNull(firstRow, secondRow, thirdRow)
        )
    }

    fun updateTopic(): CompletableFuture<Void>? {
        if (!isChannelSet()) return null
        val channel: TextChannel? = getTextChannel()
        return channelTopicUpdateRequest(channel)!!.submit()
    }

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
        cache.updateGuild(guild.id) {
            this.dedicated_channel = Json.decodeFromString(config.toString())
        }
    }

    fun cleanChannel() {
        if (!isChannelSet()) return
        if (!guild.selfMember.hasPermission(Permission.MESSAGE_HISTORY)) return
        val channel = getTextChannel()!!
        MessageHistory.getHistoryAfter((channel), getMessageId().toString())
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

        fun getConfigJsonObject(): JSONObject {
            val guildModel = mainConfig.getGuildModel()
            return guildModel.dedicated_channel.config?.toJsonObject() ?: RequestChannelConfigModel().toJsonObject()
        }

        fun getState(field: RequestChannelButton): Boolean {
            if (!hasField(field)) initConfig()
            val config: JSONObject = mainConfig
                .getGuildModel()
                .dedicated_channel
                .config!!
                .toJsonObject()
            return config.getBoolean(field.name.lowercase(Locale.getDefault()))
        }

        fun setState(field: RequestChannelButton, state: Boolean) {
            if (!hasField(field)) initConfig()

            val guildModel = mainConfig.getGuildModel()
            val config = guildModel.dedicated_channel.config!!.toJsonObject()
            config.put(field.name.lowercase(Locale.getDefault()), state)

            val fullConfig: JSONObject = guildModel
                .toJsonObject()
                .getJSONObject(GuildDB.Field.REQUEST_CHANNEL_OBJECT.toString())
                .put(GuildDB.Field.REQUEST_CHANNEL_CONFIG.toString(), config)
            mainConfig.updateConfig(fullConfig)
        }

        fun toggleStates(buttons: List<RequestChannelButton>) {
            val guildModel = mainConfig.getGuildModel()
            val config = guildModel.dedicated_channel.config!!
            cache.updateGuild(guildModel.server_id.toString()) {
                dedicated_channel {
                    config {
                        if (buttons.contains(RequestChannelButton.FILTERS)) {
                            filters = !config.filters
                        }

                        if (buttons.contains(RequestChannelButton.DISCONNECT)) {
                            disconnect = !config.disconnect
                        }

                        if (buttons.contains(RequestChannelButton.SKIP)) {
                            skip = !config.skip
                        }

                        if (buttons.contains(RequestChannelButton.PREVIOUS)) {
                            previous = !config.previous
                        }

                        if (buttons.contains(RequestChannelButton.REWIND)) {
                            rewind = !config.rewind
                        }

                        if (buttons.contains(RequestChannelButton.PLAY_PAUSE)) {
                            play_pause = !config.play_pause
                        }

                        if (buttons.contains(RequestChannelButton.STOP)) {
                            stop = !config.stop
                        }

                        if (buttons.contains(RequestChannelButton.LOOP)) {
                            loop = !config.loop
                        }

                        if (buttons.contains(RequestChannelButton.SHUFFLE)) {
                            shuffle = !config.shuffle
                        }

                        if (buttons.contains(RequestChannelButton.DISCONNECT)) {
                            disconnect = !config.disconnect
                        }

                        if (buttons.contains(RequestChannelButton.FAVOURITE)) {
                            favourite = !config.favourite
                        }

                        if (buttons.contains(RequestChannelButton.FILTERS)) {
                            filters = !config.filters
                        }
                    }
                }
            }
        }

        private fun initConfig() {
            val config: JSONObject = mainConfig.getGuildModel().toJsonObject()
                .getJSONObject(GuildDB.Field.REQUEST_CHANNEL_OBJECT.toString())
            if (!config.has(GuildDB.Field.REQUEST_CHANNEL_CONFIG.toString())) config.put(
                GuildDB.Field.REQUEST_CHANNEL_CONFIG.toString(),
                JSONObject()
            )
            val configObj: JSONObject = config.getJSONObject(GuildDB.Field.REQUEST_CHANNEL_CONFIG.toString())
            for (field: RequestChannelButton in RequestChannelButton.entries) {
                if (!configObj.has(field.name.lowercase(Locale.getDefault()))) configObj.put(
                    field.name.lowercase(Locale.getDefault()),
                    field != RequestChannelButton.FILTERS
                )
            }
            mainConfig.updateConfig(config)
        }

        private fun hasField(field: RequestChannelButton): Boolean {
            val guildModel = mainConfig.getGuildModel()
            val config = guildModel.dedicated_channel.config?.toJsonObject()
            return config?.has(field.name.lowercase(Locale.getDefault())) ?: false
        }
    }


    override fun update() {
        // Nothing
    }
}