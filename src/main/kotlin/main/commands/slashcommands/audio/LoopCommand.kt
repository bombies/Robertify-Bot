package main.commands.slashcommands.audio

import lavalink.client.player.IPlayer
import main.audiohandlers.GuildMusicManager
import main.audiohandlers.RobertifyAudioManager
import main.audiohandlers.utils.author
import main.audiohandlers.utils.title
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.Command
import main.utils.component.interactions.slashcommand.models.SubCommand
import main.utils.json.logs.LogType
import main.utils.json.logs.LogUtilsKt
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.LoopMessages
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class LoopCommand : AbstractSlashCommand(
    Command(
        name = "loop",
        description = "Replay the current song being played.",
        subcommands = listOf(
            SubCommand(
                name = "track",
                description = "Toggle looping the song being currently played"
            ),
            SubCommand(
                name = "queue",
                description = "Toggle looping the current queue"
            )
        )
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val musicManager = RobertifyAudioManager[guild]
        val checksEmbed = checks(event.member!!.voiceState!!, event.guild!!.selfMember.voiceState!!, musicManager.player)
        if (checksEmbed != null) {
            event.replyEmbed { checksEmbed }
                .setEphemeral(true)
                .queue()
            return
        }

        when (event.subcommandName) {
            "track" -> event.replyEmbed { handleRepeat(musicManager, event.user) }.queue()
            "queue" -> event.replyEmbed { handleQueueRepeat(musicManager, event.user) }.queue()
        }
    }

    private fun checks(
        selfVoiceState: GuildVoiceState,
        memberVoiceState: GuildVoiceState,
        player: IPlayer
    ): MessageEmbed? {
        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState)
        if (acChecks != null) return acChecks

        val guild = memberVoiceState.guild

        if (player.playingTrack == null)
            return RobertifyEmbedUtils.embedMessage(guild, LoopMessages.LOOP_NOTHING_PLAYING)
                .build()

        return null
    }

    private fun handleRepeat(musicManager: GuildMusicManager, looper: User): MessageEmbed {
        val guild = musicManager.guild
        val player = musicManager.player
        val scheduler = musicManager.scheduler
        val queueHandler = scheduler.queueHandler
        val track = player.playingTrack
            ?: return RobertifyEmbedUtils.embedMessage(
                guild,
                GeneralMessages.NOTHING_PLAYING
            ).build()

        val embed = if (queueHandler.trackRepeating) {
            queueHandler.trackRepeating = false
            RobertifyEmbedUtils.embedMessage(
                guild,
                LoopMessages.LOOP_STOP,
                Pair("{title}", track.title)
            ).build()
        } else {
            queueHandler.trackRepeating = true
            RobertifyEmbedUtils.embedMessage(
                guild,
                LoopMessages.LOOP_START,
                Pair("{title}", track.title)
            ).build()
        }

        LogUtilsKt(guild).sendLog(
            LogType.TRACK_LOOP,
            LoopMessages.LOOP_STOP,
            Pair("{user}", looper.asMention),
            Pair("{status}", if (queueHandler.trackRepeating) "looped" else "unlooped"),
            Pair("{title}", track.title),
            Pair("{author}", track.author)
        )

        return embed
    }

    private fun handleQueueRepeat(musicManager: GuildMusicManager, looper: User): MessageEmbed {
        val scheduler = musicManager.scheduler
        val queueHandler = scheduler.queueHandler
        val guild = musicManager.guild
        val player = musicManager.player
        val embed = if (queueHandler.queueRepeating) {
            queueHandler.queueRepeating = false
            queueHandler.clearSavedQueue()
            RobertifyEmbedUtils.embedMessage(guild, LoopMessages.QUEUE_LOOP_STOP).build()
        } else {
            if (player.playingTrack == null) {
                RobertifyEmbedUtils.embedMessage(guild, GeneralMessages.NOTHING_PLAYING)
                    .build()
            } else {
                val thisTrack = player.playingTrack
                if (queueHandler.isEmpty)
                    RobertifyEmbedUtils.embedMessage(guild, LoopMessages.QUEUE_LOOP_NOTHING)
                        .build()
                else {
                    queueHandler.queueRepeating = true
                    scheduler.addToBeginningOfQueue(thisTrack)
                    queueHandler.setSavedQueue(queueHandler.contents)
                    queueHandler.remove(thisTrack)
                    RobertifyEmbedUtils.embedMessage(guild, LoopMessages.QUEUE_LOOP_START)
                        .build()
                }
            }
        }

        LogUtilsKt(guild).sendLog(
            LogType.TRACK_LOOP,
            LoopMessages.QUEUE_LOOP_LOG,
            Pair("{user}", looper.asMention),
            Pair("{status}", if (queueHandler.queueRepeating) "looped" else "unlooped")
        )
        return embed
    }

    override val help: String
        get() = "Set the song being currently played or the queue to constantly loop"
}