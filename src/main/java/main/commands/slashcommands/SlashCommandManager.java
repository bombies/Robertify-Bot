package main.commands.slashcommands;

import lombok.Getter;
import main.commands.slashcommands.commands.audio.*;
import main.commands.slashcommands.commands.audio.filters.*;
import main.commands.slashcommands.commands.dev.*;
import main.commands.slashcommands.commands.dev.test.ImageBuilderTest;
import main.commands.slashcommands.commands.management.*;
import main.commands.slashcommands.commands.management.requestchannel.RequestChannelCommand;
import main.commands.slashcommands.commands.management.requestchannel.RequestChannelEditCommand;
import main.commands.slashcommands.commands.management.permissions.ListDJCommand;
import main.commands.slashcommands.commands.management.permissions.PermissionsCommand;
import main.commands.slashcommands.commands.management.permissions.RemoveDJCommand;
import main.commands.slashcommands.commands.management.permissions.SetDJCommand;
import main.commands.slashcommands.commands.misc.EightBallCommand;
import main.commands.slashcommands.commands.misc.PingCommand;
import main.commands.slashcommands.commands.misc.PlaytimeCommand;
import main.commands.slashcommands.commands.misc.poll.PollCommand;
import main.commands.slashcommands.commands.misc.reminders.RemindersCommand;
import main.commands.slashcommands.commands.util.*;
import main.utils.component.interactions.AbstractSlashCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SlashCommandManager {
    private final static SlashCommandManager STATIC_INSTANCE = new SlashCommandManager();
    
    @Getter
    private final List<AbstractSlashCommand> musicCommands = new ArrayList<>();
    @Getter
    private final List<AbstractSlashCommand> managementCommands = new ArrayList<>();
    @Getter
    private final List<AbstractSlashCommand> miscCommands = new ArrayList<>();
    @Getter
    private final List<AbstractSlashCommand> utilityCommands = new ArrayList<>();
    @Getter
    private final List<AbstractSlashCommand> devCommands = new ArrayList<>();

    private SlashCommandManager() {
        addMusicCommands(
                new PlaySlashCommand(),
                new DisconnectSlashCommand(),
                new ClearQueueSlashCommand(),
                new JoinSlashCommand(),
                new JumpSlashCommand(),
                new LoopSlashCommand(),
                new MoveSlashCommand(),
                new NowPlayingSlashCommand(),
                new PauseSlashCommand(),
                new QueueSlashCommand(),
                new RemoveSlashCommand(),
                new RewindSlashCommand(),
                new SeekSlashCommand(),
                new ShufflePlaySlashCommand(),
                new ShuffleSlashCommand(),
                new SkipSlashCommand(),
                new VolumeSlashCommand(),
                new FavouriteTracksCommand(),
                new TwentyFourSevenCommand(),
                new AutoPlayCommand(),
                new SearchCommand(),
                new ResumeCommand(),
                new PreviousTrackCommand(),
                new LyricsCommand(),
                new KaraokeFilter(),
                new NightcoreFilter(),
                new EightDFilter(),
                new TremoloFilter(),
                new VibratoFilter(),
                new StopCommand(),
                new HistoryCommand()
        );

        addManagementCommands(
                new SetDJCommand(),
                new RemoveDJCommand(),
                new ListDJCommand(),
                new BanCommand(),
                new UnbanCommand(),
                new PermissionsCommand(),
                new TogglesCommand(),
                new RequestChannelCommand(),
                new RequestChannelEditCommand(),
                new ThemeCommand(),
                new RestrictedChannelsCommand(),
                new LogCommand(),
                new SetLogChannelCommand(),
                new LanguageCommand(),
                new PremiumCommand()
        );

        addMiscCommands(
                new EightBallCommand(),
                new RemindersCommand(),
                new PollCommand()
        );

        addUtilityCommands(
                new PingCommand(),
                new VoteCommand(),
                new UptimeCommand(),
                new SupportServerCommand(),
                new WebsiteCommand(),
                new DonateCommand(),
                new SuggestionCommand(),
                new BotInfoCommand(),
                new PlaytimeCommand(),
                new HelpCommand(),
                new AlertCommand()
        );

        addDevCommands(
                new GuildCommand(),
                new NodeInfoCommand(),
                new UpdateCommand(),
                new EvalCommand(),
                new RandomMessageCommand(),
                new ReloadConfigCommand(),
                new ShardInfoCommand(),
                new SendAlertCommand(),
                new PostCommandInfoCommand(),
                new ManagePremiumCommand(),
                new ResetPremiumFeaturesCommand(),
                new ImageBuilderTest(),
                new UnloadGuildCommandsCommand()
        );
    }
    
    public static SlashCommandManager getInstance() {
        return STATIC_INSTANCE;
    }

    private void addMusicCommands(AbstractSlashCommand... commands) {
        musicCommands.addAll(Arrays.asList(commands));
    }

    private void addManagementCommands(AbstractSlashCommand... commands) {
        managementCommands.addAll(Arrays.asList(commands));
    }

    private void addMiscCommands(AbstractSlashCommand... commands) {
        miscCommands.addAll(Arrays.asList(commands));
    }

    private void addUtilityCommands(AbstractSlashCommand... commands) {
        utilityCommands.addAll(Arrays.asList(commands));
    }

    private void addDevCommands(AbstractSlashCommand... commands) {
        devCommands.addAll(Arrays.asList(commands));
    }

    public List<String> getCommandNames(List<AbstractSlashCommand> commands) {
        final List<String> ret = new ArrayList<>();
        for (var cmd : commands)
            ret.add(cmd.getName());
        return ret;
    }

    public boolean isMusicCommand(AbstractSlashCommand command) {
        return getMusicCommands()
                .stream()
                .anyMatch(it -> it.getName().equalsIgnoreCase(command.getName()));
    }

    public boolean isManagementCommand(AbstractSlashCommand command) {
        return getManagementCommands()
                .stream()
                .anyMatch(it -> it.getName().equalsIgnoreCase(command.getName()));
    }

    public boolean isMiscCommand(AbstractSlashCommand command) {
        return getMiscCommands()
                .stream()
                .anyMatch(it -> it.getName().equalsIgnoreCase(command.getName()));
    }

    public boolean isUtilityCommand(AbstractSlashCommand command) {
        return getUtilityCommands()
                .stream()
                .anyMatch(it -> it.getName().equalsIgnoreCase(command.getName()));
    }

    public boolean isDevCommand(AbstractSlashCommand command) {
        return getDevCommands()
                .stream()
                .anyMatch(it -> it.getName().equalsIgnoreCase(command.getName()));
    }

    public boolean isDevCommand(String name) {
        return getDevCommands()
                .stream()
                .anyMatch(it -> it.getName().equalsIgnoreCase(name));
    }


    public List<AbstractSlashCommand> getGlobalCommands() {
        final List<AbstractSlashCommand> abstractSlashCommands = new ArrayList<>();
        abstractSlashCommands.addAll(musicCommands);
        abstractSlashCommands.addAll(managementCommands);
        abstractSlashCommands.addAll(miscCommands);
        abstractSlashCommands.addAll(utilityCommands);
        return abstractSlashCommands.stream()
                .filter(command -> !command.isGuildCommand())
                .toList();
    }

    public List<AbstractSlashCommand> getGuildCommands() {
        final List<AbstractSlashCommand> abstractSlashCommands = new ArrayList<>();
        abstractSlashCommands.addAll(musicCommands);
        abstractSlashCommands.addAll(managementCommands);
        abstractSlashCommands.addAll(miscCommands);
        abstractSlashCommands.addAll(utilityCommands);
        return abstractSlashCommands.stream()
                .filter(AbstractSlashCommand::isGuildCommand)
                .toList();
    }

    public AbstractSlashCommand getCommand(String name) {
        return getGlobalCommands().stream()
                .filter(cmd -> cmd.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public AbstractSlashCommand getDevCommand(String name) {
        return getDevCommands().stream()
                .filter(cmd -> cmd.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public CommandType getCommandType(AbstractSlashCommand command) {
        if (isMusicCommand(command))
            return CommandType.MUSIC;
        if (isManagementCommand(command))
            return CommandType.MANAGEMENT;
        if (isMiscCommand(command))
            return CommandType.MISCELLANEOUS;
        if (isUtilityCommand(command))
            return CommandType.UTILITY;
        return null;
    }

    public enum CommandType {
        MUSIC,
        MANAGEMENT,
        MISCELLANEOUS,
        UTILITY
    }
}
