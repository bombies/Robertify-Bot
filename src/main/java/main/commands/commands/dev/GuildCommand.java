package main.commands.commands.dev;

import main.commands.CommandContext;
import main.commands.IDevCommand;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.pagination.Pages;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;

public class GuildCommand extends AbstractSlashCommand implements IDevCommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        final var guilds = Robertify.api.getGuilds();
        final List<String> guildNames = new ArrayList<>();

        guildNames.add("🤖 I am in `"+guilds.size()+"` guilds\n");
        for (var guild : guilds) guildNames.add(guild.getName() + " (" + guild.getMembers().size() + " members in cache)");

        GeneralUtils.setCustomEmbed(ctx.getGuild(), "Guilds");
        Pages.paginateMessage(ctx.getChannel(), ctx.getAuthor(), guildNames, 20);
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

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("guilds")
                        .setDescription("See the number of guilds Robertify is in")
                        .setDevCommand()
                        .build()
        );
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!nameCheck(event)) return;

        if (!predicateCheck(event)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "You must be a developer to run this command!")
                    .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "I am in `"+Robertify.api.getGuilds().size()+"` guilds!").build())
                .setEphemeral(true)
                .queue();
    }

    @Override
    public String getHelp() {
        return null;
    }
}
