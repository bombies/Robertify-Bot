package main.commands.commands.dev;

import main.commands.CommandContext;
import main.commands.IDevCommand;

import javax.script.ScriptException;
import java.util.List;

public class VoiceChannelCountCommand implements IDevCommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx))
    }

    @Override
    public String getName() {
        return "voicechannelcount";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return List.of("vccount", "vcc");
    }
}
