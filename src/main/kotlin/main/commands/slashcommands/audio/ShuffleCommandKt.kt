package main.commands.slashcommands.audio

import main.audiohandlers.RobertifyAudioManagerKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.sendWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.json.logs.LogTypeKt
import main.utils.json.logs.LogUtilsKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.ShuffleMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class ShuffleCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "shuffle",
        description = "Shuffle the current queue."
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()
        event.hook.sendWithEmbed(event.guild!!) {
            handleShuffle(event.guild!!, event.user)
        }.queue()
    }

    fun handleShuffle(guild: Guild, shuffler: User): MessageEmbed {
        val musicManager = RobertifyAudioManagerKt[guild]
        val queueHandler = musicManager.scheduler.queueHandler

        if (queueHandler.isEmpty)
            return RobertifyEmbedUtilsKt.embedMessage(guild, GeneralMessages.NOTHING_PLAYING)
                .build()

        val trackList = queueHandler.contents.shuffled()

        queueHandler.clear()
        queueHandler.addAll(trackList)

        RequestChannelConfigKt(guild).updateMessage()

        LogUtilsKt(guild).sendLog(
            LogTypeKt.QUEUE_SHUFFLE,
            ShuffleMessages.SHUFFLED_LOG,
            Pair("{user}", shuffler.asMention)
        )
        return RobertifyEmbedUtilsKt.embedMessage(guild, ShuffleMessages.SHUFFLED).build()
    }

    override val help: String
        get() = "Shuffle the current queue."
}