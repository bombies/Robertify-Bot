package main.commands.slashcommands.audio

import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.json.autoplay.AutoPlayConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.GeneralMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class AutoPlayCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "autoplay",
        description = "Play recommended tracks at the end of your queue",
        isPremium = false,
        djOnly = true
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbed(event.guild!!) { handleAutoPlay(event.guild!!) }
            .queue()
    }

    private fun handleAutoPlay(guild: Guild): MessageEmbed {
        val localeManager = LocaleManagerKt[guild]
        val autoPlayConfig = AutoPlayConfigKt(guild)

        return if (autoPlayConfig.status) {
            autoPlayConfig.status = false
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                GeneralMessages.GENERAL_TOGGLE_MESSAGE,
                Pair("{toggle}", "autoplay"),
                Pair(
                    "{status}",
                    localeManager.getMessage(GeneralMessages.OFF_STATUS).uppercase()
                )
            ).build()
        } else {
            autoPlayConfig.status = true
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                GeneralMessages.GENERAL_TOGGLE_MESSAGE,
                Pair("{toggle}", "autoplay"),
                Pair(
                    "{status}",
                    localeManager.getMessage(GeneralMessages.ON_STATUS).uppercase()
                )
            ).build()
        }
    }

    override val help: String
        get() = "Autoplay allows Robertify to keep playing music just like the last one at the end of the queue." +
                " Running this command will toggle autoplay either on or off."
}