package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManagerKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.json.logs.LogTypeKt
import main.utils.json.logs.LogUtilsKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class PreviousTrackCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "previous",
        description = "Play the previous track.",
        isPremium = true
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyWithEmbed { handlePrevious(event.guild!!, event.member!!.voiceState!!) }
            .queue()
    }

    private fun handlePrevious(guild: Guild, memberVoiceState: GuildVoiceState): MessageEmbed {
        val musicManager = RobertifyAudioManagerKt.getMusicManager(guild)
        val scheduler = musicManager.scheduler
        val queueHandler = scheduler.queueHandler
        val player = musicManager.player
        val selfVoiceState = guild.selfMember.voiceState!!
        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState, songMustBePlaying = true)
        if (acChecks != null) return acChecks

        if (queueHandler.isPreviousTracksEmpty)
            return RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.PreviousTrackMessages.NO_PREV_TRACKS
            ).build()

        val currentTrack = player.playingTrack
        queueHandler.addToBeginning(currentTrack)
        player.stopTrack()
        player.playTrack(queueHandler.popPreviousTrack())

        RequestChannelConfigKt(guild).updateMessage()

        LogUtilsKt(guild).sendLog(
            LogTypeKt.TRACK_PREVIOUS,
            RobertifyLocaleMessageKt.PreviousTrackMessages.PREV_TRACK_LOG,
            Pair("{user}", memberVoiceState.member.asMention)
        )
        return RobertifyEmbedUtilsKt.embedMessage(
            guild,
            RobertifyLocaleMessageKt.PreviousTrackMessages.PLAYING_PREV_TRACK
        ).build()
    }

    override val help: String
        get() = """
                Go back to he track that was played previously

                **Usage**: `/previous`"""
}