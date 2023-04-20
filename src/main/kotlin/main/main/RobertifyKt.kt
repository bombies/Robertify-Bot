package main.main

import com.google.common.util.concurrent.ThreadFactoryBuilder
import dev.minn.jda.ktx.events.CoroutineEventManager
import dev.minn.jda.ktx.jdabuilder.defaultShardWithLavakord
import dev.schlaubi.lavakord.LavaKord
import kotlinx.coroutines.*
import lavalink.client.io.jda.JdaLavalink
import main.audiohandlers.RobertifyAudioManagerKt
import main.commands.slashcommands.SlashCommandManagerKt
import main.constants.ENVKt
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.database.mongodb.AbstractMongoDatabaseKt
import main.utils.database.mongodb.cache.redis.GuildRedisCacheKt
import main.utils.resume.GuildResumeManagerKt
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.session.ShutdownEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.discordbots.api.client.DiscordBotListAPI
import org.quartz.SchedulerException
import org.quartz.impl.StdSchedulerFactory
import org.slf4j.LoggerFactory
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration.Companion.minutes

object RobertifyKt {
    private val logger = LoggerFactory.getLogger(RobertifyKt::class.java)
    private val threadPool = Executors.newScheduledThreadPool(ForkJoinPool.getCommonPoolParallelism().coerceAtLeast(3))
    val cronScheduler = StdSchedulerFactory.getDefaultScheduler()
    var topGGAPI: DiscordBotListAPI? = null
    lateinit var coroutineEventManager: CoroutineEventManager
        private set
    lateinit var lavalink: JdaLavalink
        private set
    lateinit var lavakord: LavaKord
        private set
    lateinit var shardManager: ShardManager
        private set

    @JvmStatic
    fun main(args: Array<String>) {
        // Setup graceful shutdown hooks
        val mainShutdownHook = ThreadFactoryBuilder()
            .setNameFormat("RobertifyShutdownHook")
            .build()
        Runtime.getRuntime().addShutdownHook(mainShutdownHook.newThread {
            logger.info("Destroying all players (If any left)")
            RobertifyAudioManagerKt.ins.musicManagers
                .values
                .forEach { musicManager ->
                    if (musicManager.player.playingTrack != null)
                        GuildResumeManagerKt(musicManager.guild).saveTracks()
                    musicManager.destroy()
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
        AbstractMongoDatabaseKt.initAllCaches()
        logger.info("Initialized all caches.")

        GuildRedisCacheKt.ins!!.loadAllGuilds()
        logger.info("All guilds have been loaded into cache.")

        // Build bot connection
        logger.info("Building shard manager...")

        runBlocking {
            val lavakordShardManager = defaultShardWithLavakord(
                token = ConfigKt.botToken,
                intents = listOf(GatewayIntent.GUILD_VOICE_STATES),
            ) {
                setShardsTotal(ConfigKt.shardCount)
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

                if (ConfigKt[ENVKt.MESSAGE_CONTENT_INTENT_ENABLED].toBoolean())
                    enabledIntents.add(GatewayIntent.MESSAGE_CONTENT)
                else disabledIntents.add(GatewayIntent.MESSAGE_CONTENT)

                enableIntents(enabledIntents)
                disableIntents(disabledIntents)

                val slashCommandManager = SlashCommandManagerKt.ins
                slashCommandManager.guildCommands
                    .merge(slashCommandManager.globalCommands, slashCommandManager.devCommands)
                    .forEach { cmd ->
                        addEventListeners(cmd)
                        logger.debug("Registered the \"${cmd.info.name}\" command.")
                    }
            }
            shardManager = lavakordShardManager.shardManager
            lavakord = lavakordShardManager.lavakord
            logger.info("Successfully built shard manager")

            // Initialize coroutine listeners
            ListenerKt(shardManager)
        }

        // Setup lavakord
        ConfigKt.lavaNodes.forEach { node ->
            lavakord.addNode(node.uri.toString(), node.password)
            logger.info("Registered lava node with address: ${node.uri}")
        }

        if (ConfigKt.loadCommands())
            AbstractSlashCommandKt.loadAllCommands()

        if (ConfigKt.loadNeededCommands()) {
            // TODO: Load needed commands
        }

        cronScheduler.start()
        initVoteSiteAPIs()

        // TODO: RobertifyAPI setup

        // TODO: Sentry setup

        // TODO: Ktor setup
    }

    fun initVoteSiteAPIs() {
        if (ConfigKt[ENVKt.TOP_GG_TOKEN].isNotEmpty())
            topGGAPI = DiscordBotListAPI.Builder()
                .token(ConfigKt[ENVKt.TOP_GG_TOKEN])
                .botId(getIdFromToken(ConfigKt.botToken))
                .build()
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun getIdFromToken(token: String) =
        Base64.decode(token.split("\\.".toRegex())[0]).toString()

    private fun List<AbstractSlashCommandKt>.merge(vararg lists: List<AbstractSlashCommandKt>): List<AbstractSlashCommandKt> =
        this + lists.reduce { acc, next -> acc + next }
}