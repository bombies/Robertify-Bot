package main.commands.commands.util;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.BotConstants;
import main.utils.GeneralUtils;
import main.utils.database.mongodb.cache.BotInfoCache;
import me.duncte123.botcommons.messaging.EmbedUtils;

import javax.script.ScriptException;
import java.time.Instant;

public class BotInfoCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {

        ctx.getMessage().replyEmbeds(EmbedUtils.embedMessage("\t")
                        .setThumbnail(BotConstants.ROBERTIFY_LOGO_TRANSPARENT.toString())
                        .addField("Developers", "<@274681651945144321>", false)
                        .addField("About Me", "Robertify is a music bot programmed completely " +
                        "in Java using JDA. The name \"Robertify\" originated from the simple fact that a friend of bombies (main Developer) " +
                        "named Robert wanted a music bot, so he made one for him. Eventually, Robertify became his own project to him and he's been putting it most of " +
                        "his efforts into it ever since.", false)
                        .addField("Uptime", GeneralUtils.getDurationString(System.currentTimeMillis() - BotInfoCache.getInstance().getLastStartup()), false)
                        .setTimestamp(Instant.now())
                .build()).queue();
    }

    @Override
    public String getName() {
        return "botinfo";
    }

    @Override
    public String getHelp(String prefix) {
        return "Looking for information on the bot? This is the place to find it.";
    }
}
