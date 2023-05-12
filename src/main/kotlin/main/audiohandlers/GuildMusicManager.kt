package main.audiohandlers

import lavalink.client.io.Link
import lavalink.client.player.LavalinkPlayer
import main.commands.slashcommands.audio.SkipCommand
import main.main.Robertify
import main.utils.json.requestchannel.RequestChannelConfig
import net.dv8tion.jda.api.entities.Guild

class GuildMusicManager(val guild: Guild) {
    val link = Robertify.lavalink.getLink(guild)
    val player: LavalinkPlayer = link.player
    val scheduler = TrackScheduler(guild, link)
    val voteSkipManager = GuildVoteSkipManager()
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

        queueHandler.trackRepeating = false
        queueHandler.queueRepeating = false

        scheduler.clearRequesters()
        SkipCommand().clearVoteSkipInfo(guild)
        RequestChannelConfig(guild).updateMessage()
    }

    fun leave() {
        clear()
        RobertifyAudioManager.removeMusicManager(guild)
    }

    fun destroy() {
        player.removeListener(scheduler)

        if (link.state != Link.State.DESTROYED)
            link.destroy()
    }
}