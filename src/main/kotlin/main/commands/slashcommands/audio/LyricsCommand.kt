package main.commands.slashcommands.audio

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.util.SLF4J
import main.audiohandlers.RobertifyAudioManager
import main.audiohandlers.utils.author
import main.audiohandlers.utils.title
import main.utils.RobertifyEmbedUtils.Companion.editEmbed
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.genius.GeniusAPI
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.LyricsMessages
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import java.io.IOException

class LyricsCommand : AbstractSlashCommand(
    SlashCommand(
        name = "lyrics",
        description = "Get the lyrics for the song being played.",
        options = listOf(
            CommandOption(
                name = "song",
                description = "The song to lookup the lyrics for",
                required = false
            )
        ),
        isPremium = true
    )
) {

    companion object {
        private val logger by SLF4J
    }

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val member = event.member!!
        val guild = event.guild!!
        val memberVoiceState = member.voiceState!!
        val selfVoiceState = guild.selfMember.voiceState!!

        val query = when (event.getOption("song")) {
            null -> {
                val acChecks = audioChannelChecks(
                    memberVoiceState,
                    selfVoiceState,
                    songMustBePlaying = true
                )
                if (acChecks != null) {
                    event.replyEmbed { acChecks }
                        .setEphemeral(true)
                        .queue()
                    return
                }

                val playingTrack = RobertifyAudioManager[guild]
                    .player
                    .playingTrack!!
                "${playingTrack.title} by ${playingTrack.author}"
            }

            else -> event.getOption("song")!!.asString
        }

        val lookingMsg = event.replyEmbed {
            embed(LyricsMessages.LYRICS_SEARCHING, Pair("{query}", query))
        }.await()

        val geniusAPI = GeniusAPI()
        val songSearch = try {
            geniusAPI.search(query)
        } catch (e: IOException) {
            logger.error("Unexpected error", e)
            return
        }

        if (songSearch.status == 403) {
            lookingMsg.editEmbed(guild) {
                embed(GeneralMessages.SELF_INSUFFICIENT_PERMS)
            }.queue()
            return
        }

        if (songSearch.status == 404 || songSearch.hits.size == 0) {
            lookingMsg.editEmbed(guild) {
                embed(LyricsMessages.LYRICS_NOTHING_FOUND, Pair("{query}", query))
            }.queue()
            return
        }

        val hit = songSearch.hits.first
        val lyrics = hit.fetchLyrics()

        if (lyrics == null) {
            lookingMsg.editEmbed(guild) {
                embed(LyricsMessages.LYRICS_NOTHING_FOUND, Pair("{query}", query))
            }.queue()
            return
        }

        try {
            lookingMsg.editEmbed(guild) {
                embed(
                    title = LyricsMessages.LYRICS_EMBED_TITLE,
                    description = lyrics,
                    Pair("{title}", hit.title),
                    Pair("{author}", hit.artist.name)
                )
            }.queue()
        } catch (e: IllegalArgumentException) {
            val chars = lyrics.length
            lookingMsg.editEmbed(guild) {
                embed(
                    LyricsMessages.LYRICS_EMBED_TITLE,
                    lyrics.substring(0, 4096),
                    Pair("{title}", hit.title),
                    Pair("{author}", hit.artist.name)
                )
            }.queue()

            var i = 4096
            do {
                event.channel.sendEmbed(guild) {
                    embed(lyrics.substring(i, chars.coerceAtMost(i + 4096)))
                }.queue()

                i += 4096
            } while (i < chars)
        }
    }

    override val help: String
        get() = "Get the lyrics for the song being played or search for the lyrics of a specific song."
}