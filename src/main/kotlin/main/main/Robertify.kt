package main.main

//import main.utils.resume.GuildResumeManager
import api.RobertifyKtorApi
import com.adamratzman.spotify.SpotifyAppApi
import com.adamratzman.spotify.spotifyAppApi
import com.google.common.util.concurrent.ThreadFactoryBuilder
import dev.minn.jda.ktx.events.CoroutineEventManager
import dev.minn.jda.ktx.util.SLF4J
import dev.schlaubi.lavakord.LavaKord
import dev.schlaubi.lavakord.MutableLavaKordOptions
import dev.schlaubi.lavakord.jda.LavaKordShardManager
import dev.schlaubi.lavakord.jda.applyLavakord
import dev.schlaubi.lavakord.jda.lavakord
import dev.schlaubi.lavakord.plugins.lavasrc.LavaSrc
import io.sentry.Sentry
import kotlinx.coroutines.*
import main.audiohandlers.RobertifyAudioManager
import main.commands.slashcommands.SlashCommandManager
import main.commands.slashcommands.SlashCommandManager.registerCommands
import main.constants.ENV
import main.events.EventManager
import main.main.Listener.Companion.loadNeededSlashCommands
import main.main.Listener.Companion.rescheduleUnbans
import main.utils.GeneralUtils
import main.utils.api.robertify.RobertifyApi
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.database.mongodb.AbstractMongoDatabase
import main.utils.database.mongodb.cache.BotDBCache
import main.utils.database.mongodb.cache.redis.GuildRedisCache
import main.utils.json.locale.LocaleConfig
import main.utils.json.reminders.RemindersConfig
import main.utils.json.requestchannel.RequestChannelConfig
import main.utils.locale.LocaleManager
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.events.session.ShutdownEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.discordbots.api.client.DiscordBotListAPI
import org.quartz.SchedulerException
import org.quartz.impl.StdSchedulerFactory
import org.yaml.snakeyaml.reader.ReaderException
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds

object Robertify {
    private val logger by SLF4J
    val cronScheduler = StdSchedulerFactory.getDefaultScheduler()
    var topGGAPI: DiscordBotListAPI? = null
    lateinit var lavaKord: LavaKord
        private set
    lateinit var shardManager: ShardManager
        private set
    lateinit var spotifyApi: SpotifyAppApi
        private set
    lateinit var externalApi: RobertifyApi
        private set
    lateinit var coroutineEventManager: CoroutineEventManager
        private set

    private val lavakordShardManager = LavaKordShardManager()


