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
import main.utils.votes.VoteManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
                new LofiCommand(),
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
                new ViewConfigCommand(),
                new EvalCommand(),
                new ChangeLogCommand(),
                new GuildCommand(),
                new VoiceChannelCountCommand(),
                new RandomMessageCommand(),
                new ReloadConfigCommand(),
                new AnnouncementCommand(),
                new HostInfoCommand(),
                new StatisticsCommand(),

                //Test Commands
                new SpotifyURLToURICommand(),
                new PlaySpotifyURICommand(),
//                new KotlinTestCommand(),
                new LyricsTestCommand(),
                new MongoTestCommand(),
                new GuildConfigTestCommand(),
                new ImageTestCommand(),
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
                new LofiCommand(),
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

    public void handle(GuildMessageReceivedEvent e) throws ScriptException {
            String[] split = e.getMessage().getContentRaw()
                    .replaceFirst("(?i)" + Pattern.quote(new GuildConfig().getPrefix(e.getGuild().getIdLong())), "")
                    .split("\\s+");

            String invoke = split[0].toLowerCase();
            ICommand cmd = this.getCommand(invoke);

            if (cmd != null) {
                long timeLeft = System.currentTimeMillis() - CooldownManager.INSTANCE.getCooldown(e.getAuthor());
                if (TimeUnit.MILLISECONDS.toSeconds(timeLeft) >= CooldownManager.DEFAULT_COOLDOWN) {
                    if (cmd.requiresPermission())
                        if (!hasAllPermissions(cmd, e.getGuild().getSelfMember())) {
                            final var permissionsRequired = cmd.getPermissionsRequired();
                            e.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(e.getGuild(), "I do not have enough permissions to do this\n" +
                                                    "Please give my role the following permission(s):\n\n" +
                                                    "`"+GeneralUtils.listToString(permissionsRequired)+"`\n\n" +
                                                    "*For the recommended permissions please invite the bot by clicking the button below*")
                                            .build())
                                    .setActionRow(
                                            Button.of(ButtonStyle.LINK, "https://discord.com/oauth2/authorize?client_id=893558050504466482&permissions=269479308656&scope=bot%20applications.commands", "Give Permissions! (Requires Manage Server)", RobertifyTheme.LIGHT.getEmoji())
                                    )
                                    .queue();
                            return;
                        }

                    final List<String> args = Arrays.asList(split).subList(1, split.length);
                    final CommandContext ctx = new CommandContext(e, args);
                    final Guild guild = e.getGuild();
                    final Message msg = e.getMessage();
                    final var toggles = new TogglesConfig();

                    if (!guild.getSelfMember().hasPermission(ctx.getChannel(), net.dv8tion.jda.api.Permission.MESSAGE_EMBED_LINKS)) {
                        e.getChannel().sendMessage("""
                                    ⚠️ I do not have permissions to send embeds!

                                    Please enable the `Embed Links` permission for my role in this channel in order for my commands to work!""")
                                .queue();
                        return;
                    }

                    if (toggles.getToggle(guild, Toggles.RESTRICTED_TEXT_CHANNELS)) {
                        if (!GeneralUtils.hasPerms(guild, ctx.getMember(), Permission.ROBERTIFY_ADMIN)) {
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

                    if (cmd.isPremiumCommand() && Robertify.getTopGGAPI() != null) {
                        if (!new VoteManager().userVoted(ctx.getAuthor().getId(), VoteManager.Website.TOP_GG)
                            && ctx.getAuthor().getIdLong() != 276778018440085505L) {
                            msg.replyEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(guild,
                                    "🔒 Locked Command", """
                                                    Woah there! You must vote before interacting with this command.
                                                    Click on each of the buttons below to vote!

                                                    *Note: Only the first two votes sites are required, the last two are optional!*""").build())
                                    .setActionRow(
                                            Button.of(ButtonStyle.LINK, "https://top.gg/bot/893558050504466482/vote", "Top.gg"),
                                            Button.of(ButtonStyle.LINK, "https://discordbotlist.com/bots/robertify/upvote", "Discord Bot List"),
                                            Button.of(ButtonStyle.LINK, "https://discords.com/bots/bot/893558050504466482/vote", "Discords.com")
                                    )
                                    .queue();
                            return;
                        }
                    }

                    BotBDCache botDB = BotBDCache.getInstance();
                    String latestAlert = botDB.getLatestAlert().getLeft();
                    if (!botDB.userHasViewedAlert(ctx.getAuthor().getIdLong()) && (!latestAlert.isEmpty() && !latestAlert.isBlank()))
                        msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "⚠️ You have an unread alert!\n" +
                                "Run the `/alert` command to view this alert.").build()).queue();

                    if (commandTypeHasCommandWithName(CommandType.MUSIC, cmd.getName()))
                        new RandomMessageManager().randomlySendMessage(ctx.getChannel());

                    if (toggles.isDJToggleSet(guild, cmd)) {
                        if (toggles.getDJToggle(guild, cmd) && !cmd.getName().equals("skip")) {
                            if (GeneralUtils.hasPerms(guild, ctx.getMember(), Permission.ROBERTIFY_DJ)) {
                                cmd.handle(ctx);
//                            if (!(cmd instanceof StatisticsCommand))
//                                StatisticsManager.ins().incrementStatistic(1, Statistic.COMMANDS_USED);
                            } else {
                                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be a DJ" +
                                                " to run this command!").build())
                                        .queue();
                            }
                        } else {
                            cmd.handle(ctx);
//                        if (!(cmd instanceof StatisticsCommand))
//                            StatisticsManager.ins().incrementStatistic(1, Statistic.COMMANDS_USED);
                        }
                    } else {
                        cmd.handle(ctx);
//                    if (!(cmd instanceof StatisticsCommand))
//                        StatisticsManager.ins().incrementStatistic(1, Statistic.COMMANDS_USED);
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
