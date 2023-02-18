package main.main;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.sentry.Sentry;
import lavalink.client.io.LavalinkLoadBalancer;
import lavalink.client.io.jda.JdaLavalink;
import lombok.Getter;
import lombok.SneakyThrows;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.contextcommands.ContextCommandManager;
import main.commands.prefixcommands.audio.SkipCommand;
import main.commands.prefixcommands.dev.test.MenuPaginationTestCommand;
import main.commands.prefixcommands.util.reports.ReportsEvents;
import main.commands.slashcommands.SlashCommandManager;
import main.commands.slashcommands.commands.management.dedicatedchannel.DedicatedChannelEvents;
import main.commands.slashcommands.commands.misc.poll.PollEvents;
import main.constants.ENV;
import main.events.LogChannelEvents;
import main.events.SuggestionCategoryDeletionEvents;
import main.events.VoiceChannelEvents;
import main.utils.EventWaiter;
import main.utils.apis.robertify.RobertifyAPI;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.database.mongodb.AbstractMongoDatabase;
import main.utils.database.mongodb.cache.redis.GuildRedisCache;
import main.utils.json.AbstractJSONFile;
import main.utils.pagination.PaginationEvents;
import main.utils.votes.api.discordbotlist.DBLApi;
import me.duncte123.botcommons.web.WebUtils;
import net.dv8tion.jda.api.GatewayEncoding;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.SessionControllerAdapter;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.discordbots.api.client.DiscordBotListAPI;
import org.jetbrains.annotations.NotNull;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

public class Robertify {

    private static final Logger logger = LoggerFactory.getLogger(Robertify.class);


    @Getter
    public static ShardManager shardManager;
    @Getter
    private static JdaLavalink lavalink;
    @Getter
    private static DiscordBotListAPI topGGAPI;
    @Getter
    private static DBLApi discordBotListAPI;
    @Getter
    private static RobertifyAPI robertifyAPI;
    @Getter
    private static final EventWaiter commandWaiter = new EventWaiter();
    @Getter
    private static Scheduler cronScheduler;

    static {
        try {
            Robertify.cronScheduler = new StdSchedulerFactory().getScheduler();
        } catch (SchedulerException e) {
            logger.error("I was unable to initialize the CRON scheduler!");
        }
    }

