package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManager
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.Command
import main.utils.locale.messages.DuplicateMessages
import main.utils.locale.messages.GeneralMessages
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class RemoveDuplicatesCommand : AbstractSlashCommand(Command(
    name = "removedupes",
    description = "Remove all duplicate tracks in the queue."
)) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val queueHandler = RobertifyAudioManager[guild]
            .scheduler
            .queueHandler

        if (queueHandler.isEmpty)
            return event.replyEmbed {
                embed(GeneralMessages.NOTHING_IN_QUEUE)
            }.queue()

        event.deferReply().queue()
        val newQueue = queueHandler.contents.distinctBy { it.identifier }
        queueHandler.clear()
        queueHandler.addAll(newQueue)

        event.hook
            .sendEmbed(guild, DuplicateMessages.REMOVED_DUPLICATES)
            .queue()
    }
}