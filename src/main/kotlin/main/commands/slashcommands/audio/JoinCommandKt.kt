package main.commands.slashcommands.audio

import dev.minn.jda.ktx.util.SLF4J
import main.audiohandlers.RobertifyAudioManagerKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.editEmbed
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.RobertifyEmbedUtilsKt.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.JoinMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException

class JoinCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "join",
        description = "Force the bot to join the audio channel you're currently in"
    )
) {

    companion object {
        private val logger by SLF4J
    }

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        handleJoin(event)
    }

    private fun handleJoin(
        event: SlashCommandInteractionEvent
    ) {
        val guild = event.guild!!
        val memberVoiceState = event.member!!.voiceState!!
        val selfVoiceState = guild.selfMember.voiceState!!

        if (!memberVoiceState.inAudioChannel())
            return event.replyEmbed(guild, GeneralMessages.USER_VOICE_CHANNEL_NEEDED)
                .setEphemeral(true)
                .queue()

        val channel = memberVoiceState.channel!!
        val musicManager = RobertifyAudioManagerKt[guild]
        val placeholderPair = Pair("{channel}", channel.asMention)

        if (selfVoiceState.inAudioChannel() && memberVoiceState.channel!!.id == selfVoiceState.channel!!.id)
            return event.replyEmbed(
                guild,
                JoinMessages.ALREADY_JOINED,
                placeholderPair
            ).setEphemeral(true)
                .queue()

        event.replyEmbed(guild, JoinMessages.ATTEMPTING_TO_JOIN, placeholderPair).queue { message ->
            if (RobertifyAudioManagerKt.joinAudioChannel(channel, musicManager, hookMessage = message)) {
                message.editEmbed(guild, JoinMessages.JOINED, placeholderPair).queue()
            }
        }
    }

    override val help: String
        get() = """
                Use this command to forcefully move the bot into your voice channel.

                *NOTE: This command can be made DJ only by using* `toggles dj join`"""
}