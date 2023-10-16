package main.commands.slashcommands.dev.test

import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class TestSentryCommand : AbstractSlashCommand(
    SlashCommand(
        name = "testsentry",
        description = "Test if Sentry is correctly capturing events",
        developerOnly = true
    )
) {

    override fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbed("Crashing! Check Sentry to see if the error was pushed.")
            .setEphemeral(true)
            .queue()
        throw RuntimeException("Forced exception!")
    }
}