package main.commands;

import main.utils.database.BotUtils;

public interface IDevCommand extends ICommand {
    default boolean permissionCheck(CommandContext ctx) {
        try {
            if (!new BotUtils().isDeveloper(ctx.getMessage().getAuthor().getId()))
                return false;

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
