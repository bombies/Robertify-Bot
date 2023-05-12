package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManager
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.Command
import main.utils.json.logs.LogType
import main.utils.json.logs.LogUtilsKt
import main.utils.json.requestchannel.RequestChannelConfig
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.ShuffleMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class ShuffleCommand : AbstractSlashCommand(
    Command(
        name = "shuffle",
        description = "Shuffle the current queue."
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()
        event.hook.sendEmbed(event.guild!!) {
            handleShuffle(event.guild!!, event.user)
        }.queue()
    }

    fun handleShuffle(guild: Guild, shuffler: User): MessageEmbed {
        val musicManager = RobertifyAudioManager[guild]
        val queueHandler = musicManager.scheduler.queueHandler

        if (queueHandler.isEmpty)
            return RobertifyEmbedUtils.embedMessage(guild, GeneralMessages.NOTHING_PLAYING)
                .build()

        val trackList = queueHandler.contents.shuffled()

        queueHandler.clear()
        queueHandler.addAll(trackList)

        RequestChannelConfig(guild).updateMessage()

        LogUtilsKt(guild).sendLog(
            LogType.QUEUE_SHUFFLE,
            ShuffleMessages.SHUFFLED_LOG,
            Pair("{user}", shuffler.asMention)
        )
        return RobertifyEmbedUtils.embedMessage(guild, ShuffleMessages.SHUFFLED).build()
    }

    override val help: String
        get() = "Shuffle the current queue."
}