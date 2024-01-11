package main.commands.slashcommands.audio.filters

import dev.arbjerg.lavalink.protocol.v4.Timescale
import dev.arbjerg.lavalink.protocol.v4.ifPresentAndNotNull
import main.commands.slashcommands.audio.filters.internal.handleGenericFilterToggle
import main.utils.GeneralUtils.isNotNull
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
                filterPredicate = { timescale.ifPresentAndNotNull { true } ?: false},
                filterOn = { setTimescale(Timescale(1.25, 1.5)) },
                filterOff = { setTimescale(null) }
            )
        }.queue()
    }

    override val help: String
        get() = "Toggle the Nightcore filter."
}