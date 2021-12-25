package main.commands;

import net.dv8tion.jda.api.Permission;

import javax.script.ScriptException;
import java.util.List;

public interface ICommand {
    void handle(CommandContext ctx) throws ScriptException;

    String getName();

    String getHelp(String prefix);

    default String getUsages(String prefix) {
        return null;
    }

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
