package main.commands.prefixcommands;

import lombok.Getter;
import main.commands.RandomMessageManager;
import main.commands.prefixcommands.audio.*;
import main.commands.slashcommands.commands.audio.*;
import main.commands.slashcommands.commands.audio.filters.*;
import main.commands.prefixcommands.dev.*;
import main.commands.prefixcommands.dev.test.*;
import main.commands.prefixcommands.management.*;
import main.commands.slashcommands.commands.management.*;
import main.commands.slashcommands.commands.management.dedicatedchannel.DedicatedChannelCommand;
import main.commands.slashcommands.commands.management.permissions.ListDJCommand;
import main.commands.slashcommands.commands.management.permissions.PermissionsCommand;
import main.commands.slashcommands.commands.management.permissions.RemoveDJCommand;
import main.commands.slashcommands.commands.management.permissions.SetDJCommand;
import main.commands.slashcommands.commands.misc.EightBallCommand;
import main.commands.slashcommands.commands.misc.PingCommand;
import main.commands.slashcommands.commands.misc.PlaytimeCommand;
import main.commands.slashcommands.commands.misc.poll.PollCommand;
import main.commands.slashcommands.commands.misc.reminders.RemindersCommand;
import main.commands.prefixcommands.util.*;
import main.commands.prefixcommands.util.reports.ReportsCommand;
import main.commands.slashcommands.commands.dev.*;
import main.commands.slashcommands.commands.util.*;
import main.constants.Permission;
import main.constants.RobertifyTheme;
import main.constants.Toggles;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.database.mongodb.cache.BotBDCache;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.restrictedchannels.RestrictedChannelsConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import main.utils.votes.VoteManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Deprecated
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

    public CommandManager() {
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
                new UptimeCommand(),
                new SupportServerCommand(),
                new ShufflePlayCommand(),
                new VoteCommand(),
                new LyricsCommand(),
                new DonateCommand(),
                new ThemeCommand(),
                new WebsiteCommand(),
                new FavouriteTracksCommand(),
                new KaraokeFilter(),
                new NightcoreFilter(),
                new EightDFilter(),
                new TremoloFilter(),
                new VibratoFilter(),
                new TwentyFourSevenCommand(),
                new PlaytimeCommand(),
                new SearchCommand(),
                new LogCommand(),
                new AutoPlayCommand(),
                new SetLogChannelCommand(),
                new RemindersCommand(),

                //Dev Commands
                new UpdateCommand(),
                new DeveloperCommand(),
                new EvalCommand(),
                new GuildCommand(),
                new NodeInfoCommand(),
                new RandomMessageCommand(),
                new ReloadConfigCommand(),
                new HostInfoCommand(),
                new StatisticsCommand(),

                //Test Commands
                new MenuPaginationTestCommand()
        );

        addMusicCommands(
                new PlayCommand(),
                new ShufflePlayCommand(),
                new DisconnectCommand(),
                new StopCommand(),
                new SkipCommand(),
                new NowPlayingCommand(),
                new QueueCommand(),
                new VolumeCommand(),
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
                new LyricsCommand(),
                new FavouriteTracksCommand(),
                new KaraokeFilter(),
                new NightcoreFilter(),
                new EightDFilter(),
                new TremoloFilter(),
                new VibratoFilter(),
                new SearchCommand(),
                new AutoPlayCommand()
        );

        addManagementCommands(
                new PermissionsCommand(),
                new SetPrefixCommand(),
                new SetDJCommand(),
                new RemoveDJCommand(),
                new ListDJCommand(),
                new TogglesCommand(),
                new BanCommand(),
                new UnbanCommand(),
                new DedicatedChannelCommand(),
                new RestrictedChannelsCommand(),
                new ThemeCommand(),
                new TwentyFourSevenCommand(),
                new LogCommand(),
                new SetLogChannelCommand()
        );

        addMiscCommands(
                new PingCommand(),
                new EightBallCommand(),
                new PollCommand(),
                new PlaytimeCommand(),
                new RemindersCommand()
        );

        addUtilityCommands(
                new TutorialCommand(),
                new HelpCommand(),
                new SuggestionCommand(),
                new ReportsCommand(),
                new BotInfoCommand(),
                new UptimeCommand(),
                new SupportServerCommand(),
                new DonateCommand(),
                new VoteCommand(),
                new WebsiteCommand()
        );
    }

    public boolean isMusicCommand(ICommand cmd) {
        return getMusicCommands()
                .stream()
                .anyMatch(it -> it.getName().equalsIgnoreCase(cmd.getName()));
    }

    public boolean isMiscCommand(ICommand cmd) {
        return getMiscCommands()
                .stream()
                .anyMatch(it -> it.getName().equalsIgnoreCase(cmd.getName()));
    }

    public boolean isManagementCommand(ICommand cmd) {
        return getManagementCommands()
                .stream()
                .anyMatch(it -> it.getName().equalsIgnoreCase(cmd.getName()));
    }

    public boolean isUtilityCommand(ICommand cmd) {
        return getUtilityCommands()
                .stream()
                .anyMatch(it -> it.getName().equalsIgnoreCase(cmd.getName()));
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

    public void handle(MessageReceivedEvent e) throws ScriptException {
            String[] split = e.getMessage().getContentRaw()
                    .replaceFirst("(?i)" + Pattern.quote(new GuildConfig(e.getGuild()).getPrefix()), "")
                    .split("\\s+");

            String invoke = split[0].toLowerCase();
            ICommand cmd = this.getCommand(invoke);

            if (cmd != null) {
                long timeLeft = System.currentTimeMillis() - CooldownManager.INSTANCE.getCooldown(e.getAuthor());
                if (TimeUnit.MILLISECONDS.toSeconds(timeLeft) >= CooldownManager.DEFAULT_COOLDOWN) {
                    if (cmd.requiresPermission())
                        if (!hasAllPermissions(cmd, e.getGuild().getSelfMember())) {
                            final var permissionsRequired = cmd.getPermissionsRequired();
                            e.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(e.getGuild(),
                                                    RobertifyLocaleMessage.GeneralMessages.SELF_INSUFFICIENT_PERMS_ARGS,
                                                    Pair.of("{permissions}", GeneralUtils.listToString(permissionsRequired))
                                            )
                                            .build())
                                    .queue();
                            return;
                        }

                    final List<String> args = Arrays.asList(split).subList(1, split.length);
                    final CommandContext ctx = new CommandContext(e, args);
                    final Guild guild = e.getGuild();
                    final Message msg = e.getMessage();
                    final var toggles = new TogglesConfig(guild);
                    final var localeManager = LocaleManager.getLocaleManager(guild);

                    if (!guild.getSelfMember().hasPermission(ctx.getChannel(), net.dv8tion.jda.api.Permission.MESSAGE_EMBED_LINKS)) {
                        e.getChannel().sendMessage(localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.NO_EMBED_PERMS))
                                .queue();
                        return;
                    }

                    if (toggles.getToggle(Toggles.RESTRICTED_TEXT_CHANNELS)) {
                        if (!GeneralUtils.hasPerms(guild, ctx.getMember(), Permission.ROBERTIFY_ADMIN)) {
                            final var rcConfig = new RestrictedChannelsConfig(guild);
                            if (!rcConfig.isRestrictedChannel(
                                    msg.getChannel().asGuildMessageChannel().getIdLong(),
                                    RestrictedChannelsConfig.ChannelType.TEXT_CHANNEL
                            )) {
                                return;
                            }
                        }
                    }

                    if (cmd.isPremiumCommand())
                        if (!GeneralUtils.checkPremium(guild, ctx.getAuthor(), msg) && ctx.getAuthor().getIdLong() != 276778018440085505L)
                            return;

                    BotBDCache botDB = BotBDCache.getInstance();
                    String latestAlert = botDB.getLatestAlert().getLeft();
                    if (!botDB.userHasViewedAlert(ctx.getAuthor().getIdLong()) && (!latestAlert.isEmpty() && !latestAlert.isBlank())
                     && isMusicCommand(cmd))
                        msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.UNREAD_ALERT).build()).queue();

                    if (commandTypeHasCommandWithName(CommandType.MUSIC, cmd.getName()))
                        new RandomMessageManager().randomlySendMessage(ctx.getChannel());

                    if (toggles.isDJToggleSet(cmd)) {
                        if (toggles.getDJToggle(cmd) && !cmd.getName().equals("skip")) {
                            if (GeneralUtils.hasPerms(guild, ctx.getMember(), Permission.ROBERTIFY_DJ)) {
                                cmd.handle(ctx);
                            } else {
                                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DJ_ONLY).build())
                                        .queue();
                            }
                        } else {
                            cmd.handle(ctx);
                        }
                    } else {
                        cmd.handle(ctx);
                    }
                    CooldownManager.INSTANCE.setCooldown(e.getAuthor(), System.currentTimeMillis());
                } else {
                    long time_left = CooldownManager.DEFAULT_COOLDOWN - TimeUnit.MILLISECONDS.toSeconds(timeLeft);
                    EmbedBuilder eb = RobertifyEmbedUtils.embedMessageWithTitle(e.getGuild(), "⚠  Slow down!",
                            "You must wait `" + time_left
                                    + " " + ((time_left <= 1) ? "second`" : "seconds`") + " before running another command!");
                    e.getMessage().replyEmbeds(eb.build()).queue(m -> m.delete().queueAfter(5, TimeUnit.SECONDS));
                }
            }
    }

    public boolean hasAllPermissions(ICommand cmd, Member selfMember) {
        for (var perm : cmd.getPermissionsRequired())
            if (!selfMember.hasPermission(perm))
                return false;
        return true;
    }

    private boolean commandTypeHasCommandWithName(CommandType type, String name) {
        List<ICommand> listToUse = new ArrayList<>();

        switch (type) {
            case MANAGEMENT -> listToUse = getManagementCommands();
            case MUSIC -> listToUse = getMusicCommands();
            case MISC -> listToUse = getMiscCommands();
            case UTILITY -> listToUse = getUtilityCommands();
        }

        for (var cmd : listToUse)
            if (cmd.getName().equals(name))
                return true;
        return false;
    }

    private enum CommandType {
        MANAGEMENT,
        MUSIC,
        MISC,
        UTILITY
    }
}
