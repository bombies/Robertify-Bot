package main.commands.commands.dev;

import main.commands.CommandContext;
import main.commands.IDevCommand;
import main.commands.RandomMessageManager;
import main.constants.ENV;
import main.main.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;

public class ReloadConfigCommand implements IDevCommand {
    private final static Logger logger = LoggerFactory.getLogger(ReloadConfigCommand.class);

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        try {
            Config.reload();
            RandomMessageManager.setChance(Double.parseDouble(Config.get(ENV.RANDOM_MESSAGE_CHANCE)));
            ctx.getMessage().addReaction("✅").queue();
        } catch (Exception e) {
            logger.error("There was an unexpected error!", e);
            ctx.getMessage().addReaction("❌").queue();
        }
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }
}
