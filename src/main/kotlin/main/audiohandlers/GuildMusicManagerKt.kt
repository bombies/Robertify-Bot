package main.audiohandlers

import dev.schlaubi.lavakord.audio.Link
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import main.main.RobertifyKt
import main.utils.internal.delegates.SynchronizedProperty
import main.utils.json.requestchannel.RequestChannelConfigKt
import net.dv8tion.jda.api.entities.Guild

class GuildMusicManagerKt(val guild: Guild) {
    val link by SynchronizedProperty { RobertifyKt.lavakord.getLink(guild.id) }
    val player = link.player
    val scheduler = TrackSchedulerKt(guild, link)
    val playerManager = RobertifyAudioManagerKt.ins.playerManager
    var isForcePaused: Boolean = false

    suspend fun clear() {
        val queueHandler = scheduler.queueHandler

        queueHandler.clear()
        queueHandler.clearSavedQueue()
        queueHandler.clearPreviousTracks()

        queueHandler.isTrackRepeating = false
        queueHandler.isQueueRepeating = false

        player.filters.reset()

        if (player.playingTrack != null)
            player.stopTrack()

        if (player.paused)
            player.pause(false)

        // TODO: Requester clearing
        // TODO: Vote skip clearing

        RequestChannelConfigKt(guild).updateMessage()
    }

    suspend fun leave() {
        clear()
        scheduler.stop()
        RobertifyAudioManagerKt.ins.removeMusicManager(guild)
    }

    suspend fun destroy() {
        if (link.state != Link.State.DESTROYED)
            link.destroy()
    }
}