package main.commands.slashcommands.audio.filters

import dev.arbjerg.lavalink.protocol.v4.Tremolo
import dev.arbjerg.lavalink.protocol.v4.ifPresentAndNotNull
import main.commands.slashcommands.audio.filters.internal.handleGenericFilterToggle
import main.utils.GeneralUtils.isNotNull
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

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbed {
            handleGenericFilterToggle(
                event = event,
                filterName = "Tremolo",
                filterPredicate = { tremolo.ifPresentAndNotNull { true } ?: false },
                filterOn = { setTremolo(Tremolo()) },
                filterOff = { setTremolo(null) }
            )
        }.queue()
    }

    override val help: String
        get() = "Toggle the Tremolo filter."
}