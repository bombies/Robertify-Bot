package main.main

import dev.arbjerg.lavalink.protocol.v4.VoiceState
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.schlaubi.lavakord.LavaKord
import dev.schlaubi.lavakord.LavaKordOptions
import dev.schlaubi.lavakord.MutableLavaKordOptions
import dev.schlaubi.lavakord.audio.Link
import dev.schlaubi.lavakord.audio.Node
import dev.schlaubi.lavakord.audio.internal.AbstractLavakord
import dev.schlaubi.lavakord.audio.internal.AbstractLink
import dev.schlaubi.lavakord.jda.LShardManager
import dev.schlaubi.lavakord.jda.LavakordJdaBase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.session.SessionRecreateEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import kotlin.coroutines.CoroutineContext

fun DefaultShardManagerBuilder.applyRobertifyLavakord(shardManager: RobertifyLavaKordShardManager): DefaultShardManagerBuilder =
    apply {
        addEventListeners(shardManager)
        setVoiceDispatchInterceptor(shardManager)
    }

suspend fun ShardManager.robertifyLavakord(
    shardManager: RobertifyLavaKordShardManager,
    executor: CoroutineContext? = null,
    options: MutableLavaKordOptions = MutableLavaKordOptions(),
    builder: MutableLavaKordOptions.() -> Unit = {}
): LavaKord {
    shardManager.shardManager = this
    val jdaProvider: (Int) -> JDA =
        { shardId -> getShardById(shardId) ?: error("Could not find shard with id: $shardId") }
    val settings = options.apply(builder).seal()
    val lavakord = RobertifyJDALavakord(
        shardManager.shardManager,
        jdaProvider,
        executor ?: (Dispatchers.IO + SupervisorJob()),
        retrieveApplicationInfo().submit().await().idLong.toULong(),
        shardsTotal,
        settings
    )
    shardManager.internalLavakord = lavakord
    return lavakord
}

class RobertifyLavaKordShardManager : AbstractRobertifyLavakordJda(), LShardManager {
    override lateinit var shardManager: ShardManager
}

sealed class AbstractRobertifyLavakordJda : VoiceDispatchInterceptor, EventListener, LavakordJdaBase {
    /**
     * The [LavaKord] instance that for this [JDA]/[ShardManager].
     */
    override val lavakord: LavaKord get() = internalLavakord ?: error("Lavakord has not been initialized yet")
    internal var internalLavakord: RobertifyJDALavakord? = null

    /**
     * @see EventListener.onEvent
     */
    override fun onEvent(event: GenericEvent): Unit = internalLavakord?.onEvent(event) ?: Unit

    /**
     * @see VoiceDispatchInterceptor.onVoiceServerUpdate
     */
    override fun onVoiceServerUpdate(update: VoiceDispatchInterceptor.VoiceServerUpdate): Unit =
        internalLavakord?.onVoiceServerUpdate(update) ?: Unit

    /**
     * @see VoiceDispatchInterceptor.onVoiceServerUpdate
     */
    override fun onVoiceStateUpdate(update: VoiceDispatchInterceptor.VoiceStateUpdate): Boolean =
        internalLavakord?.onVoiceStateUpdate(update) ?: false
}

class RobertifyJDALink(override val lavakord: RobertifyJDALavakord, node: Node, guildId: ULong) :
    AbstractLink(node, guildId) {

    private val shardId: Int
        get() = lavakord.getShardIdForGuild(guildId)
    private val jda: JDA
        get() = lavakord.jdaProvider(shardId)
    private val guild: Guild
        get() = jda.getGuildById(guildId.toLong()) ?: error("Could not find guild: $guildId")

    override suspend fun connectAudio(voiceChannelId: ULong) {
        val guild = guild
        val channel =
            guild.getVoiceChannelById(voiceChannelId.toLong()) ?: error("Could not find voice channel: $voiceChannelId")

        jda.directAudioController.connect(channel)
    }

    override suspend fun disconnectAudio() = jda.directAudioController.disconnect(guild)

}

class RobertifyJDALavakord(
    internal val shardManager: ShardManager,
    internal val jdaProvider: (Int) -> JDA,
    override val coroutineContext: CoroutineContext,
    userId: ULong,
    val shardsTotal: Int,
    options: LavaKordOptions
) : AbstractLavakord(userId, options), VoiceDispatchInterceptor {

    override fun buildNewLink(guildId: ULong, node: Node): Link = RobertifyJDALink(this, node, guildId)

    override fun onVoiceServerUpdate(update: VoiceDispatchInterceptor.VoiceServerUpdate) {
        val link = getLink(update.guildIdLong.toULong())

        launch {
            forwardVoiceEvent(
                link,
                update.guildIdLong.toULong(),
                VoiceState(
                    update.token,
                    update.endpoint,
                    update.sessionId
                )
            )
        }
    }

    override fun onVoiceStateUpdate(update: VoiceDispatchInterceptor.VoiceStateUpdate): Boolean {
        val channel = update.channel
        val link = getLink(update.guildIdLong.toULong())
        require(link is RobertifyJDALink)

        // Null channel means disconnected
        if (channel == null) {
            if (link.state != Link.State.DESTROYED) {
                link.state = Link.State.DESTROYED
            }
        } else {
            link.lastChannelId = channel.idLong.toULong()
            link.state = Link.State.CONNECTED
        }

        return link.state == Link.State.CONNECTED
    }

    init {
        shardManager.listener<SessionRecreateEvent> {
            if (options.link.autoReconnect) {
                linksMap.forEach { (_, link) ->
                    val lastChannel = link.lastChannelId
                    if (lastChannel != null && it.jda.getVoiceChannelById(lastChannel.toLong()) != null) {
                        link.connectAudio(lastChannel)
                    }
                }
            }
        }
    }

    fun onEvent(event: GenericEvent) {
        shardManager.listener<GenericEvent> {
            if (event is SessionRecreateEvent) {
                launch {
                    if (options.link.autoReconnect) {
                        linksMap.forEach { (_, link) ->
                            val lastChannel = link.lastChannelId
                            if (lastChannel != null && event.jda.getVoiceChannelById(lastChannel.toLong()) != null) {
                                link.connectAudio(lastChannel)
                            }
                        }
                    }
                }

            }
        }
    }
}

fun RobertifyJDALavakord.getShardIdForGuild(snowflake: ULong): Int =
    ((snowflake shr 22) % shardsTotal.toUInt()).toInt()
