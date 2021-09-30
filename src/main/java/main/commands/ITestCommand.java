package main.commands;

import main.utils.database.BotUtils;

public interface ITestCommand extends ICommand {
    default boolean permissionCheck(CommandContext ctx) {
        try {
            if (!new BotUtils().isDeveloper(ctx.getMessage().getAuthor().getId())) {
                ctx.getChannel().sendMessage("You don't have permission to run test commands.").queue();
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
