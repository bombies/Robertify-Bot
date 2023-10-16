package main.commands.slashcommands.audio

import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.json.autoplay.AutoPlayConfig
import main.utils.locale.LocaleManager
import main.utils.locale.messages.GeneralMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class AutoPlayCommand : AbstractSlashCommand(
    SlashCommand(
        name = "autoplay",
        description = "Play recommended tracks at the end of your queue",
        isPremium = false,
        djOnly = true
    )
) {

    override fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbed { handleAutoPlay(event.guild!!) }
            .queue()
    }

    private fun handleAutoPlay(guild: Guild): MessageEmbed {
        val localeManager = LocaleManager[guild]
        val autoPlayConfig = AutoPlayConfig(guild)

        return if (autoPlayConfig.getStatus()) {
            autoPlayConfig.setStatus(false)
            RobertifyEmbedUtils.embedMessage(
                guild,
                GeneralMessages.GENERAL_TOGGLE_MESSAGE,
                Pair("{toggle}", "autoplay"),
                Pair(
                    "{status}",
                    localeManager.getMessage(GeneralMessages.OFF_STATUS).uppercase()
                )
            ).build()
        } else {
            autoPlayConfig.setStatus(true)
            RobertifyEmbedUtils.embedMessage(
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