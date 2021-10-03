package main.commands.commands.management;

import lombok.SneakyThrows;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.commands.IDevCommand;
import main.main.Robertify;
import main.utils.database.BotUtils;
import me.duncte123.botcommons.BotCommons;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;

import javax.script.ScriptException;

public class ShutdownCommand implements IDevCommand {
    @SneakyThrows
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!new BotUtils().isDeveloper(ctx.getAuthor().getId()))
            return;

        EmbedBuilder eb = EmbedUtils.embedMessage("Now shutting down...");
        ctx.getMessage().replyEmbeds(eb.build()).queue();

        BotCommons.shutdown();
        Robertify.api.shutdown();
    }

    @Override
    public String getName() {
        return "shutdown";
    }

    @Override
    public String getHelp(String guildID) {
        return "Shuts the bot down";
    }
}
