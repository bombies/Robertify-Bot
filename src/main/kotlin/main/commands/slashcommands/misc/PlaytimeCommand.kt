package main.commands.slashcommands.misc

import main.audiohandlers.RobertifyAudioManager
import main.audiohandlers.utils.length
import main.constants.TimeFormat
import main.utils.GeneralUtils
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.database.mongodb.cache.BotDBCache
import main.utils.locale.LocaleManager
import main.utils.locale.messages.PlaytimeMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class PlaytimeCommand : AbstractSlashCommand(
    SlashCommand(
        name = "playtime",
        description = "See how long the bot has played music in this guild since its last startup."
    )
) {

    companion object {
        val playtime = mutableMapOf<Long, Long>()
    }

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        event.replyEmbed { handlePlaytime(guild) }.queue()
    }

    private fun handlePlaytime(guild: Guild): MessageEmbed {
        val player = RobertifyAudioManager[guild].player

        val time: Long = (
                    if (playtime[guild.idLong] == null) 0
                    else playtime[guild.idLong]!!
                ) + (
                    if (player.playingTrack == null) 0
                    else player.playingTrack?.length ?: 0
                )

        return RobertifyEmbedUtils.embedMessage(
            guild,
            PlaytimeMessages.LISTENED_TO,
            Pair("{time}", GeneralUtils.getDurationString(time))
        )
            .setFooter(
                LocaleManager[guild][
                    PlaytimeMessages.LAST_BOOTED,
                    Pair(
                        "{time}",
                        GeneralUtils.formatDate(
                            BotDBCache.instance.lastStartup,
                            TimeFormat.E_DD_MMM_YYYY_HH_MM_SS_Z
                        )
                    )
                ]
            )
            .build()
    }
}