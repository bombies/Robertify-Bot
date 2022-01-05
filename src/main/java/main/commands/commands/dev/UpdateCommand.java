package main.commands.commands.dev;

import main.commands.CommandContext;
import main.commands.IDevCommand;
import main.main.Robertify;
import main.utils.database.mongodb.AbstractMongoDatabase;
import main.utils.json.AbstractGuildConfig;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.List;

public class UpdateCommand implements IDevCommand {
    private final Logger logger = LoggerFactory.getLogger(UpdateCommand.class);
    
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();

        if (args.isEmpty()) {
            msg.replyEmbeds(EmbedUtils.embedMessage("You must provide arguments!").build())
                    .queue();
            return;
        }

        switch (args.get(0).toLowerCase()) {
            case "db" -> {
                AbstractMongoDatabase.initAllCaches();
                msg.addReaction("✅").queue();
            }
            case "dedichannel", "dc" -> handleDedicatedChannelUpdates(msg, args);
            default -> msg.replyEmbeds(EmbedUtils.embedMessage("Invalid args!").build()).queue();
        }
    }

    public void handleDedicatedChannelUpdates(Message msg, List<String> args) {
        if (args.size() < 2) {
            msg.replyEmbeds(EmbedUtils.embedMessage("You must provide more arguments!").build()).queue();
            return;
        }

        var conf = new DedicatedChannelConfig();

        try {
            switch (args.get(1).toLowerCase()) {
                case "all" -> {
                    for (Guild g : Robertify.api.getGuilds()) {
                        conf.updateButtons(g);
                        conf.updateTopic(g);
                        conf.updateMessage(g);
                    }
                }
                case "topic" -> conf.updateTopic();
                case "buttons" -> conf.updateButtons();
                case "message" -> {
                    for (Guild g : Robertify.api.getGuilds())
                        conf.updateMessage(g);
                }
                default -> msg.replyEmbeds(EmbedUtils.embedMessage("Invalid arg").build()).queue();
            }
            msg.addReaction("✅").queue();
        } catch (Exception e) {
            logger.error("[FATAL ERROR] An unexpected error occurred!", e);
            msg.addReaction("❌").queue();
        }
    }

    @Override
    public String getName() {
        return "update";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }
}
