package main.commands.slashcommands;

import lombok.Getter;
import main.commands.ICommand;
import main.commands.commands.audio.*;
import main.commands.commands.audio.autoplay.AutoPlayCommand;
import main.commands.commands.dev.*;
import main.commands.commands.management.*;
import main.commands.commands.management.dedicatedchannel.DedicatedChannelCommand;
import main.commands.commands.management.permissions.ListDJCommand;
import main.commands.commands.management.permissions.PermissionsCommand;
import main.commands.commands.management.permissions.RemoveDJCommand;
import main.commands.commands.management.permissions.SetDJCommand;
import main.commands.commands.misc.EightBallCommand;
import main.commands.commands.misc.PingCommand;
import main.commands.commands.misc.PlaytimeCommand;
import main.commands.commands.misc.poll.PollCommand;
import main.commands.commands.misc.reminders.RemindersCommand;
import main.commands.commands.util.*;
import main.commands.commands.util.reports.ReportsCommand;
import main.commands.slashcommands.commands.*;
import main.commands.slashcommands.commands.filters.*;
import main.utils.component.interactions.AbstractSlashCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SlashCommandManager {

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

    public SlashCommandManager() {
        addMusicCommands(
                new PlaySlashCommand(),
                new DisconnectSlashCommand(),
                new ClearQueueSlashCommand(),
                new JoinSlashCommand(),
                new JumpSlashCommand(),
                new LofiSlashCommand(),
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
                new VibratoFilter()
        );

        addManagementCommands(
                new SetDJCommand(),
                new RemoveDJCommand(),
                new ListDJCommand(),
                new BanCommand(),
                new UnbanCommand(),
                new PermissionsCommand(),
                new TogglesCommand(),
                new DedicatedChannelCommand(),
                new ThemeCommand(),
                new RestrictedChannelsCommand(),
                new LogCommand(),
                new SetLogChannelCommand()
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
                new PlaytimeCommand()
        );

        addDevCommands(
                new GuildCommand(),
                new VoiceChannelCountCommand(),
                new UpdateCommand(),
                new EvalCommand(),
                new RandomMessageCommand(),
                new ReloadConfigCommand(),
                new ChangeLogCommand(),
                new AnnouncementCommand()
        );
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


    public List<AbstractSlashCommand> getCommands() {
        final List<AbstractSlashCommand> abstractSlashCommands = new ArrayList<>();
        abstractSlashCommands.addAll(musicCommands);
        abstractSlashCommands.addAll(managementCommands);
        abstractSlashCommands.addAll(miscCommands);
        abstractSlashCommands.addAll(utilityCommands);
        return abstractSlashCommands;
    }

    public AbstractSlashCommand getCommand(String name) {
        return getCommands().stream()
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
}
