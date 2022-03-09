package main.commands.commands.util;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.component.legacy.InteractiveCommand;
import main.utils.database.mongodb.cache.BotInfoCache;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;

public class UptimeCommand extends AbstractSlashCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        ctx.getMessage().replyEmbeds(
                RobertifyEmbedUtils.embedMessage(
                        ctx.getGuild(),
                        GeneralUtils.getDurationString(System.currentTimeMillis() - BotInfoCache.getInstance().getLastStartup())
                ).build()
        ).queue();
    }

    @Override
    public String getName() {
        return "uptime";
    }

    @Override
    public String getHelp(String prefix) {
        return "See how long the bot has been online";
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("uptime")
                        .setDescription("Get how long the bot has been online")
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "See how long the bot has been online";
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!nameCheck(event)) return;

        event.replyEmbeds(
                RobertifyEmbedUtils.embedMessage(
                        event.getGuild(),
                        GeneralUtils.getDurationString(System.currentTimeMillis() - BotInfoCache.getInstance().getLastStartup())
                ).build()
        ).setEphemeral(false).queue();
    }
}
