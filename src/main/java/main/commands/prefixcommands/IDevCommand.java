package main.commands.prefixcommands;

import lombok.SneakyThrows;
import main.utils.database.mongodb.cache.BotBDCache;

@Deprecated
public interface IDevCommand extends ICommand {
    @SneakyThrows
    default boolean permissionCheck(CommandContext ctx) {
        return BotBDCache.getInstance().isDeveloper(ctx.getMessage().getAuthor().getIdLong());
    }
}
