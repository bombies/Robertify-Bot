package main.commands.slashcommands.util

import dev.minn.jda.ktx.interactions.components.link
import main.constants.BotConstants
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.Command
import main.utils.locale.LocaleManager
import main.utils.locale.messages.SupportServerMessages
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class SupportServerCommand : AbstractSlashCommand(Command(
    name = "support",
    description = "Need help? Use this command to join our support server.",
    guildUseOnly = false
)) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbed(SupportServerMessages.JOIN_SUPPORT_SERVER)
            .setEphemeral(true)
            .setActionRow(
                link(
                    url = BotConstants.SUPPORT_SERVER,
                    label = LocaleManager[event.guild].getMessage(SupportServerMessages.SUPPORT_SERVER),
                    emoji = Emoji.fromUnicode("🗣️")
                )
            )
            .queue()
    }

    override val help: String
        get() = "Need help? Use this command to join our support server."
}