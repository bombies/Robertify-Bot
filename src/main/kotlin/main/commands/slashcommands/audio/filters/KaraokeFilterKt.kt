package main.commands.slashcommands.audio.filters

import lavalink.client.io.filters.Karaoke
import main.commands.slashcommands.audio.filters.internal.handleGenericFilterToggle
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class KaraokeFilterKt : AbstractSlashCommandKt(CommandKt(
    name = "karaoke",
    description = "Toggle the Karaoke filter.",
    isPremium = true
)) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyWithEmbed {
            handleGenericFilterToggle(
                memberVoiceState = event.member!!.voiceState!!,
                selfVoiceState = event.guild!!.selfMember.voiceState!!,
                filterName = "Karaoke",
                filterPredicate = { karaoke != null },
                filterOn = { setKaraoke(Karaoke()).commit() },
                filterOff = { setKaraoke(null).commit() }
            )
        }.queue()
    }

    override val help: String
        get() = "Toggle the Karaoke filter."
}