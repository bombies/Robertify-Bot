package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManagerKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.locale.messages.HistoryMessages
import main.utils.pagination.PaginationHandlerKt
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class HistoryCommandKt : AbstractSlashCommandKt(CommandKt(
    name = "history",
    description = "See all the songs that have been played in your current listening session"
)) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val scheduler = RobertifyAudioManagerKt[guild].scheduler
        val queueHandler = scheduler.queueHandler

        if (queueHandler.isPreviousTracksEmpty)
            event.replyWithEmbed(guild) { embed(HistoryMessages.NO_PAST_TRACKS) }
                .queue()
        else {
            val content = QueueCommandKt().getPastContent(guild, queueHandler)
            PaginationHandlerKt.paginateMessage(event, content)
        }
    }

}