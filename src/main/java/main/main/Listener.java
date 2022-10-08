package main.main;

import lombok.SneakyThrows;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandManager;
import main.commands.slashcommands.commands.audio.HistoryCommand;
import main.commands.slashcommands.commands.dev.PostCommandInfoCommand;
import main.commands.slashcommands.commands.dev.RefreshSpotifyTokenCommand;
import main.commands.slashcommands.commands.management.LanguageCommand;
import main.commands.slashcommands.commands.management.TogglesCommand;
import main.commands.slashcommands.commands.management.dedicatedchannel.DedicatedChannelEditCommand;
import main.commands.slashcommands.commands.misc.reminders.ReminderScheduler;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.apis.robertify.models.RobertifyPremium;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.database.mongodb.cache.BotBDCache;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.locale.LocaleConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocale;
import main.utils.locale.RobertifyLocaleMessage;
import main.utils.resume.ResumeUtils;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Listener extends ListenerAdapter {

    public static final Logger logger = LoggerFactory.getLogger(Listener.class);

    @SneakyThrows
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        final var jda = event.getJDA();

        for (Guild g : jda.getGuildCache()) {
            final var dedicatedChannelConfig = new DedicatedChannelConfig(g);
            logger.debug("[Shard #{}] Loading {}...", jda.getShardInfo().getShardId(), g.getName());
            final var locale = new LocaleConfig(g).getLocale();
            if (locale != null)
                LocaleManager.getLocaleManager(g).setLocale(locale);

            loadNeededSlashCommands(g);
            unloadCommands(g, "lofi");
//            unloadDevCommands(g, "");
            rescheduleUnbans(g);
            ReminderScheduler.getInstance().scheduleGuildReminders(g);

            if (dedicatedChannelConfig.isChannelSet())
                dedicatedChannelConfig.updateAll();
            try {
                ResumeUtils.getInstance().loadInfo(g);
            } catch (Exception e) {
                logger.error("There was an error resuming tracks in {}", g.getName(), e);
            }
        }

        logger.info("Watching {} guilds on shard #{}", jda.getGuildCache().size(), jda.getShardInfo().getShardId());

        BotBDCache.getInstance().setLastStartup(System.currentTimeMillis());
        Robertify.shardManager.setPresence(OnlineStatus.ONLINE, Activity.listening("/help"));
    }

