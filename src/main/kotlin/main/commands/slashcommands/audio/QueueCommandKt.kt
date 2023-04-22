package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManagerKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import main.utils.pagination.PaginationHandlerKt
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class QueueCommandKt : AbstractSlashCommandKt(
    CommandKt(
    name = "queue",
    description = "See all queued songs"
)
) {
    override suspend fun handle(event: SlashCommandInteractionEvent) {
        if (!checks(event)) return

        val guild = event.guild!!
        val musicManager = RobertifyAudioManagerKt.getMusicManager(guild)
        val queueHandler = musicManager.scheduler.queueHandler

        if (queueHandler.isEmpty) {
            event.replyWithEmbed(guild) {
                embed(RobertifyLocaleMessageKt.GeneralMessages.NOTHING_IN_QUEUE)
            }.queue()
        }

        PaginationHandlerKt.paginateQueue(event)
    }

    override val help: String
        get() = "Shows all the queued songs"
}