package main.commands.slashcommands.commands.util;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.BotConstants;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;

public class SupportServerCommand extends AbstractSlashCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        ctx.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(ctx.getGuild(), RobertifyLocaleMessage.SupportServerMessages.JOIN_SUPPORT_SERVER).build())
                .setActionRow(Button.of(ButtonStyle.LINK, BotConstants.SUPPORT_SERVER.toString(), LocaleManager.getLocaleManager(ctx.getGuild()).getMessage(RobertifyLocaleMessage.SupportServerMessages.SUPPORT_SERVER), Emoji.fromUnicode("🗣️")))
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
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("support")
                        .setDescription("Need help? Use this command to join our support server!")
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Need additional help? Use this command to join our support server!";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;

        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), RobertifyLocaleMessage.SupportServerMessages.JOIN_SUPPORT_SERVER).build())
                .setEphemeral(true)
                .addActionRow(Button.of(ButtonStyle.LINK, BotConstants.SUPPORT_SERVER.toString(), LocaleManager.getLocaleManager(event.getGuild()).getMessage(RobertifyLocaleMessage.SupportServerMessages.SUPPORT_SERVER), Emoji.fromUnicode("🗣️")))
                .queue();
    }
}
