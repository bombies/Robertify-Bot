package main.audiohandlers

import dev.schlaubi.lavakord.audio.Link
import main.commands.slashcommands.audio.SkipCommand
import main.main.Robertify
import main.utils.json.requestchannel.RequestChannelConfig
import net.dv8tion.jda.api.entities.Guild

class GuildMusicManager(val guild: Guild) {
    val link: Link = Robertify.lavaKord.getLink(guild.id)
    val player = link.player
    val scheduler = TrackScheduler(guild, link)
    val voteSkipManager = GuildVoteSkipManager()
    var isForcePaused = false

    suspend fun clear() {
        val queueHandler = scheduler.queueHandler

        queueHandler.clear()
        queueHandler.clearSavedQueue()
        queueHandler.clearPreviousTracks()
        player.filters.reset()

        queueHandler.trackRepeating = false
        queueHandler.queueRepeating = false

        scheduler.clearRequesters()
        SkipCommand().clearVoteSkipInfo(guild)
        RequestChannelConfig(guild).updateMessage()?.await()
    }

    suspend fun leave() {
        clear()
        RobertifyAudioManager.removeMusicManager(guild)
    }

    suspend fun destroy() {
        if (link.state != Link.State.DESTROYED)
            link.destroy()
    }
}