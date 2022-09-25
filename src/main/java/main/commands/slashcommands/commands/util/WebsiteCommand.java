package main.commands.slashcommands.commands.util;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.RobertifyTheme;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;

public class WebsiteCommand extends AbstractSlashCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        ctx.getMessage().replyEmbeds(getEmbed(ctx.getGuild()))
                .setActionRow(Button.of(ButtonStyle.LINK, "https://robertify.me/", "Website", RobertifyTheme.GREEN.getEmoji()))
                .queue();
    }

    public MessageEmbed getEmbed(Guild guild) {
        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.WEBSITE_EMBED_DESC).build();
    }

    @Override
    public String getName() {
        return "website";
    }

    @Override
    public String getHelp(String prefix) {
        return "Visit our website using this command";
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("website")
                        .setDescription("Visit our website using this command")
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Visit our website using this command";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;

        event.replyEmbeds(getEmbed(event.getGuild()))
                .addActionRow(Button.of(ButtonStyle.LINK, "https://robertify.me/", "Website", RobertifyTheme.GREEN.getEmoji()))
                .setEphemeral(true)
                .queue();
    }
}
