package main.commands.slashcommands.commands.dev;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.IDevCommand;
import main.main.Robertify;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.pagination.PaginationHandler;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;

public class GuildCommand extends AbstractSlashCommand implements IDevCommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        final var guilds = Robertify.shardManager.getGuildCache();
        final List<String> guildNames = new ArrayList<>();

        guildNames.add("🤖 I am in `"+guilds.size()+"` guilds\n");
        for (var guild : guilds) guildNames.add(guild.getName() + " (" + guild.getMembers().size() + " members in cache)");

        PaginationHandler.paginateMessage(ctx.getChannel(), ctx.getAuthor(), guildNames, 20);
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
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!devCheck(event)) return;

        if (!predicateCheck(event)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "You must be a developer to run this command!")
                    .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "I am in `"+Robertify.shardManager.getGuilds().size()+"` guilds!").build())
                .setEphemeral(true)
                .queue();
    }

    @Override
    public String getHelp() {
        return null;
    }
}
