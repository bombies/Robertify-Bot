package main.commands.commands.dev.test;

import main.audiohandlers.PlayerManager;
import main.commands.CommandContext;
import main.commands.commands.ITestCommand;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.List;

public class LazyQueueAndPlayCommand implements ITestCommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();

        if (args.isEmpty()) {
            msg.reply("Provide args").queue();
            return;
        }


        PlayerManager.getInstance()
                .lazyLoadAndPlay(args.get(0), ctx.getChannel(), ctx.getSelfMember().getVoiceState(), ctx.getMember().getVoiceState(), ctx);

    }

    @Override
    public String getName() {
        return "lazyplay";
    }

    @Override
    public String getHelp(String guildID) {
        return "lzp";
    }

    @Override
    public List<String> getAliases() {
        return List.of("lzp");
    }
}
