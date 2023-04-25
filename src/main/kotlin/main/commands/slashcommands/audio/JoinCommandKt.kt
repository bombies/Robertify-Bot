package main.commands.slashcommands.audio

import dev.minn.jda.ktx.util.SLF4J
import main.audiohandlers.RobertifyAudioManagerKt
import main.constants.ToggleKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.sendWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.json.restrictedchannels.RestrictedChannelsConfigKt
import main.utils.json.toggles.TogglesConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import java.lang.IllegalStateException

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
        event.hook.sendWithEmbed {
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
                RobertifyLocaleMessageKt.GeneralMessages.USER_VOICE_CHANNEL_NEEDED
            ).build()

        val channel = memberVoiceState.channel!!
        val musicManager = RobertifyAudioManagerKt.getMusicManager(guild)
        val placeholderPair = Pair("{channel}", channel.asMention)

        if (selfVoiceState.inAudioChannel() && memberVoiceState.channel!!.id == selfVoiceState.channel!!.id)
            return RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.JoinMessages.ALREADY_JOINED,
                placeholderPair
            ).build()

        return try {
            RobertifyAudioManagerKt.joinAudioChannel(channel, musicManager, messageChannel)
            RobertifyEmbedUtilsKt.embedMessage(guild, RobertifyLocaleMessageKt.JoinMessages.JOINED, placeholderPair)
                .build()
        } catch (e: IllegalStateException) {
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.JoinMessages.CANT_JOIN,
                placeholderPair
            )
                .build()
        } catch (e: InsufficientPermissionException) {
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.GeneralMessages.INSUFFICIENT_PERMS_TO_JOIN,
                placeholderPair
            ).build()
        } catch (e: Exception) {
            logger.error("Unexpected error", e)
            RobertifyEmbedUtilsKt.embedMessage(guild, RobertifyLocaleMessageKt.GeneralMessages.UNEXPECTED_ERROR).build()
        }
    }

    override val help: String
        get() = """
                Use this command to forcefully move the bot into your voice channel.

                *NOTE: This command can be made DJ only by using* `toggles dj join`"""
}