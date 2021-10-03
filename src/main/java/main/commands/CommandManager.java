package main.commands;

import lombok.Getter;
import main.commands.commands.audio.*;
import main.commands.commands.dev.DeveloperCommand;
import main.commands.commands.misc.PingCommand;
import main.commands.commands.dev.config.ViewConfigCommand;
import main.commands.commands.management.permissions.PermissionsCommand;
import main.commands.commands.management.SetChannelCommand;
import main.commands.commands.management.SetPrefixCommand;
import main.commands.commands.util.HelpCommand;
import main.commands.commands.management.ShutdownCommand;
import main.utils.database.ServerUtils;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import javax.annotation.Nullable;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class CommandManager {
    @Getter
    private final List<ICommand> commands = new ArrayList<>();

    public CommandManager() {
        addCommands(
                new PingCommand(),
                new ViewConfigCommand(),
                new PermissionsCommand(),
                new HelpCommand(),
                new SetPrefixCommand(),
                new PlayCommand(),
                new LeaveCommand(),
                new StopCommand(),
                new SkipCommand(),
                new NowPlayingCommand(),
                new QueueCommand(),
                new PauseCommand(),
                new ShutdownCommand(),
                new DeveloperCommand(),
                new SetChannelCommand(),
                new RemoveCommand(),
                new MoveCommand()
        );
    }

    private void addCommands(ICommand... cmds) {
        for (ICommand cmd : cmds) {
            boolean nameFound = this.commands.stream().anyMatch((it) -> it.getName().equalsIgnoreCase(cmd.getName()));

            if (nameFound)
                throw new IllegalArgumentException("A command with this name already exists!");

            commands.add(cmd);
        }
    }

    @Nullable
    public ICommand getCommand(String search) {
        String searchLower = search.toLowerCase();

        for (ICommand cmd : this.commands)
            if (cmd.getName().equals(searchLower) || cmd.getAliases().contains(searchLower))
                return cmd;
        return null;
    }

    @Nullable
    public ICommand getTestCommand(String search) {
        String searchLower = search.toLowerCase();

        for (ICommand cmd : this.commands)
            if (cmd instanceof IDevCommand)
                if (cmd.getName().equals(searchLower) || cmd.getAliases().contains(searchLower))
                    return cmd;
        return null;
    }

    @Nullable
    public List<ICommand> getCommands() {
        List<ICommand> ret = new ArrayList<>();
        for (ICommand cmd : this.commands)
            if (!(cmd instanceof IDevCommand))
                ret.add(cmd);
        return  ret;
    }

    @Nullable
    public List<ICommand> getDevCommands() {
        List<ICommand> ret = new ArrayList<>();
        for (ICommand cmd : this.commands)
            if (cmd instanceof IDevCommand)
                ret.add(cmd);
        return ret;
    }

    public void handle(GuildMessageReceivedEvent e) throws ScriptException {
        long timeLeft = System.currentTimeMillis() - CooldownManager.INSTANCE.getCooldown(e.getAuthor());
        if (TimeUnit.MILLISECONDS.toSeconds(timeLeft) >= CooldownManager.DEFAULT_COOLDOWN) {
            String[] split = e.getMessage().getContentRaw()
                    .replaceFirst("(?i)" + Pattern.quote(ServerUtils.getPrefix(e.getGuild().getIdLong())), "")
                    .split("\\s+");

            String invoke = split[0].toLowerCase();
            ICommand cmd = this.getCommand(invoke);

            if (cmd != null) {
                List<String> args = Arrays.asList(split).subList(1, split.length);
                CommandContext ctx = new CommandContext(e, args);

                cmd.handle(ctx);
            }
            CooldownManager.INSTANCE.setCooldown(e.getAuthor(), System.currentTimeMillis());
        } else {
            long time_left = CooldownManager.DEFAULT_COOLDOWN - TimeUnit.MILLISECONDS.toSeconds(timeLeft);
            EmbedBuilder eb = EmbedUtils.embedMessageWithTitle("⚠  Slow down!",
                    "You must wait `" + time_left
                            + " " + ((time_left <= 1) ? "second`" : "seconds`") + " before running another command!");
            e.getMessage().replyEmbeds(eb.build()).queue(m -> m.delete().queueAfter(5, TimeUnit.SECONDS));
        }
    }
}
