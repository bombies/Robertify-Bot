package main.commands;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import lombok.Getter;
import main.commands.commands.dev.*;
import main.commands.commands.dev.test.*;
import main.commands.commands.audio.*;
import main.commands.commands.audio.SeekCommand;
import main.commands.commands.management.*;
import main.commands.commands.management.dedicatechannel.DedicatedChannelCommand;
import main.commands.commands.management.permissions.*;
import main.commands.commands.management.TogglesCommand;
import main.commands.commands.util.*;
import main.constants.Permission;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.restrictedchannels.RestrictedChannelsConfig;
import main.constants.Toggles;
import main.commands.commands.misc.EightBallCommand;
import main.commands.commands.misc.PingCommand;
import main.commands.commands.dev.ViewConfigCommand;
import main.commands.commands.misc.poll.PollCommand;
import main.commands.commands.util.reports.ReportsCommand;
import main.utils.GeneralUtils;
import main.utils.json.toggles.TogglesConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class CommandManager {
    private final static Logger logger = LoggerFactory.getLogger(CommandManager.class);

    private final List<ICommand> commands = new ArrayList<>();
    @Getter
    private final List<ICommand> musicCommands = new ArrayList<>();
    @Getter
    private final List<ICommand> managementCommands = new ArrayList<>();
    @Getter
    private final List<ICommand> miscCommands = new ArrayList<>();
    @Getter
    private final List<ICommand> utilityCommands = new ArrayList<>();


    public CommandManager(EventWaiter waiter) {
        addCommands(
                new PingCommand(),
                new PermissionsCommand(),
                new HelpCommand(),
                new SetPrefixCommand(),
                new PlayCommand(),
                new DisconnectCommand(),
                new StopCommand(),
                new SkipCommand(),
                new NowPlayingCommand(),
                new QueueCommand(),
                new PauseCommand(),
                new RobertifyShutdownCommand(),
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
                new DedicatedChannelCommand(),
                new EightBallCommand(),
                new PreviousTrackCommand(),
                new PollCommand(),
                new JoinCommand(),
                new RestrictedChannelsCommand(),
                new SuggestionCommand(),
                new ReportsCommand(),
                new BotInfoCommand(),
                new ListDJCommand(),
                new LofiCommand(),
                new UptimeCommand(),
                new SupportServerCommand(),
                new ShufflePlayCommand(),
                new VoteCommand(),
//                new LyricsCommand(),

                //Dev Commands
                new UpdateCommand(),
                new DeveloperCommand(),
                new ViewConfigCommand(),
                new EvalCommand(),
                new ChangeLogCommand(),
                new GuildCommand(),
                new VoiceChannelCountCommand(),
                new MongoMigrationCommand(),
                new RandomMessageCommand(),
                new ReloadConfigCommand(),
                new AnnouncementCommand(),

                //Test Commands
                new SpotifyURLToURICommand(),
                new PlaySpotifyURICommand(),
//                new KotlinTestCommand(),
                new LoadSpotifyPlaylistCommand(),
                new LyricsTestCommand(),
                new MongoTestCommand(),
                new GuildConfigTestCommand(),
                new ImageTestCommand()
        );

        addMusicCommands(
                new PlayCommand(),
                new ShufflePlayCommand(),
                new DisconnectCommand(),
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
                new SeekCommand(),
                new PreviousTrackCommand(),
                new JoinCommand(),
                new LofiCommand()
        );

        addManagementCommands(
                new PermissionsCommand(),
                new SetChannelCommand(),
                new SetPrefixCommand(),
                new SetDJCommand(),
                new RemoveDJCommand(),
                new ListDJCommand(),
                new TogglesCommand(),
                new BanCommand(),
                new UnbanCommand(),
                new DedicatedChannelCommand(),
                new RestrictedChannelsCommand()
        );

        addMiscCommands(
                new PingCommand(),
                new EightBallCommand(),
                new PollCommand()
        );

        addUtilityCommands(
                new TutorialCommand(),
                new HelpCommand(),
                new SuggestionCommand(),
                new ReportsCommand(),
                new BotInfoCommand(),
                new UptimeCommand(),
                new SupportServerCommand()
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

    private void addUtilityCommands(ICommand... cmds) {
        for (ICommand cmd : cmds) {
            boolean nameFound = this.utilityCommands.stream().anyMatch((it) -> it.getName().equalsIgnoreCase(cmd.getName()));

            if (nameFound)
                throw new IllegalArgumentException("A command with this name already exists!");

            utilityCommands.add(cmd);
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
                    .replaceFirst("(?i)" + Pattern.quote(new GuildConfig().getPrefix(e.getGuild().getIdLong())), "")
                    .split("\\s+");

            String invoke = split[0].toLowerCase();
            ICommand cmd = this.getCommand(invoke);

            if (cmd != null) {
                if (cmd.requiresPermission())
                    if (!hasAllPermissions(cmd, e.getGuild().getSelfMember())) {
                        final var permissionsRequired = cmd.getPermissionsRequired();
                        e.getMessage().replyEmbeds(EmbedUtils.embedMessage("I do not have enough permissions to do this\n" +
                                "Please give my role the following permission(s):\n\n" +
                                        "`"+GeneralUtils.listToString(permissionsRequired)+"`\n\n" +
                                        "*For the recommended permissions please invite the bot using this link: https://bit.ly/3DfaNNl*")
                                        .build())
                                .queue();
                        return;
                    }

                final List<String> args = Arrays.asList(split).subList(1, split.length);
                final CommandContext ctx = new CommandContext(e, args);
                final Guild guild = e.getGuild();
                final Message msg = e.getMessage();
                final var toggles = new TogglesConfig();

                if (!guild.getSelfMember().hasPermission(net.dv8tion.jda.api.Permission.MESSAGE_EMBED_LINKS)) {
                    e.getChannel().sendMessage("""
                                    ⚠️ I do not have permissions to send embeds!

                                    Please enable the `Embed Links` permission for my role in this channel in order for my commands to work!""")
                            .queue();
                    return;
                }

                if (toggles.getToggle(guild, Toggles.RESTRICTED_TEXT_CHANNELS)) {
                    if (!GeneralUtils.hasPerms(guild, ctx.getAuthor(), Permission.ROBERTIFY_ADMIN)) {
                        final var rcConfig = new RestrictedChannelsConfig();
                        if (!rcConfig.isRestrictedChannel(
                                guild.getIdLong(),
                                msg.getTextChannel().getIdLong(),
                                RestrictedChannelsConfig.ChannelType.TEXT_CHANNEL
                        )) {
                            return;
                        }
                    }
                }

                if (getMusicCommands().contains(cmd))
                    new RandomMessageManager().randomlySendMessage(ctx.getChannel());

                if (toggles.isDJToggleSet(guild, cmd)) {
                    if (toggles.getDJToggle(guild, cmd)) {
                        if (GeneralUtils.hasPerms(guild, ctx.getAuthor(), Permission.ROBERTIFY_DJ)) {
                            cmd.handle(ctx);
                        } else {
                            msg.replyEmbeds(EmbedUtils.embedMessage("You must be a DJ" +
                                            " to run this command!").build())
                                    .queue();
                        }
                    } else cmd.handle(ctx);
                } else cmd.handle(ctx);
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

    public boolean hasAllPermissions(ICommand cmd, Member selfMember) {
        for (var perm : cmd.getPermissionsRequired())
            if (!selfMember.hasPermission(perm))
                return false;
        return true;
    }
}
