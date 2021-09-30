package main.commands;

import javax.script.ScriptException;
import java.util.List;

public interface ICommand {
    void handle(CommandContext ctx) throws ScriptException;

    String getName();

    String getHelp(String guildID);

    default List<String> getAliases() {
        return List.of();
    }
}
