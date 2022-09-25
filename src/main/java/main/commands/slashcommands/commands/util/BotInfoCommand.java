package main.commands.slashcommands.commands.util;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.RobertifyTheme;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.database.mongodb.cache.BotBDCache;
import main.utils.json.themes.ThemesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.time.Instant;

public class BotInfoCommand extends AbstractSlashCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var localeManager = LocaleManager.getLocaleManager(ctx.getGuild());
        ctx.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(ctx.getGuild(), "\t")
                        .setThumbnail(new ThemesConfig(ctx.getGuild()).getTheme().getTransparent())
                        .addField(localeManager.getMessage(RobertifyLocaleMessage.BotInfoMessages.BOT_INFO_DEVELOPERS), "<@274681651945144321>", false)
                        .addField(localeManager.getMessage(RobertifyLocaleMessage.BotInfoMessages.BOT_INFO_ABOUT_ME_LABEL), localeManager.getMessage(RobertifyLocaleMessage.BotInfoMessages.BOT_INFO_ABOUT_ME_VALUE), false)
                        .addField(localeManager.getMessage(RobertifyLocaleMessage.BotInfoMessages.BOT_INFO_UPTIME), GeneralUtils.getDurationString(System.currentTimeMillis() - BotBDCache.getInstance().getLastStartup()), false)
                        .setTimestamp(Instant.now())
                .build())
                .setActionRow(
                        Button.of(ButtonStyle.LINK, "https://robertify.me/terms", localeManager.getMessage(RobertifyLocaleMessage.BotInfoMessages.BOT_INFO_TERMS), RobertifyTheme.ORANGE.getEmoji()),
                        Button.of(ButtonStyle.LINK, "https://robertify.me/privacypolicy", localeManager.getMessage(RobertifyLocaleMessage.BotInfoMessages.BOT_INFO_PRIVACY), RobertifyTheme.BLUE.getEmoji())
                )
                .queue();
    }

    @Override
    public String getName() {
        return "botinfo";
    }

    @Override
    public String getHelp(String prefix) {
        return "Looking for information on the bot? This is the place to find it.";
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("botinfo")
                        .setDescription("View some cool stuff about Robertify!")
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Looking for information on the bot? This is the place to find it.";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;

        final var localeManager = LocaleManager.getLocaleManager(event.getGuild());
        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "\t")
                        .setThumbnail(new ThemesConfig(event.getGuild()).getTheme().getTransparent())
                        .addField(localeManager.getMessage(RobertifyLocaleMessage.BotInfoMessages.BOT_INFO_DEVELOPERS), "<@274681651945144321>", false)
                        .addField(localeManager.getMessage(RobertifyLocaleMessage.BotInfoMessages.BOT_INFO_ABOUT_ME_LABEL), localeManager.getMessage(RobertifyLocaleMessage.BotInfoMessages.BOT_INFO_ABOUT_ME_VALUE), false)
                        .addField(localeManager.getMessage(RobertifyLocaleMessage.BotInfoMessages.BOT_INFO_UPTIME), GeneralUtils.getDurationString(System.currentTimeMillis() - BotBDCache.getInstance().getLastStartup()), false)
                        .setTimestamp(Instant.now())
                        .build())
                .addActionRow(
                        Button.of(ButtonStyle.LINK, "https://robertify.me/terms", localeManager.getMessage(RobertifyLocaleMessage.BotInfoMessages.BOT_INFO_TERMS), RobertifyTheme.ORANGE.getEmoji()),
                        Button.of(ButtonStyle.LINK, "https://robertify.me/privacypolicy", localeManager.getMessage(RobertifyLocaleMessage.BotInfoMessages.BOT_INFO_PRIVACY), RobertifyTheme.BLUE.getEmoji())
                )
                .queue();
    }
}
