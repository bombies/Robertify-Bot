package main.commands.slashcommands.audio.filters

import dev.arbjerg.lavalink.protocol.v4.Vibrato
import dev.arbjerg.lavalink.protocol.v4.ifPresentAndNotNull
import main.commands.slashcommands.audio.filters.internal.handleGenericFilterToggle
import main.utils.GeneralUtils.isNotNull
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class VibratoFilter : AbstractSlashCommand(
    SlashCommand(
        name = "vibrato",
        description = "Toggle the Vibrato filter.",
        isPremium = true
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbed {
            handleGenericFilterToggle(
                event = event,
                filterName = "Vibrato",
                filterPredicate = { vibrato.ifPresentAndNotNull { true } ?: false },
                filterOn = { setVibrato(Vibrato()) },
                filterOff = { setVibrato(null) }
            )
        }.queue()
    }

    override val help: String
        get() = "Toggle the Vibrato filter."
}