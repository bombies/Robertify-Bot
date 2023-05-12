package main.commands.slashcommands.management

import main.audiohandlers.RobertifyAudioManager
import main.utils.RobertifyEmbedUtils
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.json.guildconfig.GuildConfig
import main.utils.locale.LocaleManager
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.TwentyFourSevenMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class TwentyFourSevenCommand : AbstractSlashCommand(
    SlashCommand(
        name = "247",
        description = "Toggle whether the bot is supposed to stay in voice channels 24/7 or not.",
        isPremium = true
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbeds(logic(event.guild!!)).queue()
    }

    private fun logic(guild: Guild): MessageEmbed {
        val config = GuildConfig(guild)
        val localeManager = LocaleManager[guild]
        val scheduler = RobertifyAudioManager[guild].scheduler

        return if (config.twentyFourSevenMode) {
            config.twentyFourSevenMode = false
            scheduler.scheduleDisconnect()
            RobertifyEmbedUtils.embedMessage(
                guild,
                TwentyFourSevenMessages.TWENTY_FOUR_SEVEN_TOGGLED,
                Pair("{status}", localeManager.getMessage(GeneralMessages.OFF_STATUS).uppercase())
            ).build()
        } else {
            config.twentyFourSevenMode = true
            scheduler.removeScheduledDisconnect()
            RobertifyEmbedUtils.embedMessage(
                guild,
                TwentyFourSevenMessages.TWENTY_FOUR_SEVEN_TOGGLED,
                Pair("{status}", localeManager.getMessage(GeneralMessages.ON_STATUS).uppercase())
            ).build()
        }
    }

    override val help: String
        get() = "Toggle whether or not the bot stays in a voice channel 24/7"
}