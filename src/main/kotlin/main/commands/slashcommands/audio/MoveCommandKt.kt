package main.commands.slashcommands.audio

import main.audiohandlers.QueueHandlerKt
import main.audiohandlers.RobertifyAudioManagerKt
import main.audiohandlers.utils.author
import main.audiohandlers.utils.title
import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.RobertifyEmbedUtilsKt.Companion.sendWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.json.logs.LogTypeKt
import main.utils.json.logs.LogUtilsKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.MoveMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

class MoveCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "move",
        description = "Rearrange the position of tracks in the queue.",
        options = listOf(
            CommandOptionKt(
                type = OptionType.INTEGER,
                name = "id",
                description = "The ID of the track in the queue to move."
            ),
            CommandOptionKt(
                type = OptionType.INTEGER,
                name = "position",
                description = "The position to move the track to."
            )
        )
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val memberVoiceState = event.member!!.voiceState!!
        val selfVoiceState = guild.selfMember.voiceState!!
        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState)
        if (acChecks != null) {
            event.replyWithEmbed { acChecks }.setEphemeral(true).queue()
            return
        }

        event.deferReply().queue()
        val queueHandler = RobertifyAudioManagerKt[guild]
            .scheduler
            .queueHandler
        val id = event.getRequiredOption("id").asInt
        val pos = event.getRequiredOption("position").asInt

        event.hook.sendWithEmbed {
            handleMove(
                guild = guild,
                mover = event.user,
                queueHandler = queueHandler,
                id = id,
                position = pos
            )
        }.queue()
    }

    private fun handleMove(
        guild: Guild,
        mover: User,
        queueHandler: QueueHandlerKt,
        id: Int,
        position: Int
    ): MessageEmbed {
        if (queueHandler.isEmpty)
            return RobertifyEmbedUtilsKt.embedMessage(guild, GeneralMessages.NOTHING_IN_QUEUE)
                .build()

        val trackList = queueHandler.contents.toMutableList()

        if (id <= 0 || id > trackList.size)
            return RobertifyEmbedUtilsKt.embedMessage(guild, MoveMessages.INVALID_SONG_ID)
                .build()
        else if (position <= 0 || position > trackList.size)
            return RobertifyEmbedUtilsKt.embedMessage(guild, MoveMessages.INVALID_POSITION_ID)
                .build()

        val prevList = trackList.toMutableList()
        queueHandler.clear()
        prevList.remove(trackList[id - 1])
        prevList.add(position - 1, trackList[id - 1])

        if (!queueHandler.addAll(prevList)) {
            queueHandler.addAll(trackList)
            return RobertifyEmbedUtilsKt.embedMessage(
                guild,
                MoveMessages.COULDNT_MOVE,
                Pair("{id}", id.toString())
            ).build()
        }

        RequestChannelConfigKt(guild).updateMessage()

        val movedTrack = trackList[id - 1]
        LogUtilsKt(guild).sendLog(
            LogTypeKt.TRACK_MOVE,
            MoveMessages.MOVED_LOG,
            Pair("{user}", mover.asMention),
            Pair("{title}", movedTrack.title),
            Pair("{author}", movedTrack.author),
            Pair("{position}", position.toString())
        )

        return RobertifyEmbedUtilsKt.embedMessage(
            guild,
            MoveMessages.MOVED,
            Pair("{title}", movedTrack.title),
            Pair("{author}", movedTrack.author),
            Pair("{position}", position.toString())
        ).build()
    }

    override val help: String
        get() = """
                Move a specific track to a specific position in the queue

                Usage: `/move <id> <position>`"""

}