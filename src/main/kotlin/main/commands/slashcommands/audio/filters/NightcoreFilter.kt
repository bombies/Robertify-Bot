package main.commands.slashcommands.audio.filters

import dev.schlaubi.lavakord.audio.player.timescale
import main.commands.slashcommands.audio.filters.internal.handleGenericFilterToggle
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class NightcoreFilter : AbstractSlashCommand(
    SlashCommand(
        name = "nightcore",
        description = "Toggle the Nightcore filter.",
        isPremium = true
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbed {
            handleGenericFilterToggle(
                event = event,
                filterName = "Nightcore",
                filterPredicate = { timescale != null },
                filterOn = { timescale { pitch = 1.5 } },
                filterOff = { unsetTimescale() }
            )
        }.queue()
    }

    override val help: String
        get() = "Toggle the Nightcore filter."
}