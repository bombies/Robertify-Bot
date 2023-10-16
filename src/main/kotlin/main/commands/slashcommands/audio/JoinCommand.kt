package main.commands.slashcommands.audio

import dev.minn.jda.ktx.util.SLF4J
import main.audiohandlers.RobertifyAudioManager
import main.utils.RobertifyEmbedUtils.Companion.editEmbed
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.JoinMessages
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class JoinCommand : AbstractSlashCommand(
    SlashCommand(
        name = "join",
        description = "Force the bot to join the audio channel you're currently in"
    )
) {

    companion object {
        private val logger by SLF4J
    }

    override fun handle(event: SlashCommandInteractionEvent) {
        handleJoin(event)
    }

    private fun handleJoin(
        event: SlashCommandInteractionEvent
    ) {
        val guild = event.guild!!
        val memberVoiceState = event.member!!.voiceState!!
        val selfVoiceState = guild.selfMember.voiceState!!

        if (!memberVoiceState.inAudioChannel())
            return event.replyEmbed(GeneralMessages.USER_VOICE_CHANNEL_NEEDED)
                .setEphemeral(true)
                .queue()

        val channel = memberVoiceState.channel!!
        val musicManager = RobertifyAudioManager[guild]
        val placeholderPair = Pair("{channel}", channel.asMention)

        if (selfVoiceState.inAudioChannel() && memberVoiceState.channel!!.id == selfVoiceState.channel!!.id)
            return event.replyEmbed(
                JoinMessages.ALREADY_JOINED,
                placeholderPair
            ).setEphemeral(true)
                .queue()

        event.replyEmbed(JoinMessages.ATTEMPTING_TO_JOIN, placeholderPair).queue { message ->
            if (RobertifyAudioManager.joinAudioChannel(channel, musicManager, hookMessage = message)) {
                message.editEmbed(guild, JoinMessages.JOINED, placeholderPair).queue()
            }
        }
    }

    override val help: String
        get() = """
                Use this command to forcefully move the bot into your voice channel.

                *NOTE: This command can be made DJ only by using* `toggles dj join`"""
}