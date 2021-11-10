package main.commands;

import lombok.SneakyThrows;
import main.utils.database.BotUtils;

public interface IDevCommand extends ICommand {
    @SneakyThrows
    default boolean permissionCheck(CommandContext ctx) {
        return new BotUtils().isDeveloper(ctx.getMessage().getAuthor().getId());
    }
}
