package main.commands.prefixcommands.dev.test;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ITestCommand;
import main.exceptions.InvalidSpotifyURIException;
import main.utils.spotify.SpotifyURI;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.List;

public class SpotifyURLToURICommand implements ITestCommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx))
            return;

        if (ctx.getArgs().isEmpty()) {
            ctx.getMessage().reply("Provide a link.").queue();
            return;
        }

        Message msg = ctx.getMessage();

        try {
            var uri = SpotifyURI.parse(ctx.getArgs().get(0));
            msg.reply("ID: " + uri.getId() +  "\nTYPE: " + uri.getType().name()).queue();
        } catch (InvalidSpotifyURIException e) {
            msg.reply(e.getMessage()).queue();
        }
    }

    @Override
    public String getName() {
        return "spoturitourl";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return List.of("sutu");
    }
}
