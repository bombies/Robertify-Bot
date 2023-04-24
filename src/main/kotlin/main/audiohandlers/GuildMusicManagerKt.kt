package main.audiohandlers

import dev.schlaubi.lavakord.audio.Link
import main.commands.slashcommands.audio.SkipCommandKt
import main.main.RobertifyKt
import main.utils.internal.delegates.SynchronizedProperty
import main.utils.json.requestchannel.RequestChannelConfigKt
import net.dv8tion.jda.api.entities.Guild

class GuildMusicManagerKt(val guild: Guild) {
    val link by SynchronizedProperty { RobertifyKt.lavakord.getLink(guild.id) }
    val player = link.player
    val scheduler = TrackSchedulerKt(guild, link)
    val voteSkipManager = GuildVoteSkipManagerKt()
    val playerManager = RobertifyAudioManagerKt.playerManager
    var isForcePaused = false


    suspend fun clear() {
        val queueHandler = scheduler.queueHandler

        queueHandler.clear()
        queueHandler.clearSavedQueue()
        queueHandler.clearPreviousTracks()

        queueHandler.isTrackRepeating = false
        queueHandler.isQueueRepeating = false

        player.filters.reset()

        scheduler.clearRequesters()
        SkipCommandKt().clearVoteSkipInfo(guild)
        RequestChannelConfigKt(guild).updateMessage()
    }

    suspend fun leave() {
        clear()
        RobertifyAudioManagerKt.removeMusicManager(guild)
    }

    suspend fun destroy() {
        if (link.state != Link.State.DESTROYED)
            link.destroy()
    }
}