package main.commands.slashcommands.util

import dev.minn.jda.ktx.interactions.components.link
import main.constants.RobertifyTheme
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.json.themes.ThemesConfig
import main.utils.locale.messages.GeneralMessages
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class WebsiteCommand : AbstractSlashCommand(SlashCommand(
    name = "website",
    description = "Visit our website using this command.",
    guildUseOnly = false
)) {

    override fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbed(GeneralMessages.WEBSITE_EMBED_DESC)
            .setActionRow(
                link(
                    url = "https://robertify.me",
                    label = "Website",
                    emoji = if (event.guild != null) ThemesConfig(event.guild!!).getTheme().emoji else RobertifyTheme.GREEN.emoji
                )
            )
            .queue()
    }

    override val help: String
        get() = "Visit our website using this command."
}