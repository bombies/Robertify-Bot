package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManager
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.json.logs.LogType
import main.utils.json.logs.LogUtilsKt
import main.utils.json.requestchannel.RequestChannelConfig
import main.utils.locale.messages.PreviousTrackMessages
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class PreviousTrackCommand : AbstractSlashCommand(
    SlashCommand(
        name = "previous",
        description = "Play the previous track.",
        isPremium = true
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbed { handlePrevious(event.guild!!.selfMember.voiceState!!, event.member!!.voiceState!!) }
            .queue()
    }

    suspend fun handlePrevious(selfVoiceState: GuildVoiceState, memberVoiceState: GuildVoiceState): MessageEmbed {
        val guild = selfVoiceState.guild
        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState, songMustBePlaying = true)
        if (acChecks != null) return acChecks

        val musicManager = RobertifyAudioManager[guild]
        val scheduler = musicManager.scheduler
        val queueHandler = scheduler.queueHandler
        val player = musicManager.player

        if (queueHandler.isPreviousTracksEmpty)
            return RobertifyEmbedUtils.embedMessage(
                guild,
                PreviousTrackMessages.NO_PREV_TRACKS
            ).build()

        val currentTrack = player.playingTrack!!
        queueHandler.addToBeginning(currentTrack)
        player.stopTrack()
        player.playTrack(queueHandler.popPreviousTrack()!!)

        RequestChannelConfig(guild).updateMessage()

        LogUtilsKt(guild).sendLog(
            LogType.TRACK_PREVIOUS,
            PreviousTrackMessages.PREV_TRACK_LOG,
            Pair("{user}", memberVoiceState.member.asMention)
        )
        return RobertifyEmbedUtils.embedMessage(
            guild,
            PreviousTrackMessages.PLAYING_PREV_TRACK
        ).build()
    }

    override val help: String
        get() = """
                Go back to he track that was played previously

                **Usage**: `/previous`"""
}