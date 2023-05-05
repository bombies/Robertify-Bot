package main.commands.slashcommands.audio.filters

import lavalink.client.io.filters.Timescale
import main.commands.slashcommands.audio.filters.internal.handleGenericFilterToggle
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class NightcoreFilterKt : AbstractSlashCommandKt(CommandKt(
    name = "nightcore",
    description = "Toggle the Nightcore filter.",
    isPremium = true
)) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbed {
            handleGenericFilterToggle(
                event = event,
                filterName = "Nightcore",
                filterPredicate = { timescale != null },
                filterOn = { setTimescale(Timescale().setPitch(1.5F)).commit() },
                filterOff = { setTimescale(null).commit() }
            )
        }.queue()
    }

    override val help: String
        get() = "Toggle the Nightcore filter."
}