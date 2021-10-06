package main.commands.commands.misc;

import main.commands.CommandContext;
import main.commands.ICommand;

import javax.script.ScriptException;

public class LyricsCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {

    }

    @Override
    public String getName() {
        return "lyrics";
    }

    @Override
    public String getHelp(String guildID) {
        return null;
    }
}
