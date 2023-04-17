package main.audiohandlers

import lavalink.client.io.Link
import main.main.Robertify
import main.utils.json.requestchannel.RequestChannelConfigKt
import net.dv8tion.jda.api.entities.Guild

class GuildMusicManagerKt(val guild: Guild) {
    // TODO: Replace with Robertify Kotlin implementation
    val link = Robertify.getLavalink().getLink(guild)
    val player = link.player
    val scheduler = TrackSchedulerKt(guild, link)
    val playerManager = RobertifyAudioManagerKt.ins.playerManager
    var isForcePaused: Boolean = false

    init {
        player.addListener(scheduler)
    }

    fun clear() {
        val queueHandler = scheduler.queueHandler

        queueHandler.clear()
        queueHandler.clearSavedQueue()
        queueHandler.clearPreviousTracks()

        queueHandler.isTrackRepeating = false
        queueHandler.isQueueRepeating = false
        player.filters.clear().commit()

        if (player.playingTrack != null)
            player.stopTrack()

        if (player.isPaused)
            player.isPaused = false

        // TODO: Requester clearing
        // TODO: Vote skip clearing

        RequestChannelConfigKt(guild).updateMessage()
    }

    fun leave() {
        clear()
        scheduler.stop()
        RobertifyAudioManagerKt.ins.removeMusicManager(guild)
    }

    fun destroy() {
        player.removeListener(scheduler)

        if (link.state != Link.State.DESTROYED)
            link.destroy()
    }
}