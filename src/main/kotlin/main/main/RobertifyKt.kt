package main.main

import api.RobertifyKtorApi
import com.adamratzman.spotify.SpotifyAppApi
import com.adamratzman.spotify.spotifyAppApi
import com.google.common.util.concurrent.ThreadFactoryBuilder
import dev.minn.jda.ktx.events.CoroutineEventManager
import dev.minn.jda.ktx.jdabuilder.defaultShard
import kotlinx.coroutines.*
import lavalink.client.io.jda.JdaLavalink
import main.audiohandlers.RobertifyAudioManagerKt
import main.commands.slashcommands.SlashCommandManagerKt
import main.commands.slashcommands.SlashCommandManagerKt.registerCommands
import main.commands.slashcommands.misc.reminders.RemindersCommandKt
import main.constants.ENVKt
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.database.mongodb.AbstractMongoDatabaseKt
import main.utils.database.mongodb.cache.redis.GuildRedisCacheKt
import main.events.EventManager
import main.main.ListenerKt.Companion.loadNeededSlashCommands
import main.main.ListenerKt.Companion.rescheduleUnbans
import main.utils.api.robertify.RobertifyApi
import main.utils.database.mongodb.cache.BotDBCacheKt
import main.utils.json.locale.LocaleConfigKt
import main.utils.json.reminders.RemindersConfigKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.resume.GuildResumeManagerKt
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.events.session.ShutdownEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.discordbots.api.client.DiscordBotListAPI
import org.quartz.SchedulerException
import org.quartz.impl.StdSchedulerFactory
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.reader.ReaderException
import java.util.Base64
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.minutes

object RobertifyKt {
    private val logger = LoggerFactory.getLogger(RobertifyKt::class.java)
    val cronScheduler = StdSchedulerFactory.getDefaultScheduler()
    var topGGAPI: DiscordBotListAPI? = null
    lateinit var lavalink: JdaLavalink
        private set
    lateinit var shardManager: ShardManager
        private set
    lateinit var spotifyApi: SpotifyAppApi
        private set
    lateinit var externalApi: RobertifyApi
        private set

