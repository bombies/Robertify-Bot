package main.commands.slashcommands.management

import main.audiohandlers.RobertifyAudioManagerKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.json.guildconfig.GuildConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.TwentyFourSevenMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class TwentyFourSevenCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "247",
        description = "Toggle whether the bot is supposed to stay in voice channels 24/7 or not.",
        isPremium = true
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.replyEmbeds(logic(event.guild!!)).queue()
    }

    private fun logic(guild: Guild): MessageEmbed {
        val config = GuildConfigKt(guild)
        val localeManager = LocaleManagerKt[guild]
        val scheduler = RobertifyAudioManagerKt[guild].scheduler

        return if (config.twentyFourSevenMode) {
            config.twentyFourSevenMode = false
            scheduler.scheduleDisconnect()
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                TwentyFourSevenMessages.TWENTY_FOUR_SEVEN_TOGGLED,
                Pair("{status}", localeManager.getMessage(GeneralMessages.OFF_STATUS).uppercase())
            ).build()
        } else {
            config.twentyFourSevenMode = true
            scheduler.removeScheduledDisconnect()
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                TwentyFourSevenMessages.TWENTY_FOUR_SEVEN_TOGGLED,
                Pair("{status}", localeManager.getMessage(GeneralMessages.ON_STATUS).uppercase())
            ).build()
        }
    }

    override val help: String
        get() = "Toggle whether or not the bot stays in a voice channel 24/7"
}