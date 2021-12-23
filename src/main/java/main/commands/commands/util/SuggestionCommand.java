package main.commands.commands.util;

import main.commands.CommandContext;
import main.commands.ICommand;

import javax.script.ScriptException;

public class SuggestionCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {

    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

    @Override
    public String getUsages(long guildID) {
        return ICommand.super.getUsages(guildID);
    }
}
