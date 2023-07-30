package main.commands.slashcommands.audio

import dev.schlaubi.lavakord.audio.player.applyFilters
import main.audiohandlers.RobertifyAudioManager
import main.commands.slashcommands.SlashCommandManager.getRequiredOption
import main.utils.GeneralUtils.isNotNull
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.json.logs.LogType
import main.utils.json.logs.LogUtilsKt
import main.utils.json.requestchannel.RequestChannelConfig
import main.utils.locale.messages.VolumeMessages
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

class VolumeCommand : AbstractSlashCommand(
    SlashCommand(
        name = "volume",
        description = "Adjust the global volume of the bot",
        options = listOf(
            CommandOption(
                type = OptionType.INTEGER,
                name = "volume",
                description = "The volume to set the player to.",
                range = Pair(0, 200)
            )
        ),
        isPremium = true
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbed {
            handleVolumeChange(
                memberVoiceState = event.member!!.voiceState!!,
                selfVoiceState = event.guild!!.selfMember.voiceState!!,
                volume = event.getRequiredOption("volume").asInt
            )
        }.queue()
    }

    private suspend fun handleVolumeChange(
        selfVoiceState: GuildVoiceState,
        memberVoiceState: GuildVoiceState,
        volume: Int
    ): MessageEmbed {
        val guild = selfVoiceState.guild
        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState, songMustBePlaying = true)
        if (acChecks.isNotNull()) return acChecks!!

        if (volume < 0 || volume > 200)
            return RobertifyEmbedUtils.embedMessage(guild, VolumeMessages.INVALID_VOLUME)
                .build()

        val player = RobertifyAudioManager[guild].player
        player.applyFilters {
            this.volume = volume / 100F
        }

        RequestChannelConfig(guild).updateMessage()

        LogUtilsKt(guild).sendLog(
            LogType.VOLUME_CHANGE,
            VolumeMessages.VOLUME_CHANGED_LOG,
            Pair("{user}", memberVoiceState.member.asMention),
            Pair("{volume}", volume.toString())
        )

        return RobertifyEmbedUtils.embedMessage(
            guild,
            VolumeMessages.VOLUME_CHANGED,
            Pair("{volume}", volume.toString())
        ).build()
    }

    override val help: String
        get() = """
                Control the volume of the bot

                **__Usages__**
                `/volume <0-100>`"""
}