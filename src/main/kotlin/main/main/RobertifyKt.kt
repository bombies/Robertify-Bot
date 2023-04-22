package main.main

import com.google.common.util.concurrent.ThreadFactoryBuilder
import dev.minn.jda.ktx.jdabuilder.defaultShardWithLavakord
import dev.schlaubi.lavakord.LavaKord
import kotlinx.coroutines.runBlocking
import lavalink.client.io.jda.JdaLavalink
import main.audiohandlers.RobertifyAudioManagerKt
import main.commands.slashcommands.SlashCommandManagerKt
import main.commands.slashcommands.SlashCommandManagerKt.registerCommands
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.database.mongodb.AbstractMongoDatabaseKt
import main.utils.database.mongodb.cache.redis.GuildRedisCacheKt
import main.utils.pagination.PaginationEventsKt
import main.utils.resume.GuildResumeManagerKt
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.discordbots.api.client.DiscordBotListAPI
import org.quartz.SchedulerException
import org.quartz.impl.StdSchedulerFactory
import org.slf4j.LoggerFactory
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object RobertifyKt {
    private val logger = LoggerFactory.getLogger(RobertifyKt::class.java)
    val cronScheduler = StdSchedulerFactory.getDefaultScheduler()
    var topGGAPI: DiscordBotListAPI? = null
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

        // Init caches
        AbstractMongoDatabaseKt.initAllCaches()
        logger.info("Initialized all caches.")

        GuildRedisCacheKt.ins.loadAllGuilds()
        logger.info("All guilds have been loaded into cache.")

        // Build bot connection
        logger.info("Building shard manager...")
        runBlocking {
            val lavakordShardManager = defaultShardWithLavakord(
                token = ConfigKt.BOT_TOKEN,
                intents = listOf(GatewayIntent.GUILD_VOICE_STATES),
            ) {
                setShardsTotal(ConfigKt.SHARD_COUNT)
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

                if (ConfigKt.MESSAGE_CONTENT_ENABLED)
                    enabledIntents.add(GatewayIntent.MESSAGE_CONTENT)
                else disabledIntents.add(GatewayIntent.MESSAGE_CONTENT)

                enableIntents(enabledIntents)
                disableIntents(disabledIntents)
            }
            shardManager = lavakordShardManager.shardManager
            lavakord = lavakordShardManager.lavakord
            logger.info("Successfully built shard manager")

            val slashCommandManager = SlashCommandManagerKt
            shardManager.registerCommands(
                slashCommandManager.guildCommands
                    .merge(slashCommandManager.globalCommands, slashCommandManager.devCommands)
            )
            logger.info("Registered all slash commands.")

        }


        // Initialize coroutine listeners
        ListenerKt(shardManager)
        PaginationEventsKt(shardManager)

        // Setup lavakord
        ConfigKt.LAVA_NODES.forEach { node ->
            lavakord.addNode(node.uri.toString(), node.password)
            logger.info("Registered lava node with address: ${node.uri}")
        }

        if (ConfigKt.LOAD_COMMANDS)
            AbstractSlashCommandKt.loadAllCommands()

        if (ConfigKt.LOAD_NEEDED_COMMANDS) {
            // TODO: Load needed commands
        }

        cronScheduler.start()
        initVoteSiteAPIs()

        // TODO: RobertifyAPI setup

        // TODO: Sentry setup

        // TODO: Ktor setup
    }

    fun initVoteSiteAPIs() {
        if (ConfigKt.TOP_GG_TOKEN.isNotEmpty())
            topGGAPI = DiscordBotListAPI.Builder()
                .token(ConfigKt.TOP_GG_TOKEN)
                .botId(getIdFromToken(ConfigKt.BOT_TOKEN))
                .build()
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun getIdFromToken(token: String) =
        Base64.decode(token.split("\\.".toRegex())[0]).toString()

    private fun List<AbstractSlashCommandKt>.merge(vararg lists: List<AbstractSlashCommandKt>): List<AbstractSlashCommandKt> =
        this + lists.reduce { acc, next -> acc + next }
}