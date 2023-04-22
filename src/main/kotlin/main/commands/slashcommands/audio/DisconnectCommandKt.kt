package main.commands.slashcommands.audio

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import main.audiohandlers.RobertifyAudioManagerKt
import main.utils.RobertifyEmbedUtilsKt.Companion.sendWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.json.logs.LogTypeKt
import main.utils.json.logs.LogUtilsKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class DisconnectCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "disconnect",
        description = "Disconnect the bot from the voice channel it's currently in",
    )
) {
    override suspend fun handle(event: SlashCommandInteractionEvent) {
        if (!checks(event)) return

        event.deferReply().queue()

        val guild = event.guild!!
        val selfVoiceState = guild.selfMember.voiceState!!

        if (!selfVoiceState.inAudioChannel()) {
            event.hook.sendWithEmbed(guild) {
                embed(RobertifyLocaleMessageKt.DisconnectMessages.NOT_IN_CHANNEL)
            }.queue()
            return
        }

        val memberVoiceState = event.member!!.voiceState!!

        if (!memberVoiceState.inAudioChannel() || memberVoiceState.channel != selfVoiceState.channel) {
            event.hook.sendWithEmbed(guild) {
                embed(
                    RobertifyLocaleMessageKt.GeneralMessages.SAME_VOICE_CHANNEL_LOC,
                    Pair("{channel}", selfVoiceState.channel!!.asMention)
                )
            }.queue()
            return
        }

        runBlocking {
            launch {
                RobertifyAudioManagerKt
                    .getMusicManager(guild)
                    .leave()

                event.hook.sendWithEmbed(guild) {
                    embed(RobertifyLocaleMessageKt.DisconnectMessages.DISCONNECTED)
                }.queue()

                LogUtilsKt(guild)
                    .sendLog(
                        LogTypeKt.BOT_DISCONNECTED,
                        "${event.user.asMention} ${
                            LocaleManagerKt.getLocaleManager(guild)
                                .getMessage(RobertifyLocaleMessageKt.DisconnectMessages.DISCONNECTED_USER)
                        }"
                    )
            }
        }
    }

    override val help: String
        get() = "Forces the bot to stop playing music and leave the voice channel" +
                " if already in one."

}