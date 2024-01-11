package main.audiohandlers

import dev.arbjerg.lavalink.client.LavalinkPlayer
import dev.arbjerg.lavalink.client.Link
import dev.arbjerg.lavalink.protocol.v4.Filters
import main.commands.slashcommands.audio.SkipCommand
import main.main.Robertify
import main.utils.json.requestchannel.RequestChannelConfig
import net.dv8tion.jda.api.entities.Guild

class GuildMusicManager(val guild: Guild) {
    val link: Link
        get() = Robertify.lavalink.getLink(guild.idLong)
    val scheduler = TrackScheduler(guild)
    val voteSkipManager = GuildVoteSkipManager()
    val player: LavalinkPlayer?
        get() = link.getPlayer().block()
    var isForcePaused = false

    fun usePlayer(playerCallback: (LavalinkPlayer) -> Unit) {
        link.getPlayer().subscribe(playerCallback)
    }


    fun clear() {
        val queueHandler = scheduler.queueHandler

        queueHandler.clear()
        queueHandler.clearSavedQueue()
        queueHandler.clearPreviousTracks()

        if (link.getPlayer().block() != null)
            link.updatePlayer {
                it.setFilters(Filters())
            }.subscribe()

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
}