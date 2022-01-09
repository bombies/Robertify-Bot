package main.commands.commands.util;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.InteractiveCommand;
import main.utils.database.mongodb.cache.BotInfoCache;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;

public class UptimeCommand extends InteractiveCommand implements ICommand {
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
                        "Get how long the bot has been online"
                ))
                .build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(getName())) return;

        event.replyEmbeds(
                RobertifyEmbedUtils.embedMessage(
                        event.getGuild(),
                        GeneralUtils.getDurationString(System.currentTimeMillis() - BotInfoCache.getInstance().getLastStartup())
                ).build()
        ).setEphemeral(false).queue();
    }
}
