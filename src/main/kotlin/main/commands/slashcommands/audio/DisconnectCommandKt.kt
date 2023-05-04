package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManagerKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.json.logs.LogTypeKt
import main.utils.json.logs.LogUtilsKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.DisconnectMessages
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class DisconnectCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "disconnect",
        description = "Disconnect the bot from the voice channel it's currently in",
    )
) {
    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyWithEmbed {
            handleDisconnect(event.guild!!.selfMember.voiceState!!, event.member!!.voiceState!!)
        }.queue()
    }

    fun handleDisconnect(selfVoiceState: GuildVoiceState, memberVoiceState: GuildVoiceState): MessageEmbed {
        val guild = selfVoiceState.guild
        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState, selfChannelNeeded = true)
        if (acChecks != null) return acChecks

        RobertifyAudioManagerKt
            .getMusicManager(guild)
            .leave()

        LogUtilsKt(guild)
            .sendLog(
                LogTypeKt.BOT_DISCONNECTED,
                "${memberVoiceState.member.asMention} ${
                    LocaleManagerKt.getLocaleManager(guild)
                        .getMessage(DisconnectMessages.DISCONNECTED_USER)
                }"
            )

        return RobertifyEmbedUtilsKt.embedMessage(guild, DisconnectMessages.DISCONNECTED)
            .build()
    }

    override val help: String
        get() = "Forces the bot to stop playing music and leave the voice channel" +
                " if already in one."

}