package main.commands.slashcommands.audio.filters

import dev.arbjerg.lavalink.protocol.v4.Karaoke
import main.commands.slashcommands.audio.filters.internal.handleGenericFilterToggle
import main.utils.GeneralUtils.isNotNull
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class KaraokeFilter : AbstractSlashCommand(
    SlashCommand(
        name = "karaoke",
        description = "Toggle the Karaoke filter.",
        isPremium = true
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbed {
            handleGenericFilterToggle(
                memberVoiceState = event.member!!.voiceState!!,
                selfVoiceState = event.guild!!.selfMember.voiceState!!,
                filterName = "Karaoke",
                filterPredicate = { karaoke.isNotNull() },
                filterOn = { setKaraoke(Karaoke()) },
                filterOff = { setKaraoke(null) }
            )
        }.queue()
    }

    override val help: String
        get() = "Toggle the Karaoke filter."
}