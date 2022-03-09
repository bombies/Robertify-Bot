package main.commands.commands.util;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.RobertifyTheme;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.component.legacy.InteractiveCommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
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
        return RobertifyEmbedUtils.embedMessage(guild, "Click on the link below to visit our website!").build();
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
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!nameCheck(event)) return;

        event.replyEmbeds(getEmbed(event.getGuild()))
                .addActionRow(Button.of(ButtonStyle.LINK, "https://robertify.me/", "Website", RobertifyTheme.GREEN.getEmoji()))
                .setEphemeral(true)
                .queue();
    }
}
