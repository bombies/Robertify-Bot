package main.commands.commands.util;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.BotConstants;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.legacy.InteractiveCommand;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;

public class SupportServerCommand extends InteractiveCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        ctx.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(ctx.getGuild(), "Click on the button below to join our support server").build())
                .setActionRow(Button.of(ButtonStyle.LINK, BotConstants.SUPPORT_SERVER.toString(), "Support Server", Emoji.fromUnicode("🗣️")))
                .queue();
    }

    @Override
    public String getName() {
        return "support";
    }

    @Override
    public String getHelp(String prefix) {
        return "Need additional help? Use this command to join our support server!";
    }

    @Override
    public void initCommand() {
        setInteractionCommand(getCommand());
        upsertCommand();
    }

    @Override
    public void initCommand(Guild g) {
        setInteractionCommand(getCommand());
        upsertCommand(g);
    }

    private InteractionCommand getCommand() {
        return InteractionCommand.create()
                .setCommand(Command.of(
                        getName(),
                        "Need help? Use this command to join our support server!"
                )).build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(getName())) return;

        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "Click on the button below to join our support server").build())
                .setEphemeral(true)
                .addActionRow(Button.of(ButtonStyle.LINK, BotConstants.SUPPORT_SERVER.toString(), "Support Server", Emoji.fromUnicode("🗣️")))
                .queue();
    }
}
