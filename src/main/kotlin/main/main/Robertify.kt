package main.main

//import main.utils.resume.GuildResumeManager
import api.RobertifyKtorApi
import com.adamratzman.spotify.SpotifyAppApi
import com.adamratzman.spotify.spotifyAppApi
import com.google.common.util.concurrent.ThreadFactoryBuilder
import dev.arbjerg.lavalink.client.*
import dev.arbjerg.lavalink.client.event.TrackEndEvent
import dev.arbjerg.lavalink.client.event.TrackExceptionEvent
import dev.arbjerg.lavalink.client.event.TrackStartEvent
import dev.arbjerg.lavalink.client.event.TrackStuckEvent
import dev.arbjerg.lavalink.client.loadbalancing.RegionGroup
import dev.arbjerg.lavalink.libraries.jda.JDAVoiceUpdateListener
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.jdabuilder.injectKTX
import dev.minn.jda.ktx.util.SLF4J
import io.sentry.Sentry
import kotlinx.coroutines.runBlocking
import main.audiohandlers.RobertifyAudioManager
import main.commands.slashcommands.SlashCommandManager
import main.commands.slashcommands.SlashCommandManager.registerCommands
import main.constants.ENV
import main.events.EventManager
import main.main.Listener.Companion.loadNeededSlashCommands
import main.main.Listener.Companion.rescheduleUnbans
import main.utils.api.robertify.RobertifyApi
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.database.mongodb.AbstractMongoDatabase
import main.utils.database.mongodb.cache.BotDBCache
import main.utils.json.reminders.RemindersConfig
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.discordbots.api.client.DiscordBotListAPI
import org.quartz.SchedulerException
import org.quartz.impl.StdSchedulerFactory
import java.util.*

object Robertify {
    private val logger by SLF4J
    val cronScheduler = StdSchedulerFactory.getDefaultScheduler()
    var topGGAPI: DiscordBotListAPI? = null
    lateinit var lavalink: LavalinkClient
        private set
    lateinit var shardManager: ShardManager
        private set
    lateinit var spotifyApi: SpotifyAppApi
        private set
    lateinit var externalApi: RobertifyApi
        private set


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
                shardManager.guilds
                    .filter { guild -> guild.selfMember.voiceState != null }
                    .forEach { guild -> guild.jda.directAudioController.disconnect(guild) }
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


        // Setup LavaLink
        logger.info("Setting up LavaLink...")
        lavalink = LavalinkClient(getIdFromToken(Config.BOT_TOKEN).toLong())
        lavalink.on<dev.arbjerg.lavalink.client.event.ReadyEvent>()
            .subscribe { event ->
                logger.info("LavaLink ready on node ${event.node.name}. Session ID is ${event.sessionId}")
            }
        logger.debug("LavaLink ReadyEvent registered.")

        Config.LAVA_NODES.forEach { node ->
            val lavaNode = lavalink.addNode(
                NodeOptions.Builder(
                    serverUri = node.uri,
                    password = node.password,
                    name = node.name,
                    regionFilter = RegionGroup.US
                ).build()
            )

            lavaNode.on<TrackStartEvent>().subscribe { event ->
                val musicManager = RobertifyAudioManager.getMusicManager(event.guildId)
                musicManager.scheduler.onTrackStart(event)
            }
            lavaNode.on<TrackEndEvent>().subscribe { event ->
                val musicManager = RobertifyAudioManager.getMusicManager(event.guildId)
                musicManager.scheduler.onTrackEnd(event)
            }
            lavaNode.on<TrackStuckEvent>().subscribe { event ->
                val musicManager = RobertifyAudioManager.getMusicManager(event.guildId)
                musicManager.scheduler.onTrackStuck(event)
            }
            lavaNode.on<TrackExceptionEvent>().subscribe { event ->
                val musicManager = RobertifyAudioManager.getMusicManager(event.guildId)
                musicManager.scheduler.onTrackException(event)
            }

            logger.info("Registered lava node with address: ${node.uri}")
        }

        // Build bot connection
        logger.info("Building shard manager...")
        val shardManagerBuilder = DefaultShardManagerBuilder.createLight(
            Config.BOT_TOKEN,
            listOf(GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MESSAGE_REACTIONS),
        )
            .apply {
                setShardsTotal(Config.SHARD_COUNT)
                injectKTX()
                setMemberCachePolicy(MemberCachePolicy.DEFAULT)
                setBulkDeleteSplittingEnabled(false)
                enableCache(CacheFlag.VOICE_STATE)
                setVoiceDispatchInterceptor(JDAVoiceUpdateListener(lavalink))
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

        shardManager = shardManagerBuilder.build()
        logger.info("Successfully built shard manager")

        shardManager.handleShardReady()
        shardManager.handleGuildReady()

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

        RobertifyAudioManager.scheduleCleanup()
        RobertifyKtorApi.start()
    }

    private fun ShardManager.handleShardReady() = listener<ReadyEvent> { event ->
        val jda = event.jda
        logger.info("Watching ${event.guildAvailableCount} guilds on shard #${jda.shardInfo.shardId} (${event.guildUnavailableCount} unavailable)")
        BotDBCache.instance.lastStartup = System.currentTimeMillis()
        jda.shardManager?.setPresence(OnlineStatus.ONLINE, Activity.listening("/help"))
    }

    private fun ShardManager.handleGuildReady() = listener<GuildReadyEvent> { event ->
        val guild = event.guild
        loadNeededSlashCommands(guild)

        try {
            rescheduleUnbans(guild)
        } catch (e: Exception) {
            logger.error("Failed to reschedule unbans for guild ${guild.id}", e)
        }

        try {
            RemindersConfig(guild).scheduleReminders()
        } catch (e: Exception) {
            logger.error("Failed to schedule reminders for guild ${guild.id}", e)
        }
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