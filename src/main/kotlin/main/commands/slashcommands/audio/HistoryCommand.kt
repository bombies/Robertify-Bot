package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManager
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.locale.messages.HistoryMessages
import main.utils.pagination.PaginationHandler
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class HistoryCommand : AbstractSlashCommand(
    SlashCommand(
        name = "history",
        description = "See all the songs that have been played in your current listening session"
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val scheduler = RobertifyAudioManager[guild].scheduler
        val queueHandler = scheduler.queueHandler

        if (queueHandler.isPreviousTracksEmpty)
            event.replyEmbed { embed(HistoryMessages.NO_PAST_TRACKS) }
                .queue()
        else {
            val content = QueueCommand().getPastContent(guild, queueHandler).reversed()
            PaginationHandler.paginateMessage(event, content)
        }
    }

}