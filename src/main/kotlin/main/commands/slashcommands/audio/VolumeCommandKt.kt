package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManagerKt
import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.utils.GeneralUtilsKt.isNotNull
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.json.logs.LogTypeKt
import main.utils.json.logs.LogUtilsKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

class VolumeCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "volume",
        description = "Adjust the global volume of the bot",
        options = listOf(
            CommandOptionKt(
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
        event.replyWithEmbed {
            handleVolumeChange(
                memberVoiceState = event.member!!.voiceState!!,
                selfVoiceState = event.guild!!.selfMember.voiceState!!,
                volume = event.getRequiredOption("volume").asInt
            )
        }.queue()
    }

    private fun handleVolumeChange(
        selfVoiceState: GuildVoiceState,
        memberVoiceState: GuildVoiceState,
        volume: Int
    ): MessageEmbed {
        val guild = selfVoiceState.guild
        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState, songMustBePlaying = true)
        if (acChecks.isNotNull()) return acChecks!!

        if (volume < 0 || volume > 200)
            return RobertifyEmbedUtilsKt.embedMessage(guild, RobertifyLocaleMessageKt.VolumeMessages.INVALID_VOLUME)
                .build()

        val player = RobertifyAudioManagerKt.getMusicManager(guild).player
        player.filters.setVolume(volume / 100.0.toFloat()).commit()

        RequestChannelConfigKt(guild).updateMessage()

        LogUtilsKt(guild).sendLog(
            LogTypeKt.VOLUME_CHANGE,
            RobertifyLocaleMessageKt.VolumeMessages.VOLUME_CHANGED_LOG,
            Pair("{user}", memberVoiceState.member.asMention),
            Pair("{volume}", volume.toString())
        )

        return RobertifyEmbedUtilsKt.embedMessage(
            guild,
            RobertifyLocaleMessageKt.VolumeMessages.VOLUME_CHANGED,
            Pair("{volume}", volume.toString())
        ).build()
    }

    override val help: String
        get() = """
                Control the volume of the bot

                **__Usages__**
                `/volume <0-100>`"""
}