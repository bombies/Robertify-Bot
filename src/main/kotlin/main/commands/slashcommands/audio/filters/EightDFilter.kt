package main.commands.slashcommands.audio.filters

import dev.schlaubi.lavakord.audio.player.rotation
import main.commands.slashcommands.audio.filters.internal.handleGenericFilterToggle
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
            filterPredicate = { rotation != null },
            filterOn = {
                rotation {
                    rotationHz = 0.05
                }
            },
            filterOff = { unsetRotation() }
        )

    override val help: String
        get() = "Toggle the 8D filter"
}