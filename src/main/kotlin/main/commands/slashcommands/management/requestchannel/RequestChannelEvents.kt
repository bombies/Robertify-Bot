package main.commands.slashcommands.management.requestchannel

import dev.minn.jda.ktx.messages.reply_
import dev.minn.jda.ktx.util.SLF4J
import main.audiohandlers.RobertifyAudioManager
import main.audiohandlers.utils.author
import main.audiohandlers.utils.title
import main.commands.slashcommands.audio.*
import main.constants.RobertifyPermission
import main.events.AbstractEventController
import main.main.Config
import main.utils.GeneralUtils
import main.utils.GeneralUtils.queueAfter
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.json.logs.LogType
import main.utils.json.logs.LogUtilsKt
import main.utils.json.requestchannel.RequestChannelButtonId
import main.utils.json.requestchannel.RequestChannelConfig
import main.utils.json.toggles.TogglesConfig
import main.utils.locale.messages.DedicatedChannelMessages
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.LoopMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse
import kotlin.time.Duration.Companion.seconds

class RequestChannelEvents : AbstractEventController() {

    companion object {
        val logger by SLF4J
    }

    private val handleChannelDelete =
        onEvent<ChannelDeleteEvent> { event ->
            if (!event.isFromType(ChannelType.TEXT))
                return@onEvent

            val guild = event.guild
            val config = RequestChannelConfig(guild)

            if (!config.isChannelSet() || config.getChannelId() != event.channel.idLong)
                return@onEvent

            config.removeChannel()
        }

