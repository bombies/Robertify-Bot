package main.commands;

import lombok.SneakyThrows;
import main.utils.database.sqlite3.BotDB;

public interface IDevCommand extends ICommand {
    @SneakyThrows
    default boolean permissionCheck(CommandContext ctx) {
        return new BotDB().isDeveloper(ctx.getMessage().getAuthor().getId());
    }
}
