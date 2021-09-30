package main.commands.commands;

import main.commands.CommandContext;
import main.commands.ICommand;

import javax.script.ScriptException;

public class SetPrefixCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {

    }

    @Override
    public String getName() {
        return "setprefix";
    }

    @Override
    public String getHelp(String guildID) {
        return null;
    }
}
