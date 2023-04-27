package main.audiohandlers

import lavalink.client.io.Link
import lavalink.client.player.LavalinkPlayer
import main.commands.slashcommands.audio.SkipCommandKt
import main.main.RobertifyKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import net.dv8tion.jda.api.entities.Guild

class GuildMusicManagerKt(val guild: Guild) {
    val link = RobertifyKt.lavalink.getLink(guild)
    val player: LavalinkPlayer = link.player
    val scheduler = TrackSchedulerKt(guild, link)
    val voteSkipManager = GuildVoteSkipManagerKt()
    var isForcePaused = false

    init {
        player.addListener(scheduler)
    }

    fun clear() {
        val queueHandler = scheduler.queueHandler

        queueHandler.clear()
        queueHandler.clearSavedQueue()
        queueHandler.clearPreviousTracks()
        player.filters.clear().commit()

        queueHandler.isTrackRepeating = false
        queueHandler.isQueueRepeating = false

        scheduler.clearRequesters()
        SkipCommandKt().clearVoteSkipInfo(guild)
        RequestChannelConfigKt(guild).updateMessage()
    }

    fun leave() {
        clear()
        RobertifyAudioManagerKt.removeMusicManager(guild)
    }

    fun destroy() {
        player.removeListener(scheduler)

        if (link.state != Link.State.DESTROYED)
            link.destroy()
    }
}