package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManagerKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.RobertifyEmbedUtilsKt.Companion.sendWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.locale.messages.DuplicateMessages
import main.utils.locale.messages.GeneralMessages
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class RemoveDuplicatesCommandKt : AbstractSlashCommandKt(CommandKt(
    name = "removedupes",
    description = "Remove all duplicate tracks in the queue."
)) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val queueHandler = RobertifyAudioManagerKt.getMusicManager(guild)
            .scheduler
            .queueHandler

        if (queueHandler.isEmpty)
            return event.replyWithEmbed(guild) {
                embed(GeneralMessages.NOTHING_IN_QUEUE)
            }.queue()

        event.deferReply().queue()
        val newQueue = queueHandler.contents.distinctBy { it.identifier }
        queueHandler.clear()
        queueHandler.addAll(newQueue)

        event.hook
            .sendWithEmbed(guild, DuplicateMessages.REMOVED_DUPLICATES)
            .queue()
    }
}