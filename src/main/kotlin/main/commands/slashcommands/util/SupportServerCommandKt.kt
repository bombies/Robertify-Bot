package main.commands.slashcommands.util

import dev.minn.jda.ktx.interactions.components.link
import main.constants.BotConstantsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.SupportServerMessages
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class SupportServerCommandKt : AbstractSlashCommandKt(CommandKt(
    name = "support",
    description = "Need help? Use this command to join our support server.",
    guildUseOnly = false
)) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbed(SupportServerMessages.JOIN_SUPPORT_SERVER)
            .setEphemeral(true)
            .setActionRow(
                link(
                    url = BotConstantsKt.SUPPORT_SERVER,
                    label = LocaleManagerKt[event.guild].getMessage(SupportServerMessages.SUPPORT_SERVER),
                    emoji = Emoji.fromUnicode("🗣️")
                )
            )
            .queue()
    }

    override val help: String
        get() = "Need help? Use this command to join our support server."
}