    public static void main(String[] args) {
        WebUtils.setUserAgent("Mozilla/Robertify / bombies#4445");

        try {
            lavalink = new JdaLavalink(
                    getIdFromToken(Config.getBotToken()),
                    Config.getShardCount(),
                    shardId -> Robertify.getShardManager().getShardById(shardId)
            );

            for (var node : Config.getLavaNodes())
                lavalink.addNode(node.getURI(), node.getPassword());

            lavalink.getLoadBalancer().addPenalty(LavalinkLoadBalancer.Penalties::getPlayerPenalty);
            lavalink.getLoadBalancer().addPenalty(LavalinkLoadBalancer.Penalties::getCpuPenalty);

            var thread = new ThreadFactoryBuilder().setNameFormat("RobertifyShutdownHook").build();
            Runtime.getRuntime().addShutdownHook(thread.newThread(() -> {
                logger.info("Destroying all players (If any left)");
                shardManager.getGuildCache().stream()
                        .filter(guild -> guild.getSelfMember().getVoiceState().inAudioChannel())
                        .forEach(guild -> {
                            GuildMusicManager musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
                            musicManager.getScheduler().disconnect(false);
                        });
                logger.info("Killing cron scheduler");
                try {
                    getCronScheduler().clear();
                } catch (SchedulerException e) {
                    logger.error("I couldn't clear scheduling data!");
                }
                shardManager.shutdown();
            }));

            DefaultShardManagerBuilder jdaBuilder = DefaultShardManagerBuilder.createDefault(
                            Config.getBotToken(),
                            GatewayIntent.GUILD_VOICE_STATES
                    )
                    .setShardsTotal(Config.getShardCount())
                    .setBulkDeleteSplittingEnabled(false)
                    .setChunkingFilter(ChunkingFilter.NONE)
                    .setMemberCachePolicy(MemberCachePolicy.VOICE)

                    // Event Listeners
                    .addEventListeners(
                            lavalink,
                            VoiceChannelEvents.waiter,
                            commandWaiter,
                            new Listener(),
                            new VoiceChannelEvents(),
                            new DedicatedChannelEvents(),
                            new PollEvents(),
                            new SuggestionCategoryDeletionEvents(),
                            new ReportsEvents(),
                            new LogChannelEvents(),
                            new SkipCommand()
                    )
                    .setVoiceDispatchInterceptor(lavalink.getVoiceInterceptor())

                    // Test Listeners
                    .addEventListeners(
                            new MenuPaginationTestCommand()
                    )

                    // Button Listeners
                    .addEventListeners(
                            new PaginationEvents()
                    )

                    .enableCache(
                            CacheFlag.VOICE_STATE
                    )
                    .disableCache(
                            CacheFlag.ACTIVITY,
                            CacheFlag.EMOJI,
                            CacheFlag.CLIENT_STATUS,
                            CacheFlag.ROLE_TAGS,
                            CacheFlag.ONLINE_STATUS,
                            CacheFlag.STICKER,
                            CacheFlag.SCHEDULED_EVENTS
                    )
                    .setSessionController(new SessionControllerAdapter() {
                        @NotNull
                        @Override
                        public String getGateway() {
                            return Config.hasGatewayUrl() ? Config.getGatewayUrl() : super.getGateway();
                        }

                        @Override
                        public void setGlobalRatelimit(long ratelimit) {
                            if (Config.hasGatewayUrl())
                                super.setGlobalRatelimit(0);
                            else super.setGlobalRatelimit(ratelimit);
                        }

                        @Override
                        public long getGlobalRatelimit() {
                            if (Config.hasGatewayUrl())
                                return 0L;
                            return super.getGlobalRatelimit();
                        }
                    })
                    .setActivity(Activity.listening("Starting up..."));

            final var disabledIntents = Lists.newArrayList(
                    GatewayIntent.DIRECT_MESSAGE_TYPING,
                    GatewayIntent.GUILD_BANS,
                    GatewayIntent.GUILD_INVITES,
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.GUILD_MESSAGE_TYPING,
                    GatewayIntent.GUILD_PRESENCES,
                    GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                    GatewayIntent.SCHEDULED_EVENTS
            );

            final var enabledIntents = Lists.newArrayList(GatewayIntent.GUILD_MESSAGES);

            if (Config.getBoolean(ENV.MESSAGE_CONTENT_INTENT_ENABLED))
                enabledIntents.add(GatewayIntent.MESSAGE_CONTENT);
            else
                disabledIntents.add(GatewayIntent.MESSAGE_CONTENT);

            jdaBuilder.enableIntents(enabledIntents);
            jdaBuilder.disableIntents(disabledIntents);

            // Register all slash commands
            SlashCommandManager slashCommandManager = new SlashCommandManager();
            for (var cmd : slashCommandManager.getGlobalCommands()) {
                jdaBuilder.addEventListeners(cmd);
                logger.debug("Registered the \"{}\" command.", cmd.getName());
            }
            for (var cmd : slashCommandManager.getGuildCommands()) {
                jdaBuilder.addEventListeners(cmd);
                logger.debug("Registered the \"{}\" command.", cmd.getName());
            }
            for (var cmd : slashCommandManager.getDevCommands()) {
                jdaBuilder.addEventListeners(cmd);
                logger.debug("Registered the \"{}\" command.", cmd.getName());
            }

            ContextCommandManager contextCommandManager = new ContextCommandManager();
            for (var cmd : contextCommandManager.getCommands()) {
                jdaBuilder.addEventListeners(cmd);
                logger.debug("Registered the \"{}\" context command.", cmd.getName());
            }

            // Initialize the JSON directory
            // This is a deprecated feature and is marked for removal
            // Until everything is fully removed, this method needs to be enabled
            // For a proper first-boot.
            AbstractJSONFile.initDirectory();

            AbstractMongoDatabase.initAllCaches();
            logger.info("Initialized all caches");

            GuildRedisCache.getInstance().loadAllGuilds();
            logger.info("All guilds have been loaded into cache");

            shardManager = jdaBuilder.build();

            if (Config.loadCommands())
                AbstractSlashCommand.loadAllCommands();

            Robertify.cronScheduler.start();

            initVoteSiteAPIs();

            if (Config.hasValue(ENV.ROBERTIFY_API_PASSWORD))
                    robertifyAPI = RobertifyAPI.ins;

            if (Config.hasValue(ENV.SENTRY_DSN))
                Sentry.init(options -> {
                    options.setDsn(Config.get(ENV.SENTRY_DSN));
                    options.setTracesSampleRate(1.0);
                });
        } catch (Exception e) {
            logger.error("[FATAL ERROR] An unexpected error occurred!", e);
        }
    }

    private static void loadNeededGlobalCommands() {

    }

    private void loadGlobalSlashCommands() {
        AbstractSlashCommand.loadAllCommands();
    }

    public static void initVoteSiteAPIs() {
        if (!Config.get(ENV.TOP_GG_TOKEN).isEmpty())
            topGGAPI = new DiscordBotListAPI.Builder()
                    .token(Config.get(ENV.TOP_GG_TOKEN))
                    .botId(getIdFromToken(Config.get(ENV.BOT_TOKEN)))
                    .build();

        if (!Config.get(ENV.DBL_TOKEN).isEmpty())
            discordBotListAPI = new DBLApi.Builder()
                    .setToken(Config.get(ENV.DBL_TOKEN))
                    .setBotID(getIdFromToken(Config.get(ENV.BOT_TOKEN)))
                    .build();
    }

    private static String getIdFromToken(String token) {
        return new String(
                Base64.getDecoder().decode(
                        token.split("\\.")[0]
                )
        );
    }
}


