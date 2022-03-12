package main.commands.prefixcommands;

import lombok.SneakyThrows;
import main.utils.database.mongodb.cache.BotInfoCache;

@Deprecated
public interface IDevCommand extends ICommand {
    @SneakyThrows
    default boolean permissionCheck(CommandContext ctx) {
        return BotInfoCache.getInstance().isDeveloper(ctx.getMessage().getAuthor().getIdLong());
    }
}
