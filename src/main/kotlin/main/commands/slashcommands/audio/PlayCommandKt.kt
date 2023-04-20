package main.commands.slashcommands.audio

import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import main.audiohandlers.RobertifyAudioManagerKt
import main.main.ConfigKt
import main.utils.GeneralUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.sendWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.component.interactions.slashcommand.models.SubCommandKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.LoggerFactory

class PlayCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "play",
        description = "Play a song! Links are accepted by Spotify, Deezer, SoundCloud, etc...",
        subcommands = listOf(
            SubCommandKt(
                name = "tracks",
                description = "Play a song! Links are accepted by Spotify, Deezer, SoundCloud, etc...",
                options = listOf(
                    CommandOptionKt(
                        name = "tracks",
                        description = "The name/url of the track/album/playlist to play"
                    )
                )
            )
        )
    )
) {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
    }

    override val help: String
        get() = "Plays a song"

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (!checks(event)) return
        sendRandomMessage(event)

        event.deferReply().queue()
        val guild = event.guild!!
        val member = event.member
        val memberVoiceState = member?.voiceState!!
        val selfVoiceState = guild.selfMember.voiceState!!

        if (!memberVoiceState.inAudioChannel()) {
            event.hook.sendWithEmbed(guild) {
                embed(RobertifyLocaleMessageKt.GeneralMessages.USER_VOICE_CHANNEL_NEEDED)
            }
                .queue()
            return
        }

        if (selfVoiceState.inAudioChannel() && memberVoiceState.channel!! != selfVoiceState.channel) {
            event.hook.sendWithEmbed(guild) {
                embed(
                    RobertifyLocaleMessageKt.GeneralMessages.SAME_VOICE_CHANNEL_LOC,
                    Pair("{channel}", selfVoiceState.channel!!.asMention)
                )
            }
            return
        }

        val commandPath = event.fullCommandName.split("\\s".toRegex())

        when (commandPath[1]) {
            "tracks" -> {
                var link = event.getOption("tracks")?.asString ?: run {
                    logger.error("Somehow the \"tracks\" option for the \"play\" command was null. Did you forget to set the field as required and is the name correctly typed?")
                    event.hook.sendWithEmbed(guild) { embed(RobertifyLocaleMessageKt.GeneralMessages.UNEXPECTED_ERROR) }
                        .queue()
                    return
                }

                if (!GeneralUtilsKt.isUrl(link))
                    link = "${SpotifySourceManager.SEARCH_PREFIX}$link"

                handlePlayTracks(event, link)
            }
        }
    }


    private fun handlePlayTracks(
        event: SlashCommandInteractionEvent,
        link: String,
        addToBeginning: Boolean = false
    ) {
        val guild = event.guild
        val member = event.member!!

        if (GeneralUtilsKt.isUrl(link) && !ConfigKt.youtubeEnabled) {
            val linkDestination = GeneralUtilsKt.getLinkDestination(link)
            if (linkDestination.contains("youtube.com") || linkDestination.contains("youtu.be")) {
                event.hook.sendWithEmbed(guild) {
                    embed(RobertifyLocaleMessageKt.GeneralMessages.NO_YOUTUBE_SUPPORT)
                }
                    .queue()
                return
            }
        }

        event.hook.sendWithEmbed(guild) {
            embed(RobertifyLocaleMessageKt.FavouriteTracksMessages.FT_ADDING_TO_QUEUE_2)
        }.queue { msg ->
            runBlocking {
                launch {
                    RobertifyAudioManagerKt.ins
                        .loadAndPlay(
                            trackUrl = link,
                            memberVoiceState = member.voiceState!!,
                            botMessage = msg,
                            addToBeginning = addToBeginning
                        )
                }
            }
        }
    }
}