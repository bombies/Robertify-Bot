package main.commands.prefixcommands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;

import java.util.List;

@Deprecated
public class CommandContext {
    private final MessageReceivedEvent e;
    private final List<String> args;

    public CommandContext(MessageReceivedEvent e, List<String> args) {
        this.e = e;
        this.args = args;
    }

    public Guild getGuild() {
        return this.getEvent().getGuild();
    }

    public TextChannel getChannel() {
        return this.getEvent().getTextChannel();
    }

    public Message getMessage() {
        return this.getEvent().getMessage();
    }

    public User getAuthor() {
        return this.getEvent().getAuthor();
    }

    public Member getMember() {
        return this.getEvent().getMember();
    }

    public JDA getJDA() {
        return this.getEvent().getJDA();
    }

    public ShardManager getShardManager() {
        return this.getJDA().getShardManager();
    }

    public User getSelfUser() {
        return this.getJDA().getSelfUser();
    }

    public Member getSelfMember() {
        return this.getGuild().getSelfMember();
    }


    public MessageReceivedEvent getEvent() {
        return this.e;
    }

    public List<String> getArgs() {
        return args;
    }
}
