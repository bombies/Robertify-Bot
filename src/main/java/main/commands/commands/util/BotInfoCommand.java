package main.commands.commands.util;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.RobertifyTheme;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.database.mongodb.cache.BotInfoCache;
import main.utils.json.themes.ThemesConfig;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.time.Instant;

public class BotInfoCommand extends AbstractSlashCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {

        ctx.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(ctx.getGuild(), "\t")
                        .setThumbnail(new ThemesConfig().getTheme(ctx.getGuild().getIdLong()).getTransparent())
                        .addField("Developers", "<@274681651945144321>", false)
                        .addField("About Me", "Robertify is a music bot programmed completely " +
                        "in Java using JDA. The name \"Robertify\" originated from the simple fact that a friend of bombies (main Developer) " +
                        "named Robert wanted a music bot, so he made one for him. Eventually, Robertify became his own project to him and he's been putting in most of " +
                        "his efforts into it ever since.", false)
                        .addField("Uptime", GeneralUtils.getDurationString(System.currentTimeMillis() - BotInfoCache.getInstance().getLastStartup()), false)
                        .setTimestamp(Instant.now())
                .build())
                .setActionRow(
                        Button.of(ButtonStyle.LINK, "https://robertify.me/terms", "Terms of Service", RobertifyTheme.ORANGE.getEmoji()),
                        Button.of(ButtonStyle.LINK, "https://robertify.me/privacypolicy", "Privacy Policy", RobertifyTheme.BLUE.getEmoji())
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
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!checks(event)) return;

        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "\t")
                        .setThumbnail(new ThemesConfig().getTheme(event.getGuild().getIdLong()).getTransparent())
                        .addField("Developers", "<@274681651945144321>", false)
                        .addField("About Me", "Robertify is a music bot programmed completely " +
                                "in Java using JDA. The name \"Robertify\" originated from the simple fact that a friend of bombies (main Developer) " +
                                "named Robert wanted a music bot, so he made one for him. Eventually, Robertify became his own project to him and he's been putting in most of " +
                                "his efforts into it ever since.", false)
                        .addField("Uptime", GeneralUtils.getDurationString(System.currentTimeMillis() - BotInfoCache.getInstance().getLastStartup()), false)
                        .setTimestamp(Instant.now())
                        .build())
                .addActionRow(
                        Button.of(ButtonStyle.LINK, "https://robertify.me/terms", "Terms of Service", RobertifyTheme.ORANGE.getEmoji()),
                        Button.of(ButtonStyle.LINK, "https://robertify.me/privacypolicy", "Privacy Policy", RobertifyTheme.BLUE.getEmoji())
                )
                .queue();
    }
}
