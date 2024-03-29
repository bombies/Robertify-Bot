package main.commands.slashcommands.audio.filters

import dev.arbjerg.lavalink.protocol.v4.Rotation
import dev.arbjerg.lavalink.protocol.v4.ifPresentAndNotNull
import main.commands.slashcommands.audio.filters.internal.handleGenericFilterToggle
import main.utils.GeneralUtils.isNotNull
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class EightDFilter : AbstractSlashCommand(
    SlashCommand(
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
            filterPredicate = { rotation.ifPresentAndNotNull { it.rotationHz != 0.0 } ?: false },
            filterOn = {
                setRotation(Rotation(0.05))
            },
            filterOff = { setRotation(null) }
        )

    override val help: String
        get() = "Toggle the 8D filter"
}