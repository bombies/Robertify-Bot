package main.commands.slashcommands.misc

import main.audiohandlers.RobertifyAudioManagerKt
import main.audiohandlers.utils.length
import main.constants.TimeFormatKt
import main.utils.GeneralUtilsKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.database.mongodb.cache.BotDBCacheKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.PlaytimeMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class PlaytimeCommandKt : AbstractSlashCommandKt(
    CommandKt(
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
        val player = RobertifyAudioManagerKt[guild].player

        val time: Long = (if (playtime[guild.idLong] == null) 0
        else playtime[guild.idLong]!!) + (if (player.playingTrack == null) 0
        else player.playingTrack.length)

        return RobertifyEmbedUtilsKt.embedMessage(
            guild,
            PlaytimeMessages.LISTENED_TO,
            Pair("{time}", GeneralUtilsKt.getDurationString(time))
        )
            .setFooter(
                LocaleManagerKt[guild][
                    PlaytimeMessages.LAST_BOOTED,
                    Pair(
                        "{time}",
                        GeneralUtilsKt.formatDate(
                            BotDBCacheKt.instance.lastStartup,
                            TimeFormatKt.E_DD_MMM_YYYY_HH_MM_SS_Z
                        )
                    )
                ]
            )
            .build()
    }
}