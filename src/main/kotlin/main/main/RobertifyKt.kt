package main.main

import com.google.common.util.concurrent.ThreadFactoryBuilder
import dev.minn.jda.ktx.jdabuilder.defaultShard
import kotlinx.coroutines.runBlocking
import lavalink.client.io.jda.JdaLavalink
import main.audiohandlers.RobertifyAudioManagerKt
import main.commands.slashcommands.SlashCommandManagerKt
import main.commands.slashcommands.SlashCommandManagerKt.registerCommands
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.database.mongodb.AbstractMongoDatabaseKt
import main.utils.database.mongodb.cache.redis.GuildRedisCacheKt
import main.events.EventManager
import main.events.EventManager.registerEvents
import main.utils.resume.GuildResumeManagerKt
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.discordbots.api.client.DiscordBotListAPI
import org.quartz.SchedulerException
import org.quartz.impl.StdSchedulerFactory
import org.slf4j.LoggerFactory
import java.util.Base64

object RobertifyKt {
    private val logger = LoggerFactory.getLogger(RobertifyKt::class.java)
    val cronScheduler = StdSchedulerFactory.getDefaultScheduler()
    var topGGAPI: DiscordBotListAPI? = null
    lateinit var lavalink: JdaLavalink
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

        // Build bot connection
        logger.info("Building shard manager...")
        runBlocking {
            shardManager = defaultShard(
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

            shardManager.registerEvents(EventManager.getRegisteredEvents())
            logger.info("Registered all event controllers")
        }

        if (ConfigKt.LOAD_COMMANDS)
            AbstractSlashCommandKt.loadAllCommands()

        if (ConfigKt.LOAD_NEEDED_COMMANDS)
            loadNeededGlobalCommands()

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

    private fun getIdFromToken(token: String) =
        String(Base64.getDecoder().decode(token.split("\\.".toRegex())[0]))

    private fun List<AbstractSlashCommandKt>.merge(vararg lists: List<AbstractSlashCommandKt>): List<AbstractSlashCommandKt> =
        this + lists.reduce { acc, next -> acc + next }

    private fun loadNeededGlobalCommands() {

    }
}