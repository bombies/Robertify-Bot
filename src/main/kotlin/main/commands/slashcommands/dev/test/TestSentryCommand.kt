package main.commands.slashcommands.dev.test

import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class TestSentryCommand : AbstractSlashCommandKt(
    CommandKt(
        name = "testsentry",
        description = "Test if Sentry is correctly capturing events",
        developerOnly = true
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbed(message = "Crashing! Check Sentry to see if the error was pushed.")
            .setEphemeral(true)
            .queue()
        throw RuntimeException("Forced exception!")
    }
}