    private val handleMessageSend =
        onEvent<MessageReceivedEvent> { event ->
            if (!event.isFromGuild) return@onEvent

            val guild = event.guild
            val config = RequestChannelConfig(guild)

            if (!config.isChannelSet()) return@onEvent

            if (config.getChannelId() != event.channel.idLong) return@onEvent

            val selfVoiceState = guild.selfMember.voiceState!!
            val memberVoiceState = event.member!!.voiceState!!
            val user = event.author

            val msg = event.message
            val msgContent = msg.contentRaw

            when {
                !user.isBot && !event.isWebhookMessage -> {
                    if (!Config.MESSAGE_CONTENT_ENABLED)
                        return@onEvent run {
                            msg.reply_(
                                content = user.asMention,
                                embeds = listOf(
                                    RobertifyEmbedUtils.embedMessage(
                                        guild,
                                        DedicatedChannelMessages.DEDICATED_CHANNEL_NO_CONTENT_INTENT
                                    ).build()
                                )
                            ).queue()
                            msg.delete().queueAfter(10.seconds)
                        }

                    if (!memberVoiceState.inAudioChannel())
                        return@onEvent run {
                            msg.reply_(
                                content = user.asMention,
                                embeds = listOf(
                                    RobertifyEmbedUtils.embedMessage(
                                        guild,
                                        GeneralMessages.USER_VOICE_CHANNEL_NEEDED
                                    ).build()
                                )
                            ).queue()
                            msg.delete().queueAfter(10.seconds)
                        }


                    if (selfVoiceState.inAudioChannel()) {
                        if (memberVoiceState.channel!!.id != selfVoiceState.channel!!.id)
                            return@onEvent run {
                                msg.reply_(
                                    content = user.asMention,
                                    embeds = listOf(
                                        RobertifyEmbedUtils.embedMessage(
                                            guild,
                                            GeneralMessages.SAME_VOICE_CHANNEL_LOC,
                                            "{channel}" to selfVoiceState.channel!!.asMention
                                        ).build()
                                    )
                                ).queue()
                                msg.delete().queueAfter(10.seconds)
                            }
                    } else {
                        // Cant be bothered to implement this especially since it's not being used anymore.
                    }
                }
            }

            when {
                user.isBot -> {
                    if (!msg.isEphemeral)
                        msg.delete().queueAfter(10.seconds, null) {
                            ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE) {}
                        }
                }

                else -> {
                    msg.delete().queueAfter(2.seconds, null) {
                        ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE) {}
                    }
                }
            }
        }

    private val handleButtonClick =
        onEvent<ButtonInteractionEvent> { event ->
            if (!event.isFromGuild || event.button.id?.startsWith(RequestChannelButtonId.IDENTIFIER.toString()) != true)
                return@onEvent

            val guild = event.guild!!
            val config = RequestChannelConfig(guild)

            if (!config.isChannelSet() && config.getChannelId() != event.channel.idLong)
                return@onEvent

            val id = event.button.id!!
            val selfVoiceState = guild.selfMember.voiceState!!
            val memberVoiceState = event.member!!.voiceState!!

            event.reply_(
                content = event.user.asMention,
                embeds = listOf(
                    when (id) {
                        RequestChannelButtonId.REWIND.toString() -> handleRewind(selfVoiceState, memberVoiceState)
                        RequestChannelButtonId.PLAY_AND_PAUSE.toString() -> handlePlayAndPause(
                            selfVoiceState,
                            memberVoiceState
                        )

                        RequestChannelButtonId.SKIP.toString() -> handleSkip(selfVoiceState, memberVoiceState)
                        RequestChannelButtonId.LOOP.toString() -> handleLoop(selfVoiceState, memberVoiceState)
                        RequestChannelButtonId.SHUFFLE.toString() -> handleShuffle(memberVoiceState)
                        RequestChannelButtonId.DISCONNECT.toString() -> handleDisconnect(
                            selfVoiceState,
                            memberVoiceState
                        )

                        RequestChannelButtonId.STOP.toString() -> handleStop(memberVoiceState)
                        RequestChannelButtonId.PREVIOUS.toString() -> handlePrevious(selfVoiceState, memberVoiceState)
                        RequestChannelButtonId.FAVOURITE.toString() -> handleFavouriteTrack(memberVoiceState)
                        else -> throw IllegalArgumentException("Somehow received $id as an argument to parse")
                    }
                )
            ).queue(null) {
                ErrorHandler().ignore(ErrorResponse.UNKNOWN_INTERACTION)
            }
        }

    private suspend fun handleRewind(
        selfVoiceState: GuildVoiceState,
        memberVoiceState: GuildVoiceState
    ): MessageEmbed =
        handleGenericCommand(
            command = RewindCommand(),
            memberVoiceState
        ) { handleRewind(memberVoiceState, selfVoiceState) }

    private suspend fun handlePlayAndPause(
        selfVoiceState: GuildVoiceState,
        memberVoiceState: GuildVoiceState
    ): MessageEmbed =
        handleGenericCommand(
            command = PauseCommand(),
            memberVoiceState
        ) { handlePause(memberVoiceState, selfVoiceState) }

    private suspend fun handleSkip(
        selfVoiceState: GuildVoiceState,
        memberVoiceState: GuildVoiceState
    ): MessageEmbed =
        handleGenericCommand(
            SkipCommand(),
            memberVoiceState
        ) { handleSkip(selfVoiceState, memberVoiceState) }

    private suspend fun handleLoop(
        selfVoiceState: GuildVoiceState,
        memberVoiceState: GuildVoiceState
    ): MessageEmbed =
        handleGenericCommand(
            LoopCommand(),
            memberVoiceState
        ) {
            val guild = selfVoiceState.guild
            val musicManager = RobertifyAudioManager[guild]
            val scheduler = musicManager.scheduler
            val queueHandler = scheduler.queueHandler
            val playingTrack = musicManager.player.playingTrack
            val logUtils = LogUtilsKt(guild)
            val member = memberVoiceState.member

            return@handleGenericCommand when {
                queueHandler.trackRepeating -> {
                    queueHandler.trackRepeating = false
                    queueHandler.queueRepeating = true

                    logUtils.sendLog(
                        LogType.QUEUE_LOOP,
                        LoopMessages.QUEUE_LOOP_LOG,
                        Pair("{user}", member.asMention),
                        Pair("{status}", "looped")
                    )

                    RobertifyEmbedUtils.embedMessage(guild, LoopMessages.QUEUE_LOOP_START)
                        .build()
                }

                queueHandler.queueRepeating -> {
                    queueHandler.trackRepeating = false
                    queueHandler.queueRepeating = false

                    logUtils.sendLog(
                        LogType.QUEUE_LOOP,
                        LoopMessages.QUEUE_LOOP_LOG,
                        Pair("{user}", member.asMention),
                        Pair("{status}", "unlooped")
                    )

                    RobertifyEmbedUtils.embedMessage(guild, LoopMessages.QUEUE_LOOP_STOP)
                        .build()
                }

                else -> {
                    queueHandler.trackRepeating = true
                    queueHandler.queueRepeating = false

                    logUtils.sendLog(
                        LogType.QUEUE_LOOP,
                        LoopMessages.LOOP_LOG,
                        Pair("{user}", member.asMention),
                        Pair("{status}", "looped"),
                        Pair("{title}", playingTrack?.title ?: "Unknown"),
                        Pair("{author}", playingTrack?.author ?: "Unknown")
                    )

                    RobertifyEmbedUtils.embedMessage(
                        guild,
                        LoopMessages.LOOP_START,
                        Pair("{title}", playingTrack?.title ?: "Unknown")
                    )
                        .build()
                }
            }

        }

    private suspend fun handleShuffle(
        memberVoiceState: GuildVoiceState
    ): MessageEmbed =
        handleGenericCommand(
            ShuffleCommand(),
            memberVoiceState
        ) { handleShuffle(memberVoiceState.guild, memberVoiceState.member.user) }

    private suspend fun handleDisconnect(
        selfVoiceState: GuildVoiceState,
        memberVoiceState: GuildVoiceState
    ): MessageEmbed =
        handleGenericCommand(
            DisconnectCommand(),
            memberVoiceState
        ) { handleDisconnect(selfVoiceState, memberVoiceState) }

    private suspend fun handleStop(
        memberVoiceState: GuildVoiceState
    ): MessageEmbed =
        handleGenericCommand(
            StopCommand(),
            memberVoiceState
        ) { handleStop(memberVoiceState.member) }

    private suspend fun handlePrevious(
        selfVoiceState: GuildVoiceState,
        memberVoiceState: GuildVoiceState
    ): MessageEmbed =
        handleGenericCommand(
            PreviousTrackCommand(),
            memberVoiceState
        ) { handlePrevious(selfVoiceState, memberVoiceState) }

    private suspend fun handleFavouriteTrack(
        memberVoiceState: GuildVoiceState
    ): MessageEmbed =
        handleGenericCommand(
            FavouriteTracksCommand(),
            memberVoiceState
        ) { handleAdd(memberVoiceState.guild, memberVoiceState.member) }


    private suspend inline fun <T : AbstractSlashCommand> handleGenericCommand(
        command: T,
        memberVoiceState: GuildVoiceState,
        logic: T.() -> MessageEmbed
    ): MessageEmbed {
        val guild = memberVoiceState.guild
        if (!djCheck(command, guild, memberVoiceState.member))
            return RobertifyEmbedUtils.embedMessage(guild, GeneralMessages.DJ_BUTTON).build()
        return logic(command)
    }

    private suspend fun djCheck(
        command: AbstractSlashCommand,
        guild: Guild,
        member: Member
    ): Boolean {
        val toggles = TogglesConfig(guild)
        if (toggles.isDJToggleSet(command) && toggles.getDJToggle(command))
            return GeneralUtils.hasPerms(guild, member, RobertifyPermission.ROBERTIFY_DJ)
        return true
    }
}