//    @SneakyThrows
//    @Override
//    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
//        if (!event.isFromGuild()) return;
//
//        final User user = event.getAuthor();
//        final var guild = event.getGuild();
//        final var guildConfig = new GuildConfig(guild);
//        final String prefix;
//
//        try {
//            prefix = guildConfig.getPrefix();
//        } catch (NullPointerException ignored) {
//            return;
//        }
//
//        Message message = event.getMessage();
//        final String raw = message.getContentRaw();
//
//        if (user.isBot() || event.isWebhookMessage()) return;
//
//        if (raw.startsWith(prefix) && raw.length() > prefix.length()) {
//            final var localeManager = LocaleManager.getLocaleManager(guild);
//            if (Config.isPremiumBot() && !guildConfig.isPremium()) {
//                try {
//                    event.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(guild, RobertifyLocaleMessage.GeneralMessages.PREMIUM_EMBED_TITLE, RobertifyLocaleMessage.GeneralMessages.PREMIUM_INSTANCE_NEEDED)
//                                    .build())
//                            .setActionRow(Button.link("https://robertify.me/premium", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.PREMIUM_UPGRADE_BUTTON)))
//                            .queue();
//                } catch (InsufficientPermissionException e) {
//                    if (e.getMessage().contains("Permission.MESSAGE_EMBED_LINKS")) {
//                        event.getChannel().sendMessage(localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.PREMIUM_INSTANCE_NEEDED))
//                                .setActionRow(Button.link("https://robertify.me/premium", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.PREMIUM_UPGRADE_BUTTON)))
//                                .queue();
//                    } else {
//                        logger.error("Insufficient permissions", e);
//                    }
//                }
//                return;
//            }
//
//            if (guildConfig.isBannedUser(user.getIdLong())) {
//                try {
//                    message.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.BANNED_FROM_COMMANDS).build())
//                            .queue();
//                } catch (InsufficientPermissionException e) {
//                    if (e.getMessage().contains("Permission.MESSAGE_EMBED_LINKS")) {
//                        event.getChannel().sendMessage(localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.BANNED_FROM_COMMANDS))
//                                .queue();
//                    } else {
//                        logger.error("Insufficient permissions", e);
//                    }
//                }
//            } else {
//                try {
//                    manager.handle(event);
//                } catch (InsufficientPermissionException e) {
//                    try {
//                        if (e.getMessage().contains("Permission.MESSAGE_EMBED_LINKS")) {
//                            event.getChannel().sendMessage(localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.NO_EMBED_PERMS))
//                                    .queue();
//                        } else {
//                            logger.error("Insufficient permissions", e);
//                        }
//                    } catch (InsufficientPermissionException ignored) {}
//                }
//            }
//        }
//    }

    private void removeAllSlashCommands(Guild g) {
        g.retrieveCommands().queue(commands -> commands.forEach(command -> g.deleteCommandById(command.getIdLong()).queue()));
    }

    @SneakyThrows
    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        Guild guild = event.getGuild();
        new GuildConfig(guild).addGuild();
        loadSlashCommands(guild);
        GeneralUtils.setDefaultEmbed(guild);
        logger.info("Joined {}", guild.getName());

        updateServerCount();
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        Guild guild = event.getGuild();
        new GuildConfig(guild).removeGuild();
        RobertifyAudioManager.getInstance().getMusicManager(guild)
                        .destroy();
        RobertifyAudioManager.getInstance().removeMusicManager(event.getGuild());
        logger.info("Left {}", guild.getName());

        updateServerCount();
    }

    /**
     * Update the server count displayed on the vote websites
     */
    private void updateServerCount() {
        final int serverCount = Robertify.shardManager.getGuilds().size();

        BotBDCache.getInstance().setGuildCount(serverCount);

        if (Robertify.getTopGGAPI() != null)
            Robertify.getTopGGAPI().setStats(serverCount);

        if (Robertify.getDiscordBotListAPI() != null)
            Robertify.getDiscordBotListAPI().setStats(serverCount);
    }

    public void loadSlashCommands(Guild g) {
        AbstractSlashCommand.loadAllCommands(g);
    }

    /**
     * Load slash commands that NEED to be updated in a guild
     * @param g The guild to load the commands in
     */
    public void loadNeededSlashCommands(Guild g) {
        AbstractSlashCommand.loadAllCommands(g);
    }

    /**
     * Unload specific slash commands from a guild
     * @param g The guild to unload the commands in
     */
    public void unloadCommands(Guild g, String... commandNames) {
        if (commandNames.length == 0)
            return;

        g.retrieveCommands().queue(commands -> commands.stream()
                .filter(command -> GeneralUtils.equalsAny(command.getName(), commandNames))
                .toList()
                .forEach(command -> g.deleteCommandById(command.getIdLong()).queue()),
                new ErrorHandler().handle(ErrorResponse.UNKNOWN_COMMAND, e -> logger.warn("Could not remove some commands from {}", g.getName()))
        );
    }

    public void unloadDevCommands(Guild g, String... commandNames) {
        if (g.getOwnerIdLong() == Config.getOwnerID())
            unloadCommands(g, commandNames);
    }

    /**
     * Load bans for a specific guilds and reschedule the executors for the unbans
     * of specific users
     * @param g The guild to reschedule unbans for
     */
    private static void rescheduleUnbans(Guild g) {
        final var banUtils = new GuildConfig(g);
        final var map = banUtils.getBannedUsersWithUnbanTimes();

        for (long user : map.keySet()) {
            if (map.get(user) == null) continue;
            if (map.get(user) == -1) continue;
            if (map.get(user) - System.currentTimeMillis() <= 0) {
                try {
                    banUtils.unbanUser(user);
                    sendUnbanMessage(user, g);
                } catch (IllegalArgumentException e) {
                    map.remove(user);
                }
                continue;
            }

            final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            final Runnable task = () -> {
                banUtils.unbanUser(user);

                sendUnbanMessage(user, g);
            };
            scheduler.schedule(task, banUtils.getTimeUntilUnban(user), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * The initial scheduling of an un-ban for a banned user
     * @param g The guild the banned user is in
     * @param u The banner user
     */
    public static void scheduleUnban(Guild g, User u) {
        final GuildConfig banUtils = new GuildConfig(g);
        final var scheduler = Executors.newScheduledThreadPool(1);

        final Runnable task = () -> {
            if (!banUtils.isBannedUser(u.getIdLong()))
                return;

            banUtils.unbanUser(u.getIdLong());

            sendUnbanMessage(u.getIdLong(), g);
        };
        scheduler.schedule(task, banUtils.getTimeUntilUnban(u.getIdLong()), TimeUnit.MILLISECONDS);
    }

    private static void sendUnbanMessage(long user, Guild g) {
        Robertify.shardManager.retrieveUserById(user).queue(user1 -> user1.openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(
                RobertifyEmbedUtils.embedMessage(g, RobertifyLocaleMessage.UnbanMessages.USER_UNBANNED, Pair.of("{server}", g.getName()))
                        .build()
        ).queue(success -> {}, new ErrorHandler()
                .handle(ErrorResponse.CANNOT_SEND_TO_USER, (e) -> logger.warn("Was not able to send an unban message to " + user1.getAsTag() + "("+ user1.getIdLong()+")")))));
    }
}
