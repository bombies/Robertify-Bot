package main.main;

import lombok.SneakyThrows;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandManager;
import main.commands.slashcommands.commands.audio.SeekSlashCommand;
import main.commands.slashcommands.commands.dev.SendAlertCommand;
import main.commands.slashcommands.commands.misc.reminders.ReminderScheduler;
import main.commands.slashcommands.commands.util.AlertCommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.database.mongodb.cache.BotBDCache;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.resume.ResumeUtils;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
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

    public Listener() {
        manager = new CommandManager();
    }

    @SneakyThrows
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        final var jda = event.getJDA();

        for (Guild g : jda.getGuildCache()) {
            logger.debug("[Shard #{}] Loading {}...", jda.getShardInfo().getShardId(), g.getName());
            loadNeededSlashCommands(g);
            unloadCommands(g);
            rescheduleUnbans(g);
            ReminderScheduler.getInstance().scheduleGuildReminders(g);

            try {
                ResumeUtils.getInstance().loadInfo(g);
            } catch (Exception e) {
                logger.error("There was an error resuming tracks in {}", g.getName(), e);
            }
        }

//        StatisticsDB.startDailyUpdateCheck();

        updateServerCount();
        logger.info("Watching {} guilds on shard #{}", jda.getGuildCache().size(), jda.getShardInfo().getShardId());

        BotBDCache.getInstance().setLastStartup(System.currentTimeMillis());
        Robertify.shardManager.setPresence(OnlineStatus.ONLINE, Activity.listening("/help"));
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

        Message message = event.getMessage();
        final String raw = message.getContentRaw();

        if (user.isBot() || event.isWebhookMessage()) return;

        if (raw.startsWith(prefix) && raw.length() > prefix.length()) {
            if (new GuildConfig().isBannedUser(event.getGuild().getIdLong(), user.getIdLong())) {
                message.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You are banned from using commands in this server!").build())
                        .queue();
            } else {
                try {
                    manager.handle(event);
                } catch (InsufficientPermissionException e) {
                    try {
                        if (e.getMessage().contains("Permission.MESSAGE_EMBED_LINKS")) {
                            event.getChannel().sendMessage("""
                                            ⚠️ I don't have permission to send embeds!

                                            Please tell an admin to enable the `Embed Links` permission for my role in this channel in order for my commands to work!"""
                                    )
                                    .queue();
                        } else {
                            logger.error("Insufficient permissions", e);
                        }
                    } catch (InsufficientPermissionException ignored) {}
                }
            }
        } else if (!message.getMentionedMembers().isEmpty()) {
            if (!message.getContentRaw().startsWith("<@!"+guild.getSelfMember().getId()+">"))
                return;

            try {
                message.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Hey " + event.getAuthor().getAsMention() + "! Thank you for using Robertify. :)\n" +
                                        "My prefix in this server is: `" + prefix + "`\n\n" +
                                        "Type `" + prefix + "help` to see all the commands I offer!\n" +
                                        "[Invite](https://robertify.me/invite) | [Commands](https://robertify.me/commands) | [Support](https://robertify.me/support)")
                                .setFooter("Developed by bombies#4445")
                                .build())
                        .queue();
            } catch (InsufficientPermissionException e) {
                if (!e.getMessage().contains("EMBED")) return;

                message.reply("Hey " + event.getAuthor().getAsMention() + "! Thank you for using Robertify. :)\n" +
                        "My prefix in this server is: `" + prefix + "`\n\n" +
                        "Type `" + prefix + "help` to see all the commands I offer!\n").queue();
            }
        }
    }

    private void removeAllSlashCommands(Guild g) {
        g.retrieveCommands().queue(commands -> commands.forEach(command -> g.deleteCommandById(command.getIdLong()).queue()));
    }

    @SneakyThrows
    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        Guild guild = event.getGuild();
        new GuildConfig().addGuild(guild.getIdLong());
        loadSlashCommands(guild);
        GeneralUtils.setDefaultEmbed(guild);
        logger.info("Joined {}", guild.getName());

        updateServerCount();
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        Guild guild = event.getGuild();
        new GuildConfig().removeGuild(guild.getIdLong());
        RobertifyAudioManager.getInstance().getMusicManager(guild)
                        .destroy();
        RobertifyAudioManager.getInstance().removeMusicManager(event.getGuild());
        logger.info("Left {}", guild.getName());

        updateServerCount();
    }

    private void updateServerCount() {
        final int serverCount = Robertify.shardManager.getGuilds().size();

        if (Robertify.getTopGGAPI() != null)
            Robertify.getTopGGAPI().setStats(serverCount);

        if (Robertify.getDiscordBotListAPI() != null)
            Robertify.getDiscordBotListAPI().setStats(serverCount);
    }

    public void loadSlashCommands(Guild g) {
        AbstractSlashCommand.loadAllCommands(g);
    }

    public void loadNeededSlashCommands(Guild g) {
        // Only slash commands that NEED to be updated in each guild.
        new SeekSlashCommand().loadCommand(g);
    }

    public void unloadCommands(Guild g) {

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
        Robertify.shardManager.retrieveUserById(user).queue(user1 -> user1.openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(
                RobertifyEmbedUtils.embedMessage(g, "You have been unbanned from Robertify in **"+g.getName()+"**")
                        .build()
        ).queue(success -> {}, new ErrorHandler()
                .handle(ErrorResponse.CANNOT_SEND_TO_USER, (e) -> logger.warn("Was not able to send an unban message to " + user1.getAsTag() + "("+ user1.getIdLong()+")")))));
    }
}
