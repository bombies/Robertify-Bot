package main.commands.slashcommands.audio.filters

import dev.schlaubi.lavakord.audio.player.tremolo
import main.commands.slashcommands.audio.filters.internal.handleGenericFilterToggle
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class TremoloFilter : AbstractSlashCommand(
    SlashCommand(
        name = "tremolo",
        description = "Toggle the Tremolo filter.",
        isPremium = true
    )
) {

    override fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbed {
            handleGenericFilterToggle(
                event = event,
                filterName = "Tremolo",
                filterPredicate = { tremolo != null },
                filterOn = { tremolo {} },
                filterOff = { unsetTremolo() }
            )
        }.queue()
    }

    override val help: String
        get() = "Toggle the Tremolo filter."
}