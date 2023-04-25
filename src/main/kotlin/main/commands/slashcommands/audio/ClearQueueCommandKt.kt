package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManagerKt
import main.utils.RobertifyEmbedUtilsKt.Companion.sendWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.json.logs.LogTypeKt
import main.utils.json.logs.LogUtilsKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class ClearQueueCommandKt : AbstractSlashCommandKt(CommandKt(
    name = "clear",
    description = "Clear the queue of all its contents."
)) {

    override val help: String
        get() = "Clear all the queued songs"

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val musicManager = RobertifyAudioManagerKt.getMusicManager(guild)
        val queueHandler = musicManager.scheduler.queueHandler
        val localeManager = LocaleManagerKt.getLocaleManager(guild)

        event.deferReply().queue()

        if (queueHandler.isEmpty) {
            event.hook.sendWithEmbed(guild) {
                embed(RobertifyLocaleMessageKt.ClearQueueMessages.CQ_NOTHING_IN_QUEUE)
            }.queue()
            return
        }

        val memberVoiceState = event.member!!.voiceState!!
        val selfVoiceState = guild.selfMember.voiceState!!
        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState)
        if (acChecks != null) {
            event.hook.sendWithEmbed { acChecks }.queue()
            return
        }

        queueHandler.clear()
        LogUtilsKt(guild).sendLog(
            LogTypeKt.QUEUE_CLEAR,
            "${event.user.asMention} ${localeManager.getMessage(RobertifyLocaleMessageKt.ClearQueueMessages.QUEUE_CLEARED_USER)}"
        )

        event.hook.sendWithEmbed(guild) {
            embed(RobertifyLocaleMessageKt.ClearQueueMessages.QUEUE_CLEAR)
        }.queue()
    }
}