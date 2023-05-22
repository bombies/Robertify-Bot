package main.commands.slashcommands.audio.filters

import lavalink.client.io.filters.Tremolo
import main.commands.slashcommands.audio.filters.internal.handleGenericFilterToggle
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class TremoloFilter : AbstractSlashCommand(SlashCommand(
    name = "tremolo",
    description = "Toggle the Tremolo filter.",
    isPremium = true
)) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbed {
            handleGenericFilterToggle(
                event = event,
                filterName = "Tremolo",
                filterPredicate = { tremolo != null },
                filterOn = { setTremolo(Tremolo()).commit() },
                filterOff = { setTremolo(null).commit() }
            )
        }.queue()
    }

    override val help: String
        get() = "Toggle the Tremolo filter."
}