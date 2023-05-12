package main.commands.slashcommands.management.requestchannel

import dev.minn.jda.ktx.util.SLF4J
import main.audiohandlers.RobertifyAudioManager
import main.audiohandlers.utils.author
import main.audiohandlers.utils.title
import main.commands.slashcommands.audio.*
import main.constants.RobertifyPermission
import main.events.AbstractEventController
import main.utils.GeneralUtils
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.json.logs.LogType
import main.utils.json.logs.LogUtilsKt
import main.utils.json.requestchannel.RequestChannelButtonId
import main.utils.json.requestchannel.RequestChannelConfig
import main.utils.json.toggles.TogglesConfig
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.LoopMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse

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

            if (!config.isChannelSet() || config.channelId != event.channel.idLong)
                return@onEvent

            config.removeChannel()
        }

    private val handleButtonClick =
        onEvent<ButtonInteractionEvent> { event ->
            if (!event.isFromGuild || event.button.id?.startsWith(RequestChannelButtonId.IDENTIFIER.toString()) != true)
                return@onEvent

            val guild = event.guild!!
            val config = RequestChannelConfig(guild)

            if (!config.isChannelSet() && config.channelId != event.channel.idLong)
                return@onEvent

            val id = event.button.id!!
            val selfVoiceState = guild.selfMember.voiceState!!
            val memberVoiceState = event.member!!.voiceState!!

            event.replyEmbed {
                when (id) {
                    RequestChannelButtonId.REWIND.toString() -> handleRewind(selfVoiceState, memberVoiceState)
                    RequestChannelButtonId.PLAY_AND_PAUSE.toString() -> handlePlayAndPause(
                        selfVoiceState,
                        memberVoiceState
                    )

                    RequestChannelButtonId.SKIP.toString() -> handleSkip(selfVoiceState, memberVoiceState)
                    RequestChannelButtonId.LOOP.toString() -> handleLoop(selfVoiceState, memberVoiceState)
                    RequestChannelButtonId.SHUFFLE.toString() -> handleShuffle(memberVoiceState)
                    RequestChannelButtonId.DISCONNECT.toString() -> handleDisconnect(selfVoiceState, memberVoiceState)
                    RequestChannelButtonId.STOP.toString() -> handleStop(memberVoiceState)
                    RequestChannelButtonId.PREVIOUS.toString() -> handlePrevious(selfVoiceState, memberVoiceState)
                    RequestChannelButtonId.FAVOURITE.toString() -> handleFavouriteTrack(memberVoiceState)
                    else -> throw IllegalArgumentException("Somehow received $id as an argument to parse")
                }
            }.queue(null) {
                ErrorHandler().ignore(ErrorResponse.UNKNOWN_INTERACTION)
            }
        }

    private fun handleRewind(
        selfVoiceState: GuildVoiceState,
        memberVoiceState: GuildVoiceState
    ): MessageEmbed =
        handleGenericCommand(
            command = RewindCommand(),
            memberVoiceState
        ) { handleRewind(memberVoiceState, selfVoiceState) }

    private fun handlePlayAndPause(
        selfVoiceState: GuildVoiceState,
        memberVoiceState: GuildVoiceState
    ): MessageEmbed =
        handleGenericCommand(
            command = PauseCommand(),
            memberVoiceState
        ) { handlePause(memberVoiceState, selfVoiceState) }

    private fun handleSkip(
        selfVoiceState: GuildVoiceState,
        memberVoiceState: GuildVoiceState
    ): MessageEmbed =
        handleGenericCommand(
            SkipCommand(),
            memberVoiceState
        ) { handleSkip(selfVoiceState, memberVoiceState) }

    private fun handleLoop(
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
                        Pair("{title}", playingTrack.title),
                        Pair("{author}", playingTrack.author)
                    )

                    RobertifyEmbedUtils.embedMessage(
                        guild,
                        LoopMessages.LOOP_START,
                        Pair("{title}", playingTrack.title)
                    )
                        .build()
                }
            }

        }

    private fun handleShuffle(
        memberVoiceState: GuildVoiceState
    ): MessageEmbed =
        handleGenericCommand(
            ShuffleCommand(),
            memberVoiceState
        ) { handleShuffle(memberVoiceState.guild, memberVoiceState.member.user) }

    private fun handleDisconnect(
        selfVoiceState: GuildVoiceState,
        memberVoiceState: GuildVoiceState
    ): MessageEmbed =
        handleGenericCommand(
            DisconnectCommand(),
            memberVoiceState
        ) { handleDisconnect(selfVoiceState, memberVoiceState) }

    private fun handleStop(
        memberVoiceState: GuildVoiceState
    ): MessageEmbed =
        handleGenericCommand(
            StopCommand(),
            memberVoiceState
        ) { handleStop(memberVoiceState.member) }

    private fun handlePrevious(
        selfVoiceState: GuildVoiceState,
        memberVoiceState: GuildVoiceState
    ): MessageEmbed =
        handleGenericCommand(
            PreviousTrackCommand(),
            memberVoiceState
        ) { handlePrevious(selfVoiceState, memberVoiceState) }

    private fun handleFavouriteTrack(
        memberVoiceState: GuildVoiceState
    ): MessageEmbed =
        handleGenericCommand(
            FavouriteTracksCommand(),
            memberVoiceState
        ) { handleAdd(memberVoiceState.guild, memberVoiceState.member) }


    private inline fun <T : AbstractSlashCommand> handleGenericCommand(
        command: T,
        memberVoiceState: GuildVoiceState,
        logic: T.() -> MessageEmbed
    ): MessageEmbed {
        val guild = memberVoiceState.guild
        if (!djCheck(command, guild, memberVoiceState.member))
            return RobertifyEmbedUtils.embedMessage(guild, GeneralMessages.DJ_BUTTON).build()
        return logic(command)
    }

    private fun djCheck(
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