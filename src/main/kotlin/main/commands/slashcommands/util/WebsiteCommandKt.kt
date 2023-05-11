package main.commands.slashcommands.util

import dev.minn.jda.ktx.interactions.components.link
import main.constants.RobertifyThemeKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.json.themes.ThemesConfigKt
import main.utils.locale.messages.GeneralMessages
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class WebsiteCommandKt : AbstractSlashCommandKt(CommandKt(
    name = "website",
    description = "Visit our website using this command.",
    guildUseOnly = false
)) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbed(GeneralMessages.WEBSITE_EMBED_DESC)
            .setActionRow(
                link(
                    url = "https://robertify.me",
                    label = "Website",
                    emoji = if (event.guild != null) ThemesConfigKt(event.guild!!).theme.emoji else RobertifyThemeKt.GREEN.emoji
                )
            )
            .queue()
    }

    override val help: String
        get() = "Visit our website using this command."
}