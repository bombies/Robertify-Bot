package main.commands.slashcommands.audio.filters

import lavalink.client.io.filters.Rotation
import main.commands.slashcommands.audio.filters.internal.handleGenericFilterToggle
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class EightDFilterKt : AbstractSlashCommandKt(
    CommandKt(
        name = "8d",
        description = "toggle the 8D filter.",
        isPremium = true
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbed { handle8DToggle(event.member!!.voiceState!!, event.guild!!.selfMember.voiceState!!) }
            .queue()
    }

    private fun handle8DToggle(memberVoiceState: GuildVoiceState, selfVoiceState: GuildVoiceState): MessageEmbed =
        handleGenericFilterToggle(
            memberVoiceState = memberVoiceState,
            selfVoiceState = selfVoiceState,
            filterName = "8D",
            filterPredicate = { rotation != null },
            filterOn = { setRotation(Rotation().setFrequency(0.05F)).commit() },
            filterOff = { setRotation(null).commit() }
        )

    override val help: String
        get() = "Toggle the 8D filter"
}