    fun main() = runBlocking {
        if (Config.hasValue(ENV.SENTRY_DSN))
            Sentry.init { options ->
                options.dsn = Config.SENTRY_DSN
                options.environment = Config.ENVIRONMENT
            }

        // Setup graceful shutdown hooks
        val mainShutdownHook = ThreadFactoryBuilder()
            .setNameFormat("RobertifyShutdownHook")
            .build()
        Runtime.getRuntime().addShutdownHook(mainShutdownHook.newThread {
            logger.info("Destroying all players (If any left)")
            runBlocking {
                RobertifyAudioManager.musicManagers
                    .values
                    .forEach { musicManager ->
//                        if (musicManager.player.playingTrack != null)
//                            GuildResumeManager(musicManager.guild).saveTracks()
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

        // Init caches
        AbstractMongoDatabase.initAllCaches()
        logger.info("Initialized all caches.")

        GuildRedisCache.ins.loadAllGuilds()
        logger.info("All guilds have been loaded into cache.")

        // Setup custom coroutine event manager
        val (dispatcher, supervisor, handler) = GeneralUtils.generateHandleCoroutineContextComponents()
        val context = dispatcher + supervisor + handler
        val scope = CoroutineScope(context)
        coroutineEventManager = CoroutineEventManager(scope, 30.seconds)
        coroutineEventManager.handleShardReady()
        coroutineEventManager.handleGuildReady()
        coroutineEventManager.listener<ShutdownEvent> {
            supervisor.cancel()
        }

        // Build bot connection
        logger.info("Building shard manager...")
        val shardManagerBuilder = DefaultShardManagerBuilder.createDefault(
            Config.BOT_TOKEN,
            listOf(GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MESSAGE_REACTIONS),
        )
            .apply {
                setShardsTotal(Config.SHARD_COUNT)
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

                if (Config.MESSAGE_CONTENT_ENABLED)
                    enabledIntents.add(GatewayIntent.MESSAGE_CONTENT)
                else disabledIntents.add(GatewayIntent.MESSAGE_CONTENT)

                enableIntents(enabledIntents)
                disableIntents(disabledIntents)
            }

        shardManagerBuilder.applyLavakord(lavakordShardManager)
        shardManager = shardManagerBuilder.build()
        logger.info("Successfully built shard manager")

        val slashCommandManager = SlashCommandManager
        shardManager.registerCommands(
            slashCommandManager.guildCommands
                .merge(slashCommandManager.globalCommands, slashCommandManager.devCommands)
        )

        logger.info("Registered all slash commands.")

        EventManager.registeredEvents
        logger.info("Registered all event controllers.")

        if (Config.LOAD_COMMANDS)
            AbstractSlashCommand.loadAllCommands()

        if (Config.LOAD_NEEDED_COMMANDS)
            loadNeededGlobalCommands()

        cronScheduler.start()
        initVoteSiteAPIs()

        // Setup SpotifyAPI
        spotifyApi = spotifyAppApi(
            clientId = Config.SPOTIFY_CLIENT_ID,
            clientSecret = Config.SPOTIFY_CLIENT_SECRET
        ).build(true)

        if (Config.hasValue(ENV.ROBERTIFY_API_PASSWORD))
            externalApi = RobertifyApi()

        RobertifyKtorApi.start()
    }

    private fun CoroutineEventManager.handleShardReady() = listener<ReadyEvent> { event ->
        val jda = event.jda
        logger.info("Watching ${event.guildAvailableCount} guilds on shard #${jda.shardInfo.shardId} (${event.guildUnavailableCount} unavailable)")
        BotDBCache.instance.lastStartup = System.currentTimeMillis()
        jda.shardManager?.setPresence(OnlineStatus.ONLINE, Activity.listening("/help"))

        logger.info("Setting up LavaKord...")
        val (dispatcher, supervisor, handler) = GeneralUtils.generateHandleCoroutineContextComponents()
        lavaKord = shardManager.lavakord(
            lavakordShardManager, dispatcher + supervisor + handler, options = MutableLavaKordOptions(
                link = MutableLavaKordOptions.LinkConfig(
                    showTrace = true
                )
            )
        ) {
            plugins {
                install(LavaSrc)
            }
        }

        Config.LAVA_NODES.forEach { node ->
            lavaKord.addNode(
                serverUri = node.uri.toString(),
                password = node.password,
                name = node.name
            )
            logger.info("Registered lava node with address: ${node.uri}")
        }
        logger.info("LavaKord ready")

        shardManager.guildCache.forEach { guild ->
            RequestChannelConfig(guild).updateMessage()?.await()
        }
    }

    private fun CoroutineEventManager.handleGuildReady() = listener<GuildReadyEvent> { event ->
        val guild = event.guild
        launch {
            val locale = LocaleConfig(guild).locale
            try {
                LocaleManager[guild].locale = locale
            } catch (e: ReaderException) {
                logger.error("I couldn't set the locale for ${guild.name}")
            }
        }

        loadNeededSlashCommands(guild)
        rescheduleUnbans(guild)
        RemindersConfig(guild).scheduleReminders()

//        GuildResumeManager(guild).loadTracks()
    }

    fun initVoteSiteAPIs() {
        if (Config.TOP_GG_TOKEN.isNotEmpty())
            topGGAPI = DiscordBotListAPI.Builder()
                .token(Config.TOP_GG_TOKEN)
                .botId(getIdFromToken(Config.BOT_TOKEN))
                .build()
    }

    private fun getIdFromToken(token: String) =
        String(Base64.getDecoder().decode(token.split("\\.".toRegex())[0]))

    private fun List<AbstractSlashCommand>.merge(vararg lists: List<AbstractSlashCommand>): List<AbstractSlashCommand> =
        this + lists.reduce { acc, next -> acc + next }

    private fun loadNeededGlobalCommands() {

    }
}

fun main(args: Array<String>) = Robertify.main()