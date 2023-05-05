package main.commands.slashcommands.audio

import dev.minn.jda.ktx.util.SLF4J
import main.audiohandlers.RobertifyAudioManagerKt
import main.utils.RobertifyEmbedUtilsKt
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
        event.deferReply().queue()
        event.hook.sendEmbed {
            handleJoin(
                event.guild!!,
                event.channel.asGuildMessageChannel(),
                event.member!!.voiceState!!,
                event.guild!!.selfMember.voiceState!!
            )
        }
            .queue()
    }

    private fun handleJoin(
        guild: Guild,
        messageChannel: GuildMessageChannel,
        memberVoiceState: GuildVoiceState,
        selfVoiceState: GuildVoiceState
    ): MessageEmbed {
        if (!memberVoiceState.inAudioChannel())
            return RobertifyEmbedUtilsKt.embedMessage(
                guild,
                GeneralMessages.USER_VOICE_CHANNEL_NEEDED
            ).build()

        val channel = memberVoiceState.channel!!
        val musicManager = RobertifyAudioManagerKt[guild]
        val placeholderPair = Pair("{channel}", channel.asMention)

        if (selfVoiceState.inAudioChannel() && memberVoiceState.channel!!.id == selfVoiceState.channel!!.id)
            return RobertifyEmbedUtilsKt.embedMessage(
                guild,
                JoinMessages.ALREADY_JOINED,
                placeholderPair
            ).build()

        return try {
            RobertifyAudioManagerKt.joinAudioChannel(channel, musicManager, messageChannel)
            RobertifyEmbedUtilsKt.embedMessage(guild, JoinMessages.JOINED, placeholderPair)
                .build()
        } catch (e: IllegalStateException) {
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                JoinMessages.CANT_JOIN,
                placeholderPair
            )
                .build()
        } catch (e: InsufficientPermissionException) {
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                GeneralMessages.INSUFFICIENT_PERMS_TO_JOIN,
                placeholderPair
            ).build()
        } catch (e: Exception) {
            logger.error("Unexpected error", e)
            RobertifyEmbedUtilsKt.embedMessage(guild, GeneralMessages.UNEXPECTED_ERROR).build()
        }
    }

    override val help: String
        get() = """
                Use this command to forcefully move the bot into your voice channel.

                *NOTE: This command can be made DJ only by using* `toggles dj join`"""
}