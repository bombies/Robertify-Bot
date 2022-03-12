package main.commands.prefixcommands;

import me.duncte123.botcommons.commands.ICommandContext;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.List;

@Deprecated
public class CommandContext implements ICommandContext {
    private final GuildMessageReceivedEvent e;
    private final List<String> args;

    public CommandContext(GuildMessageReceivedEvent e, List<String> args) {
        this.e = e;
        this.args = args;
    }

    @Override
    public Guild getGuild() {
        return ICommandContext.super.getGuild();
    }

    @Override
    public GuildMessageReceivedEvent getEvent() {
        return this.e;
    }

    public List<String> getArgs() {
        return args;
    }
}
