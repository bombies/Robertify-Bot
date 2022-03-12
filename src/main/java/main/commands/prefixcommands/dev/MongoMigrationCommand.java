//package main.commands.commands.dev;
//
//import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
//import lombok.Getter;
//import main.commands.commands.CommandContext;
//import main.commands.commands.CommandManager;
//import main.commands.commands.IDevCommand;
//import main.constants.Permission;
//import main.constants.ENV;
//import main.main.Config;
//import main.main.Robertify;
//import main.utils.database.mongodb.cache.BotInfoCache;
//import main.utils.database.sqlite3.BanDB;
//import main.utils.database.sqlite3.BotDB;
//import main.utils.database.sqlite3.ServerDB;
//import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
//import main.utils.json.eightball.EightBallConfig;
//import main.utils.json.guildconfig.GuildConfig;
//import main.utils.json.legacy.LegacyEightBallConfig;
//import main.utils.json.legacy.dedicatedchannel.LegacyDedicatedChannelConfig;
//import main.utils.json.legacy.permissions.LegacyPermissionsConfig;
//import main.utils.json.legacy.reports.LegacyReportsConfig;
//import main.utils.json.legacy.reports.ReportsConfigField;
//import main.utils.json.legacy.restrictedchannels.LegacyRestrictedChannelsConfig;
//import main.utils.json.legacy.suggestions.LegacySuggestionsConfig;
//import main.utils.json.legacy.togglesconfig.LegacyTogglesConfig;
//import main.utils.json.permissions.PermissionsConfig;
//import main.utils.json.restrictedchannels.RestrictedChannelsConfig;
//import main.constants.Toggles;
//import main.utils.json.toggles.TogglesConfig;
//import net.dv8tion.jda.api.entities.Guild;
//import net.dv8tion.jda.api.entities.Message;
//import org.json.JSONException;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.script.ScriptException;
//import java.util.List;
//
//public class MongoMigrationCommand implements IDevCommand {
//    private final Logger logger = LoggerFactory.getLogger(MongoMigrationCommand.class);
//    private final String migrationPrefix = "[Mongo Migration]";
//    @Getter
//    private static boolean isMigrating = false;
//
//    @Override
//    public void handle(CommandContext ctx) throws ScriptException {
//        if (!permissionCheck(ctx)) return;
//
//        final List<String> args = ctx.getArgs();
//        final Message msg = ctx.getMessage();
//
//        if (args.isEmpty()) {
//            msg.reply("Please provide args").queue();
//        } else {
//            try {
//                switch (args.get(0).toLowerCase()) {
//                    case "all" -> migrateAll();
//                    case "developers" -> migrateDevelopers();
//                    case "reports" -> migrateReports();
//                    case "suggestions" -> migrateSuggestions();
//                    case "dedicatedchannels" -> migrateDedicatedChannels();
//                    case "restrictedchannels" -> migrateRestrictedChannels();
//                    case "prefixes" -> migratePrefixes();
//                    case "permissions" -> migratePermissions();
//                    case "8ball" -> migrateEightBall();
//                    case "announcementschannel" -> migrateAnnouncementChannel();
//                    case "bannedusers" -> migrateBannedUsers();
//                }
//                msg.addReaction("✅").queue();
//            } catch (Exception e) {
//                logger.error("{} An unexpected error occurred.", migrationPrefix);
//                msg.addReaction("❌").queue();
//            }
//        }
//    }
//
//    private void migrateAll() {
//        isMigrating = true;
//        logger.info("""
//
//                ------------------------------------------------------
//
//                {} Starting data migration for all guilds
//
//                -----------------------------------------------------
//                """, migrationPrefix);
//
//        migrateDevelopers();
//        migrateReports();
//        migrateSuggestions();
//
//        for (Guild g : Robertify.api.getGuilds()) {
//            logger.info("{} Starting data migration for {}({})", migrationPrefix, g.getName(), g.getId());
//
//            // Dedicated Channels
//            try {
//                final var legacyDedicatedChannelConfig = new LegacyDedicatedChannelConfig();
//                if (legacyDedicatedChannelConfig.isChannelSet(g.getId())) {
//                    final var dedicatedChannelConfig = new DedicatedChannelConfig();
//                    if (!dedicatedChannelConfig.isChannelSet(g.getIdLong())) {
//
//                        String channelID = legacyDedicatedChannelConfig.getChannelID(g.getId());
//                        String messageID = legacyDedicatedChannelConfig.getMessageID(g.getId());
//                        dedicatedChannelConfig.setChannelAndMessage(g.getIdLong(), Long.parseLong(channelID), Long.parseLong(messageID));
//
//                        logger.info("{} Successfully migrated the dedicated channel", migrationPrefix);
//                    } else
//                        logger.warn("{} There is a new dedicated channel set up. Skipping migration", migrationPrefix);
//                } else
//                    logger.warn("{} There were no dedicated channels set up. ", migrationPrefix);
//            } catch (Exception e){
//                logger.error("{} An unexpected error occurred when migrating the dedicated channel.", migrationPrefix);
//            }
//
//            // Restricted Channels
//            try {
//                final var legacyConfig = new LegacyRestrictedChannelsConfig();
//                final var restrictedVoiceChannels = legacyConfig.getRestrictedChannels(g.getId(), LegacyRestrictedChannelsConfig.ChannelType.VOICE_CHANNEL);
//                final var restrictedTextChannels = legacyConfig.getRestrictedChannels(g.getId(), LegacyRestrictedChannelsConfig.ChannelType.TEXT_CHANNEL);
//                final var config = new RestrictedChannelsConfig();
//
//                if (!restrictedVoiceChannels.isEmpty()) {
//                    for (long vc : restrictedVoiceChannels) {
//                        try {
//                            config.addChannel(g.getIdLong(), vc, RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL);
//                        } catch (IllegalStateException e) {
//                            logger.warn("{} Channel with ID {} has already been migrated as a restricted voice channel", migrationPrefix, vc);
//                        }
//                    }
//                    logger.info("{} Successfully migrated all restricted voice channels", migrationPrefix);
//                } else logger.warn("{} There were no restricted voice channels to be migrated", migrationPrefix);
//
//                if (!restrictedTextChannels.isEmpty()) {
//                    for (long tc : restrictedTextChannels) {
//                        try {
//                            config.addChannel(g.getIdLong(), tc, RestrictedChannelsConfig.ChannelType.TEXT_CHANNEL);
//                        } catch (IllegalStateException e) {
//                            logger.warn("{} Channel with ID {} has already been migrated as a restricted text channel", migrationPrefix, tc);
//                        }
//                    }
//                    logger.info("{} Successfully migrated all restricted text channels", migrationPrefix);
//                } else logger.warn("{} There were no restricted text channels to be migrated", migrationPrefix);
//            } catch (JSONException e) {
//              logger.info("{} This guild wasn't found with any information for restricted channels", migrationPrefix);
//            } catch (Exception e) {
//                logger.error("{} An unexpected error occurred when migrating restricted channels.", migrationPrefix, e);
//            }
//
//            // Prefixes
//            try {
//                final var legacyDB = new ServerDB();
//                final var config = new GuildConfig();
//                var prefix = legacyDB.getServerPrefix(g.getIdLong());
//                prefix = prefix == null ? Config.get(ENV.PREFIX) : prefix;
//
//                config.setPrefix(g.getIdLong(), prefix);
//                logger.info("{} Successfully migrated the prefix", migrationPrefix);
//            } catch (Exception e) {
//                logger.error("{} An unexpected error occurred when migrating the prefix", migrationPrefix, e);
//            }
//
//            // Permissions
//            try {
//                final var legacyConfig = new LegacyPermissionsConfig();
//                final var config = new PermissionsConfig();
//
//                for (var permission : Permission.values()) {
//                    final var usersForPermission = legacyConfig.getUsersForPermission(g.getId(), permission.name());
//                    final var rolesForPermission = legacyConfig.getRolesForPermission(g.getId(), permission);
//
//                    if (!usersForPermission.isEmpty()) {
//                        for (var user : usersForPermission)
//                            try {
//                                config.addPermissionToUser(g.getIdLong(), Long.parseLong(user), permission);
//                            } catch (IllegalArgumentException ignored) {
//                            }
//                    } else logger.info("{} There were no users to add for the {} permission", migrationPrefix, permission.name());
//
//                    if (!rolesForPermission.isEmpty()) {
//                        for (var role : rolesForPermission)
//                            try {
//                                config.addRoleToPermission(g.getIdLong(), Long.parseLong(role), permission);
//                            } catch (IllegalAccessException ignored) {
//                            }
//                    } else logger.info("{} There were no roles to add for the {} permission", migrationPrefix, permission.name());
//                }
//                logger.info("{} Successfully migrated all permissions", migrationPrefix);
//            } catch (Exception e) {
//                logger.error("{} An unexpected error occurred when migrating permissions.", migrationPrefix, e);
//            }
//
//            // Eight Ball
//            try {
//                final var legacyConfig = new LegacyEightBallConfig();
//                final var config = new EightBallConfig();
//                final var oldResponses = legacyConfig.getResponses(g.getId());
//                final var newResponses = config.getResponses(g.getIdLong());
//
//                for (var response : oldResponses)
//                    if (!newResponses.contains(response))
//                        config.addResponse(g.getIdLong(), response);
//
//                    logger.info("{} Successfully migrated 8ball custom responses", migrationPrefix);
//            } catch (Exception e) {
//                logger.error("{} An unexpected error occurred when migrating custom 8ball responses.", migrationPrefix, e);
//            }
//
//            // Announcement Channel
//            try {
//                final var oldDB = new BotDB();
//                final var config = new GuildConfig();
//
//                if (oldDB.isAnnouncementChannelSet(g.getIdLong())) {
//                    final var oldChannel = oldDB.getAnnouncementChannel(g.getIdLong());
//                    config.setAnnouncementChannelID(g.getIdLong(), oldChannel);
//                    logger.info("{} Migrated the announcement channel", migrationPrefix);
//                } else logger.warn("{} There was no announcement channel set. Skipping migration", migrationPrefix);
//            } catch (Exception e) {
//                logger.error("{} An unexpected error occurred when migrating the announcement channel.", migrationPrefix, e);
//            }
//
//            // Banned Users
//            try {
//                final var oldDB = new BanDB();
//                final var config = new GuildConfig();
//                final var oldBannedUsers = oldDB.getAllBannedUsers(g.getIdLong());
//
//                if (!oldBannedUsers.isEmpty()) {
//                    for (var bannedUser : oldBannedUsers.keySet()) {
//                        if (!config.isBannedUser(g.getIdLong(), bannedUser)) {
//
//                            config.banUser(
//                                    g.getIdLong(),
//                                    bannedUser,
//                                    oldDB.getWhoBanned(g.getIdLong(), bannedUser),
//                                    oldDB.getTimeBanned(g.getIdLong(), bannedUser),
//                                    oldDB.getUnbanTime(g.getIdLong(), bannedUser)
//                            );
//                        }
//                    }
//
//                    logger.info("{} Successfully migrated banned users", migrationPrefix);
//                } else logger.warn("{} There were no banned users. Skipping migration", migrationPrefix);
//            } catch (Exception e) {
//                logger.error("{} An unexpected error occurred when migrating banned users.", migrationPrefix, e);
//            }
//
//            // Toggles
//            try {
//                final var oldConfig = new LegacyTogglesConfig();
//                final var config = new TogglesConfig();
//
//                for (var toggle : Toggles.values())
//                    if (config.getToggle(g, toggle) != oldConfig.getToggle(g, toggle))
//                        config.setToggle(g, toggle, oldConfig.getToggle(g, toggle));
//                logger.info("{} Migrated all normal toggles", migrationPrefix);
//
//                for (var cmd : new CommandManager(new EventWaiter()).getMusicCommands())
//                    if (config.getDJToggle(g, cmd) != oldConfig.getDJToggle(g, cmd))
//                        config.setDJToggle(g, cmd, oldConfig.getDJToggle(g, cmd));
//                logger.info("{} Migrated all DJ toggles", migrationPrefix);
//
//            } catch (Exception e) {
//                logger.error("{} An unexpected error occurred when migrating restricted channels for guild with ID: {}.", migrationPrefix, g.getId());
//            }
//
//            logger.info("{} Finished data migration for {}\n", migrationPrefix, g.getName());
//        }
//
//        logger.info("""
//
//                ------------------------------------------------------
//
//                {} Data migration completed!
//
//                ------------------------------------------------------""", migrationPrefix);
//        isMigrating = false;
//    }
//
//    private void migrateDevelopers() {
//        logger.info("{} Starting developer data migration", migrationPrefix);
//
//        final var cache = BotInfoCache.getInstance();
//        final var oldDevList = new BotDB().getDevelopers();
//
//        for (var id : oldDevList) {
//            try {
//                cache.addDeveloper(id);
//                logger.info("{} Successfully migrated developer with ID {}", migrationPrefix, id);
//            } catch (NullPointerException e) {
//                logger.warn("{} Developer with ID {} has already been migrated", migrationPrefix, id);
//            } catch (Exception e) {
//                logger.error("{} Could not migrate developer with ID {}", migrationPrefix, id, e);
//            }
//        }
//
//        logger.info("{} Finished developer data migration\n", migrationPrefix);
//    }
//
//    private void migrateReports() {
//        logger.info("{} Starting reports data migration", migrationPrefix);
//
//        final var cache = BotInfoCache.getInstance();
//        final var config = new LegacyReportsConfig();
//
//        if (config.isSetup()) {
//            long channelID = config.getID(ReportsConfigField.CHANNEL);
//            long categoryID = config.getID(ReportsConfigField.CATEGORY);
//            List<Long> bannedUsers = config.getBannedUsers();
//
//            try {
//                cache.initReportChannels(channelID, categoryID);
//                logger.info("{} Successfully migrated report channels. Channel ID {} | Category ID {}", migrationPrefix, channelID, categoryID);
//            } catch (IllegalStateException e) {
//                logger.warn("{} Could not migrate report channels. They have already been setup", migrationPrefix);
//            } catch (Exception e) {
//                logger.info("{} Could not migrate reports", migrationPrefix, e);
//            }
//
//            for (var user : bannedUsers) {
//                try {
//                    config.banUser(user);
//                    logger.info("{} Successfully migrated {} as a banned report user", migrationPrefix, user);
//                } catch (IllegalStateException e) {
//                    logger.warn("{} Could not migrate {} as a banned user. They have already been migrated.", migrationPrefix, user);
//                }
//            }
//        } else logger.info("{} Reports were not setup. There is nothing to migrate.", migrationPrefix);
//
//        logger.info("{} Finished reports data migration\n", migrationPrefix);
//    }
//
//    private void migrateSuggestions() {
//        logger.info("{} Starting suggestions data migration", migrationPrefix);
//
//        final var cache = BotInfoCache.getInstance();
//        final var config = new LegacySuggestionsConfig();
//
//        if (config.isSetup()) {
//            long acceptedChannelID = config.getAcceptedChannelID();
//            long pendingChannelID = config.getPendingChannelID();
//            long deniedChannelID = config.getDeniedChannelID();
//            long categoryID = config.getCategoryID();
//            List<Long> bannedUsers = config.getBannedUsers();
//
//            try {
//                cache.initSuggestionChannels(categoryID, pendingChannelID, acceptedChannelID, deniedChannelID);
//                logger.info("{} Successfully migrated suggestion channels", migrationPrefix);
//            } catch (IllegalStateException e) {
//                logger.warn("{} Could not migrate suggestion channels. They have already been setup", migrationPrefix);
//            } catch (Exception e) {
//                logger.info("{} Could not migrate suggestions", migrationPrefix, e);
//            }
//
//            for (var user : bannedUsers) {
//                try {
//                    config.banUser(user);
//                    logger.info("{} Successfully migrated {} as a banned suggestion user", migrationPrefix, user);
//                } catch (IllegalStateException e) {
//                    logger.warn("{} Could not migrate {} as a banned user. They have already been migrated.", migrationPrefix, user);
//                }
//            }
//        } else logger.info("{} Suggestions were not setup. There is nothing to migrate.", migrationPrefix);
//
//        logger.info("{} Finished suggestions data migration\n", migrationPrefix);
//    }
//
//    private void migrateDedicatedChannels() {
//        logger.info("{} Starting dedicated channels data migration", migrationPrefix);
//
//        logger.info("{} Finished dedicated channels data migration", migrationPrefix);
//    }
//
//    private void migrateRestrictedChannels() {
//        logger.info("{} Starting restricted channels data migration", migrationPrefix);
//
//        logger.info("{} Finished restricted channels data migration", migrationPrefix);
//    }
//
//    private void migratePrefixes() {
//        logger.info("{} Starting prefix data migration", migrationPrefix);
//
//        logger.info("{} Finished prefix data migration", migrationPrefix);
//    }
//
//    private void migratePermissions() {
//        logger.info("{} Starting permissions data migration", migrationPrefix);
//
//        logger.info("{} Finished permissions data migration", migrationPrefix);
//    }
//
//    private void migrateEightBall() {
//        logger.info("{} Starting 8ball data migration", migrationPrefix);
//
//        logger.info("{} Finished 8ball data migration", migrationPrefix);
//    }
//
//    private void migrateAnnouncementChannel() {
//        logger.info("{} Starting announcement channel data migration", migrationPrefix);
//
//        logger.info("{} Finished announcement channel data migration", migrationPrefix);
//    }
//
//    private void migrateBannedUsers() {
//        logger.info("{} Starting banned users data migration", migrationPrefix);
//
//        logger.info("{} Finished banned users data migration", migrationPrefix);
//    }
//
//    @Override
//    public String getName() {
//        return "migrate";
//    }
//
//    @Override
//    public String getHelp(String prefix) {
//        return null;
//    }
//}
