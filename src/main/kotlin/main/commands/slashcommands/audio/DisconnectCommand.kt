package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManager
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.json.logs.LogType
import main.utils.json.logs.LogUtilsKt
import main.utils.locale.LocaleManager
import main.utils.locale.messages.DisconnectMessages
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class DisconnectCommand : AbstractSlashCommand(
    SlashCommand(
        name = "disconnect",
        description = "Disconnect the bot from the voice channel it's currently in",
    )
) {
    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbed {
            handleDisconnect(event.guild!!.selfMember.voiceState!!, event.member!!.voiceState!!)
        }.queue()
    }

    suspend fun handleDisconnect(selfVoiceState: GuildVoiceState, memberVoiceState: GuildVoiceState): MessageEmbed {
        val guild = selfVoiceState.guild
        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState, selfChannelNeeded = true)
        if (acChecks != null) return acChecks

        RobertifyAudioManager
            .getMusicManager(guild)
            .leave()

        LogUtilsKt(guild)
            .sendLog(
                LogType.BOT_DISCONNECTED,
                "${memberVoiceState.member.asMention} ${
                    LocaleManager[guild]
                        .getMessage(DisconnectMessages.DISCONNECTED_USER)
                }"
            )

        return RobertifyEmbedUtils.embedMessage(guild, DisconnectMessages.DISCONNECTED)
            .build()
    }

    override val help: String
        get() = "Forces the bot to stop playing music and leave the voice channel" +
                " if already in one."

}