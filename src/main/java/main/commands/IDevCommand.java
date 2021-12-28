package main.commands;

import lombok.SneakyThrows;
import main.utils.database.mongodb.cache.BotInfoCache;
import main.utils.database.sqlite3.BotDB;

public interface IDevCommand extends ICommand {
    @SneakyThrows
    default boolean permissionCheck(CommandContext ctx) {
        return BotInfoCache.getInstance().isDeveloper(ctx.getMessage().getAuthor().getIdLong());
    }
}
