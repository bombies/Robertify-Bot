package main.utils.json.requestchannel

import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
import java.util.concurrent.TimeUnit

class RequestChannelConfig(private val guild: Guild, private val shardManager: ShardManager = Robertify.shardManager) :
    AbstractGuildConfig(guild) {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)

        suspend fun updateAllButtons() {
            for (g: Guild in Robertify.shardManager.guilds) {
                val config = RequestChannelConfig(g)
                if (!config.isChannelSet()) continue
                val msg: Message = config.getMessageRequest()?.await() ?: continue
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

        suspend fun updateAllTopics() {
            for (g: Guild in Robertify.shardManager.guilds) {
                val config = RequestChannelConfig(g)
                if (!config.isChannelSet()) continue
                val channel: TextChannel = config.getTextChannel() ?: continue
                config.channelTopicUpdateRequest(channel)?.queue()
            }
        }
    }

    suspend fun getMessageId(): Long {
        if (!isChannelSet()) throw IllegalArgumentException(
            shardManager.getGuildById(guild.idLong)
                ?.name + "(" + guild.idLong + ") doesn't have a channel set"
        )

        return getGuildModel().dedicated_channel!!.message_id ?: -1
    }

    suspend fun setMessageId(id: Long) {
        cache.updateGuild(guild.id) {
            dedicated_channel {
                message_id = id
            }
        }
    }

    suspend fun getChannelId(): Long {
        if (!isChannelSet()) throw IllegalArgumentException(
            shardManager.getGuildById(guild.idLong)
                ?.name + "(" + guild.idLong + ") doesn't have a channel set"
        )

        return getGuildModel().dedicated_channel!!.channel_id ?: -1
    }

    suspend fun setChannelId(id: Long) {
        cache.updateGuild(guild.id) {
            dedicated_channel {
                channel_id = id
            }
        }
    }

    suspend fun getOriginalAnnouncementToggle(): Boolean {
        return getGuildModel().dedicated_channel?.og_announcement_toggle ?: true
    }

    suspend fun setOriginalAnnouncementToggle(value: Boolean) {
        cache.updateGuild(guild.id) {
            dedicated_channel {
                og_announcement_toggle = value
            }
        }
    }

    suspend fun getTextChannel(): TextChannel? =
        shardManager.getTextChannelById(getChannelId())

    suspend fun getMessageRequest(): RestAction<Message>? = try {
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

    suspend fun setChannelAndMessage(cid: Long, mid: Long) {
        cache.updateGuild(guild.id) {
            this.dedicated_channel = RequestChannelModel(
                mid,
                cid,
                this.dedicated_channel!!.config,
                this.dedicated_channel!!.og_announcement_toggle,
            )
        }
    }

    suspend fun removeChannel() {
        if (!isChannelSet())
            throw IllegalArgumentException(
                "${shardManager.getGuildById(guild.idLong)?.name} (${guild.idLong}) doesn't have a request channel set."
            )

        getTextChannel()
            ?.delete()
            ?.queue(null, ErrorHandler().ignore(ErrorResponse.MISSING_PERMISSIONS))

        cache.updateGuild(guild.id) {
            dedicated_channel {
                channel_id = -1L
                message_id = -1L
            }
        }
    }

    suspend fun isChannelSet(): Boolean {
        val obj = getGuildModel().dedicated_channel
        return obj != null && obj.channel_id != -1L
    }

    suspend fun isRequestChannel(channel: GuildMessageChannel): Boolean = when {
        !isChannelSet() -> false
        else -> getChannelId() == channel.idLong
    }

    suspend fun updateMessage(): Deferred<Unit>? = coroutineScope {
        logger.debug("Channel set in ${guild.name} (${guild.idLong}): ${isChannelSet()}")
        if (!isChannelSet()) return@coroutineScope null

        val job = async {
            val msgRequest: RestAction<Message> = getMessageRequest() ?: return@async
            val musicManager = RobertifyAudioManager[guild]
            val audioPlayer = musicManager.player
            val playingTrack = audioPlayer.playingTrack
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
                    val msg = msgRequest.await()
                    msg.editMessage(localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_QUEUE_NOTHING_PLAYING))
                        .setEmbeds(eb.build())
                        .await()
                    // TODO: Handle unknown message error properly
//                    msgRequest.queue(
//                        { msg: Message ->
//
//                        },
//                        ErrorHandler()
//                            .handle(
//                                ErrorResponse.UNKNOWN_MESSAGE
//                            ) { removeChannel() }
//                            .handle(
//                                ErrorResponse.MISSING_PERMISSIONS
//                            ) {
//                                sendEditErrorMessage(
//                                    announcementChannel
//                                )
//                            }
//                    )
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
                            ((audioPlayer.filters.volume?.times(100) ?: 100).toInt()).toString()
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
                val msg = msgRequest.await()
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


        return@coroutineScope job
    }

    private suspend fun sendEditErrorMessage(messageChannel: GuildMessageChannel?) {
        sendErrorMessage(
            messageChannel,
            DedicatedChannelMessages.DEDICATED_CHANNEL_SELF_INSUFFICIENT_PERMS_EDIT
        )
    }

    private suspend fun sendErrorMessage(
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

    suspend fun updateAll() {
        try {
            val messageUpdate = updateMessage()
            val buttonUpdate = updateButtons()
            val topicUpdate = updateTopic()

            if (messageUpdate == null || buttonUpdate == null || topicUpdate == null) {
                logger.error(
                    "Could not update request channel in {} because one or more of the updates resulted in a null job!",
                    guild.name
                )
            } else {
                awaitAll(
                    messageUpdate,
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

    suspend fun updateButtons(): Deferred<Message?>? = coroutineScope {
        if (!isChannelSet()) return@coroutineScope null
        val job = async {
            val msgRequest: RestAction<Message> = getMessageRequest() ?: return@async null
            msgRequest.await()
        }

        return@coroutineScope job
    }

    suspend fun buttonUpdateRequest(msg: Message): MessageEditAction {
        val localeManager = LocaleManager.getLocaleManager(msg.guild)
        val firstRow = ActionRow.of(
            RequestChannelButton.firstRow
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
            RequestChannelButton.secondRow
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

    suspend fun updateTopic(): Deferred<Void>? = coroutineScope {
        if (!isChannelSet()) return@coroutineScope null
        return@coroutineScope async {
            val channel: TextChannel? = getTextChannel()
            channelTopicUpdateRequest(channel)!!.await()
        }
    }

    suspend fun channelTopicUpdateRequest(channel: TextChannel?): TextChannelManager? {
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

    protected suspend fun updateConfig(config: JSONObject) {
        cache.updateGuild(guild.id) {
            this.dedicated_channel = Json.decodeFromString(config.toString())
        }
    }

    suspend fun cleanChannel() {
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

        suspend fun getConfigJsonObject(): JSONObject {
            val guildModel = mainConfig.getGuildModel()
            return guildModel.dedicated_channel!!.config!!.toJsonObject()
        }

        suspend fun getState(field: RequestChannelButton): Boolean {
            if (!hasField(field)) initConfig()
            val config: JSONObject = mainConfig
                .getGuildModel()
                .dedicated_channel!!
                .config!!
                .toJsonObject()
            return config.getBoolean(field.name.lowercase(Locale.getDefault()))
        }

        suspend fun setState(field: RequestChannelButton, state: Boolean) {
            if (!hasField(field)) initConfig()

            val guildModel = mainConfig.getGuildModel()
            val config = guildModel.dedicated_channel!!.config!!.toJsonObject()
            config.put(field.name.lowercase(Locale.getDefault()), state)

            val fullConfig: JSONObject = guildModel
                .toJsonObject()
                .getJSONObject(GuildDB.Field.REQUEST_CHANNEL_OBJECT.toString())
                .put(GuildDB.Field.REQUEST_CHANNEL_CONFIG.toString(), config)
            mainConfig.updateConfig(fullConfig)
        }

        private suspend fun initConfig() {
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

        private suspend fun hasField(field: RequestChannelButton): Boolean {
            val guildModel = mainConfig.getGuildModel()
            val config = guildModel.dedicated_channel?.config?.toJsonObject()
            return config?.has(field.name.lowercase(Locale.getDefault())) ?: false
        }
    }


    override suspend fun update() {
        // Nothing
    }
}