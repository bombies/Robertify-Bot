package main.main;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import lombok.SneakyThrows;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandManager;
import main.commands.commands.audio.FavouriteTracksCommand;
import main.commands.commands.audio.slashcommands.*;
import main.commands.commands.dev.MongoMigrationCommand;
import main.commands.commands.management.BanCommand;
import main.commands.commands.management.SetChannelCommand;
import main.commands.commands.management.ThemeCommand;
import main.commands.commands.management.UnbanCommand;
import main.commands.commands.management.permissions.ListDJCommand;
import main.commands.commands.management.permissions.RemoveDJCommand;
import main.commands.commands.management.permissions.SetDJCommand;
import main.commands.commands.util.*;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.commands.commands.misc.EightBallCommand;
import main.constants.BotConstants;
import main.utils.database.mongodb.AbstractMongoDatabase;
import main.utils.database.mongodb.cache.BotInfoCache;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.changelog.ChangeLogConfig;
import main.utils.json.legacy.AbstractJSONFile;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Listener extends ListenerAdapter {
    private final CommandManager manager;
    public static final Logger logger = LoggerFactory.getLogger(Listener.class);

    public Listener(EventWaiter waiter) {
        manager = new CommandManager(waiter);
    }

    @SneakyThrows
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        // Initialize the JSON directory
        // This is a deprecated feature and is marked for removal
        // Until everything is fully removed, this method needs to be enabled
        // For a proper first-boot.
        AbstractJSONFile.initDirectory();

        AbstractMongoDatabase.initAllCaches();
        AbstractMongoDatabase.updateAllCaches();
        logger.info("Initialized and updated all caches");

        new ChangeLogConfig().initConfig();

        for (Guild g : Robertify.api.getGuilds()) {
            initNeededSlashCommands(g);
            rescheduleUnbans(g);
            GeneralUtils.setDefaultEmbed(g);

            DedicatedChannelConfig dedicatedChannelConfig = new DedicatedChannelConfig();
            try {
                if (dedicatedChannelConfig.isChannelSet(g.getIdLong())) {
                    dedicatedChannelConfig.updateMessage(g);
                    dedicatedChannelConfig.updateButtons(g);
                }
            } catch (NullPointerException e) {
                dedicatedChannelConfig.removeChannel(g.getIdLong());
            }
        }

        initSelectionMenus();
//        StatisticsDB.startDailyUpdateCheck();

        logger.info("Watching {} guilds", Robertify.api.getGuilds().size());
        BotInfoCache.getInstance().setLastStartup(System.currentTimeMillis());

        Robertify.api.getPresence().setPresence(Activity.listening("+help"), true);
    }

    @SneakyThrows
    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        final User user = event.getAuthor();
        final var guild = event.getGuild();
        final String prefix;

        try {
            prefix = new GuildConfig().getPrefix(event.getGuild().getIdLong());
        } catch (NullPointerException ignored) {
            return;
        }

        final String raw = event.getMessage().getContentRaw();

        if (user.isBot() || event.isWebhookMessage()) return;

        if (raw.startsWith(prefix) && raw.length() > prefix.length()) {
            if (new GuildConfig().isBannedUser(event.getGuild().getIdLong(), user.getIdLong())) {
                event.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You are banned from using commands in this server!").build())
                        .queue();
            } else {
                try {
                    if (MongoMigrationCommand.isMigrating()) {
                        event.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "I am migrating databases at the moment!" +
                                        " You are not allowed to use commands.")
                                .build()).queue();
                        return;
                    }

                    manager.handle(event);
                } catch (InsufficientPermissionException e) {
                    try {
                        if (e.getMessage().contains("Permission.MESSAGE_EMBED_LINKS")) {
                            event.getChannel().sendMessage("""
                                            ⚠️ I don't have permission to send embeds!

                                            Please tell an admin to enable the `Embed Links` permission for my role in this
                                             channel in order for my commands to work!""")
                                    .queue();
                        } else {
                            logger.error("Insufficient permissions", e);
                        }
                    } catch (InsufficientPermissionException ignored) {}
                }
            }
        }
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        final var guild = event.getGuild();

        if (MongoMigrationCommand.isMigrating()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "I am migrating databases at the moment!" +
                            " You are not allowed to use commands.")
                    .build()).queue();
            return;
        }

        if (new GuildConfig().isBannedUser(guild.getIdLong(), event.getUser().getIdLong()))
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, BotConstants.BANNED_MESSAGE.toString()).build())
                    .queue();

    }

    @SneakyThrows
    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        Guild guild = event.getGuild();
        new GuildConfig().addGuild(guild.getIdLong());
        initSlashCommands(guild);
        logger.info("Joined {}", guild.getName());
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        Guild guild = event.getGuild();
        new GuildConfig().removeGuild(guild.getIdLong());
        RobertifyAudioManager.getInstance().removeMusicManager(event.getGuild());
        logger.info("Left {}", guild.getName());
    }

    public static void checkIfAnnouncementChannelIsSet(Guild guild, TextChannel channel) {
        var guildConfig = new GuildConfig();
        if (!guildConfig.announcementChannelIsSet(guild.getIdLong())) {
            if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong())) {
                    if (channel.getIdLong() == new DedicatedChannelConfig().getChannelID(guild.getIdLong())) {
                        channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You cannot run this command in this channel " +
                                        "without first having an announcement channel set!").build())
                                .queue();
                        return;
                    }
            }

            guildConfig.setAnnouncementChannelID(guild.getIdLong(), channel.getIdLong());

            channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, """
                    There was no announcement channel set! Setting it to this channel.

                    _You can change the announcement channel by using the "setchannel" command._""").build()).queue();
        }
    }

    public void initSlashCommands() {
        new PlaySlashCommand().initCommand();
        new QueueSlashCommand().initCommand();
        new LeaveSlashCommand().initCommand();
        new ClearQueueSlashCommand().initCommand();
        new JumpSlashCommand().initCommand();
        new NowPlayingSlashCommand().initCommand();
        new PauseSlashCommand().initCommand();
        new HelpCommand().initCommand();
    }

    public void initSlashCommands(Guild g) {
        new PlaySlashCommand().initCommand(g);
        new QueueSlashCommand().initCommand(g);
        new LeaveSlashCommand().initCommand(g);
        new ClearQueueSlashCommand().initCommand(g);
        new JumpSlashCommand().initCommand(g);
        new NowPlayingSlashCommand().initCommand(g);
        new PauseSlashCommand().initCommand(g);
        new HelpCommand().initCommand(g);
        new SkipSlashCommand().initCommand(g);
        new RemoveSlashCommand().initCommand(g);
        new LoopSlashCommand().initCommand(g);
        new MoveSlashCommand().initCommand(g);
        new RewindSlashCommand().initCommand(g);
        new SetChannelCommand().initCommand(g);
        new VolumeSlashCommand().initCommand(g);
        new SetDJCommand().initCommand(g);
        new RemoveDJCommand().initCommand(g);
        new ListDJCommand().initCommand(g);
        new SeekSlashCommand().initCommand(g);
        new BanCommand().initCommand(g);
        new UnbanCommand().initCommand(g);
        new ShuffleSlashCommand().initCommand(g);
        new EightBallCommand().initCommand(g);
        new JoinSlashCommand().initCommand(g);
        new SuggestionCommand().initCommand(g);
        new VoteCommand().initCommand(g);
        new ShufflePlaySlashCommand().initCommand(g);
        new UptimeCommand().initCommand(g);
        new LofiSlashCommand().initCommand(g);
        new SupportServerCommand().initCommand(g);
        new DonateCommand().initCommand(g);
        new ThemeCommand().initCommand(g);
        new WebsiteCommand().initCommand(g);
    }

    public void initNeededSlashCommands(Guild g) {
        // Only slash commands that NEED to be updated in each guild.
        new WebsiteCommand().initCommand(g);
        new FavouriteTracksCommand().initCommand(g);
    }

    private static void rescheduleUnbans(Guild g) {
        final var banUtils = new GuildConfig();
        final var map = banUtils.getBannedUsersWithUnbanTimes(g.getIdLong());

        for (long user : map.keySet()) {
            if (map.get(user) == null) continue;
            if (map.get(user) == -1) continue;
            if (map.get(user) - System.currentTimeMillis() <= 0) {
                try {
                    banUtils.unbanUser(g.getIdLong(), user);
                    sendUnbanMessage(user, g);
                } catch (IllegalArgumentException e) {
                    map.remove(user);
                }
                continue;
            }

            final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            final Runnable task = () -> {
                banUtils.unbanUser(user, g.getIdLong());

                sendUnbanMessage(user, g);
            };
            scheduler.schedule(task, banUtils.getTimeUntilUnban(g.getIdLong(), user), TimeUnit.MILLISECONDS);

        }
    }

    public static void scheduleUnban(Guild g, User u) {
        final GuildConfig banUtils = new GuildConfig();
        final var scheduler = Executors.newScheduledThreadPool(1);

        final Runnable task = () -> {
            if (!new GuildConfig().isBannedUser(g.getIdLong(), u.getIdLong()))
                return;

            banUtils.unbanUser(g.getIdLong(), u.getIdLong());

            sendUnbanMessage(u.getIdLong(), g);
        };
        scheduler.schedule(task, new GuildConfig().getTimeUntilUnban(g.getIdLong(), u.getIdLong()), TimeUnit.MILLISECONDS);
    }

    private static void sendUnbanMessage(long user, Guild g) {
        Robertify.api.retrieveUserById(user).queue(user1 -> user1.openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(
                RobertifyEmbedUtils.embedMessage(g, "You have been unbanned from Robertify in **"+g.getName()+"**")
                        .build()
        ).queue(success -> {}, new ErrorHandler()
                .handle(ErrorResponse.CANNOT_SEND_TO_USER, (e) -> logger.warn("Was not able to send an unban message to " + user1.getAsTag() + "("+ user1.getIdLong()+")")))));
    }

    private static void initSelectionMenus() {
        new HelpCommand().initCommandWithoutUpsertion();
        new ThemeCommand().initCommandWithoutUpsertion();
    }

}