    private val pool = Executors.newScheduledThreadPool(ForkJoinPool.getCommonPoolParallelism().coerceAtLeast(2)) {
        thread(start = false, name = "Event-Worker-Thread", isDaemon = true, block = it::run)
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // Setup graceful shutdown hooks
        val mainShutdownHook = ThreadFactoryBuilder()
            .setNameFormat("RobertifyShutdownHook")
            .build()
        Runtime.getRuntime().addShutdownHook(mainShutdownHook.newThread {
            logger.info("Destroying all players (If any left)")
            runBlocking {
                RobertifyAudioManagerKt.musicManagers
                    .values
                    .forEach { musicManager ->
                        if (musicManager.player.playingTrack != null)
                            GuildResumeManagerKt(musicManager.guild).saveTracks()
                        musicManager.destroy()
                    }
            }
        })

        val cronShutdownHook = ThreadFactoryBuilder()
            .setNameFormat("RobertifyCronShutdownHook")
            .build()
        Runtime.getRuntime().addShutdownHook(cronShutdownHook.newThread {
            logger.info("Killing cron scheduler")
            try {
                cronScheduler.clear()
            } catch (e: SchedulerException) {
                logger.error("I couldn't clear scheduling data!")
            }
        })

        // Setup LavaLink
        lavalink = JdaLavalink(
            getIdFromToken(ConfigKt.BOT_TOKEN),
            ConfigKt.SHARD_COUNT,
        ) { shardId -> shardManager.getShardById(shardId) }

        ConfigKt.LAVA_NODES.forEach { node ->
            lavalink.addNode(node.name, node.uri, node.password)
            logger.info("Registered lava node with address: ${node.uri}")
        }

        // Init caches
        AbstractMongoDatabaseKt.initAllCaches()
        logger.info("Initialized all caches.")

        GuildRedisCacheKt.ins.loadAllGuilds()
        logger.info("All guilds have been loaded into cache.")

        // Setup custom coroutine event manager
        val dispatcher = pool.asCoroutineDispatcher()
        val supervisor = SupervisorJob()
        val handler = CoroutineExceptionHandler { _, throwable ->
            if (throwable !is CancellationException)
                logger.error("Uncaught exception in coroutine event worker", throwable)
            if (throwable is Error) {
                supervisor.cancel()
                throw throwable
            }
        }
        val context = dispatcher + supervisor + handler
        val scope = CoroutineScope(context)
        val coroutineEventManager = CoroutineEventManager(scope, 1.minutes)
        coroutineEventManager.handleShardReady()
        coroutineEventManager.handleGuildReady()
        coroutineEventManager.listener<ShutdownEvent> {
            supervisor.cancel()
        }

        // Build bot connection
        logger.info("Building shard manager...")
        shardManager = defaultShard(
            token = ConfigKt.BOT_TOKEN,
            intents = listOf(GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MESSAGE_REACTIONS),
            enableCoroutines = false
        ) {
            setShardsTotal(ConfigKt.SHARD_COUNT)
            setEventManagerProvider { coroutineEventManager }
            setBulkDeleteSplittingEnabled(false)
            enableCache(CacheFlag.VOICE_STATE)
            disableCache(
                CacheFlag.ACTIVITY,
                CacheFlag.EMOJI,
                CacheFlag.CLIENT_STATUS,
                CacheFlag.ROLE_TAGS,
                CacheFlag.ONLINE_STATUS,
                CacheFlag.STICKER,
                CacheFlag.SCHEDULED_EVENTS
            )
            setVoiceDispatchInterceptor(lavalink.voiceInterceptor)
            setActivity(Activity.listening("Starting up..."))

            val disabledIntents = mutableListOf(
                GatewayIntent.DIRECT_MESSAGE_TYPING,
                GatewayIntent.GUILD_MODERATION,
                GatewayIntent.GUILD_INVITES,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_MESSAGE_TYPING,
                GatewayIntent.GUILD_PRESENCES,
                GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                GatewayIntent.SCHEDULED_EVENTS
            )

            val enabledIntents = mutableListOf(GatewayIntent.GUILD_MESSAGES)

            if (ConfigKt.MESSAGE_CONTENT_ENABLED)
                enabledIntents.add(GatewayIntent.MESSAGE_CONTENT)
            else disabledIntents.add(GatewayIntent.MESSAGE_CONTENT)

            enableIntents(enabledIntents)
            disableIntents(disabledIntents)
        }
        logger.info("Successfully built shard manager")

        val slashCommandManager = SlashCommandManagerKt
        shardManager.registerCommands(
            slashCommandManager.guildCommands
                .merge(slashCommandManager.globalCommands, slashCommandManager.devCommands)
        )

        logger.info("Registered all slash commands.")

        EventManager.registeredEvents
        logger.info("Registered all event controllers.")

        if (ConfigKt.LOAD_COMMANDS)
            AbstractSlashCommandKt.loadAllCommands()

        if (ConfigKt.LOAD_NEEDED_COMMANDS)
            loadNeededGlobalCommands()

        cronScheduler.start()
        initVoteSiteAPIs()

        // Setup SpotifyAPI
        spotifyApi = spotifyAppApi(
            clientId = ConfigKt.SPOTIFY_CLIENT_ID,
            clientSecret = ConfigKt.SPOTIFY_CLIENT_SECRET
        ).build(true)

        if (ConfigKt.hasValue(ENVKt.ROBERTIFY_API_PASSWORD))
            externalApi = RobertifyApi()

        // TODO: Sentry setup

            RobertifyKtorApi.start()
    }

    private fun CoroutineEventManager.handleShardReady() = listener<ReadyEvent> { event ->
        val jda = event.jda
        logger.info("Watching ${event.guildAvailableCount} guilds on shard #${jda.shardInfo.shardId} (${event.guildUnavailableCount} unavailable)")
        BotDBCacheKt.instance.lastStartup = System.currentTimeMillis()
        jda.shardManager?.setPresence(OnlineStatus.ONLINE, Activity.listening("/help"))
    }

    private fun CoroutineEventManager.handleGuildReady() = listener<GuildReadyEvent> { event ->
        val guild = event.guild
        val requestChannelConfig = RequestChannelConfigKt(guild)
        launch {
            val locale = LocaleConfigKt(guild).locale
            try {
                LocaleManagerKt[guild].locale = locale
            } catch (e: ReaderException) {
                logger.error("I couldn't set the locale for ${guild.name}")
            }
        }

        loadNeededSlashCommands(guild)
        rescheduleUnbans(guild)
        RemindersConfigKt(guild).scheduleReminders()

        requestChannelConfig.updateMessage()
        GuildResumeManagerKt(guild).loadTracks()
    }

    fun initVoteSiteAPIs() {
        if (ConfigKt.TOP_GG_TOKEN.isNotEmpty())
            topGGAPI = DiscordBotListAPI.Builder()
                .token(ConfigKt.TOP_GG_TOKEN)
                .botId(getIdFromToken(ConfigKt.BOT_TOKEN))
                .build()
    }

    private fun getIdFromToken(token: String) =
        String(Base64.getDecoder().decode(token.split("\\.".toRegex())[0]))

    private fun List<AbstractSlashCommandKt>.merge(vararg lists: List<AbstractSlashCommandKt>): List<AbstractSlashCommandKt> =
        this + lists.reduce { acc, next -> acc + next }

    private fun loadNeededGlobalCommands() {

    }
}