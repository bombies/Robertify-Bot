package main.commands;

import net.dv8tion.jda.api.Permission;

import javax.script.ScriptException;
import java.util.List;

public interface ICommand {
    void handle(CommandContext ctx) throws ScriptException;

    String getName();

    String getHelp(String guildID);

    default List<String> getAliases() {
        return List.of();
    }

    default List<Permission> getPermissionsRequired() {
        return null;
    }

    default boolean requiresPermission() {
        return getPermissionsRequired() != null;
    }
}
