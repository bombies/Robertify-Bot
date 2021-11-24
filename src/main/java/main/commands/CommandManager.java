package main.commands;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import lombok.Getter;
import main.commands.commands.ITestCommand;
import main.commands.commands.audio.*;
import main.commands.commands.audio.SeekCommand;
import main.commands.commands.dev.ChangeLogCommand;
import main.commands.commands.dev.DeveloperCommand;
import main.commands.commands.dev.EvalCommand;
import main.commands.commands.dev.personal.proj1.SearchArtistCommand;
import main.commands.commands.dev.personal.proj1.SortPopularityCommand;
import main.commands.commands.dev.test.PlaySpotifyURICommand;
import main.commands.commands.dev.test.SpotifyURLToURICommand;
import main.commands.commands.management.*;
import main.commands.commands.management.permissions.RemoveDJCommand;
import main.commands.commands.management.permissions.SetDJCommand;
import main.commands.commands.management.toggles.TogglesCommand;
import main.commands.commands.misc.PingCommand;
import main.commands.commands.dev.config.ViewConfigCommand;
import main.commands.commands.management.permissions.PermissionsCommand;
import main.commands.commands.util.HelpCommand;
import main.commands.commands.util.TutorialCommand;
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
    @Getter
    private final List<ICommand> musicCommands = new ArrayList<>();
    @Getter
    private final List<ICommand> managementCommands = new ArrayList<>();
    @Getter
    private final List<ICommand> miscCommands = new ArrayList<>();


    public CommandManager(EventWaiter waiter) {
        addCommands(
                new PingCommand(),
                new ViewConfigCommand(),
                new EvalCommand(),
                new ChangeLogCommand(),
                new PermissionsCommand(),
                new SearchArtistCommand(),
                new SortPopularityCommand(),
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
                new MoveCommand(),
                new ShuffleCommand(),
                new ClearQueueCommand(),
                new RewindCommand(),
                new SkipToCommand(),
                new LoopCommand(),
                new JumpCommand(),
                new SetDJCommand(),
                new RemoveDJCommand(),
                new TutorialCommand(),
                new TogglesCommand(),
                new ResumeCommand(),
                new VolumeCommand(),
                new SeekCommand(),
                new BanCommand(),
                new UnbanCommand(),

                //Test Commands
                new SpotifyURLToURICommand(),
                new PlaySpotifyURICommand()
        );

        addMusicCommands(
                new PlayCommand(),
                new LeaveCommand(),
                new StopCommand(),
                new SkipCommand(),
                new NowPlayingCommand(),
                new QueueCommand(),
                new PauseCommand(),
                new RemoveCommand(),
                new MoveCommand(),
                new ShuffleCommand(),
                new ClearQueueCommand(),
                new RewindCommand(),
                new SkipToCommand(),
                new LoopCommand(),
                new JumpCommand(),
                new ResumeCommand(),
                new SeekCommand()
        );

        addManagementCommands(
                new PermissionsCommand(),
                new SetChannelCommand(),
                new SetPrefixCommand(),
                new SetDJCommand(),
                new RemoveDJCommand(),
                new TogglesCommand(),
                new BanCommand(),
                new UnbanCommand()
        );

        addMiscCommands(
                new PingCommand()
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

    private void addMusicCommands(ICommand... cmds) {
        for (ICommand cmd : cmds) {
            boolean nameFound = this.musicCommands.stream().anyMatch((it) -> it.getName().equalsIgnoreCase(cmd.getName()));

            if (nameFound)
                throw new IllegalArgumentException("A command with this name already exists!");

            musicCommands.add(cmd);
        }
    }

    private void addManagementCommands(ICommand... cmds) {
        for (ICommand cmd : cmds) {
            boolean nameFound = this.managementCommands.stream().anyMatch((it) -> it.getName().equalsIgnoreCase(cmd.getName()));

            if (nameFound)
                throw new IllegalArgumentException("A command with this name already exists!");

            managementCommands.add(cmd);
        }
    }

    private void addMiscCommands(ICommand... cmds) {
        for (ICommand cmd : cmds) {
            boolean nameFound = this.miscCommands.stream().anyMatch((it) -> it.getName().equalsIgnoreCase(cmd.getName()));

            if (nameFound)
                throw new IllegalArgumentException("A command with this name already exists!");

            miscCommands.add(cmd);
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
            if (cmd instanceof IDevCommand && !(cmd instanceof ITestCommand))
                if (cmd.getName().equals(searchLower) || cmd.getAliases().contains(searchLower))
                    return cmd;
        return null;
    }

    @Nullable
    public List<ICommand> getCommands() {
        List<ICommand> ret = new ArrayList<>();
        for (ICommand cmd : this.commands)
            if (!(cmd instanceof IDevCommand) || !(cmd instanceof ITestCommand))
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
