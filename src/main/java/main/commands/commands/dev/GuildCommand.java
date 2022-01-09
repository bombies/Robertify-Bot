package main.commands.commands.dev;

import main.commands.CommandContext;
import main.commands.IDevCommand;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.pagination.Pages;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;

public class GuildCommand implements IDevCommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        final var guilds = Robertify.api.getGuilds();
        final List<String> guildNames = new ArrayList<>();

        guildNames.add("🤖 I am in `"+guilds.size()+"` guilds\n");
        for (var guild : guilds) guildNames.add(guild.getName() + " (" + guild.getMembers().size() + " members in cache)");

        GeneralUtils.setCustomEmbed(ctx.getGuild(), "Guilds");
        Pages.paginate(ctx.getChannel(), ctx.getAuthor(), guildNames, 20);
        GeneralUtils.setDefaultEmbed(ctx.getGuild());
    }

    @Override
    public String getName() {
        return "guilds";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return List.of("g");
    }
}
