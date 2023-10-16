package main.commands.slashcommands.audio

import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager
import dev.minn.jda.ktx.interactions.components.link
import kotlinx.coroutines.*
import main.audiohandlers.RobertifyAudioManager
import main.commands.slashcommands.SlashCommandManager.getRequiredOption
import main.constants.InteractionLimits
import main.main.Config
import main.main.Robertify
import main.utils.GeneralUtils.coerceAtMost
import main.utils.GeneralUtils.getDestination
import main.utils.GeneralUtils.isUrl
import main.utils.GeneralUtils.queueCoroutine
import main.utils.GeneralUtils.toUrl
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.SubCommand
import main.utils.locale.messages.FavouriteTracksMessages
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.PlayMessages
import main.utils.locale.messages.ShufflePlayMessages
import net.dv8tion.jda.api.entities.Message.Attachment
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.OptionType
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.Path

class PlayCommand : AbstractSlashCommand(
    SlashCommand(
        name = "play",
        description = "Play a song! Links are accepted by Spotify, Deezer, SoundCloud, etc...",
        subcommands = listOf(
            SubCommand(
                name = "tracks",
                description = "Play a song! Links are accepted by Spotify, Deezer, SoundCloud, etc...",
                options = listOf(
                    CommandOption(
                        name = "tracks",
                        description = "The name/url of the track/album/playlist to play",
                        autoComplete = true
                    )
                )
            ),
            SubCommand(
                name = "next",
                description = "Add songs to the beginning of the queue! Links are accepted by Spotify, Deezer, SoundCloud, etc...",
                options = listOf(
                    CommandOption(
                        name = "tracks",
                        description = "The name/url of the track/album/playlist to add to the top of your queue",
                        autoComplete = true
                    )
                )
            ),
            SubCommand(
                name = "shuffled",
                description = "Play a playlist/album shuffled right off the bat!",
                options = listOf(
                    CommandOption(
                        name = "tracks",
                        description = "The playlist/album to play"
                    )
                )
            ),
            SubCommand(
                name = "file",
                description = "Play a local file.",
                options = listOf(
                    CommandOption(
                        type = OptionType.ATTACHMENT,
                        name = "file",
                        description = "The name/url of the tracks/album/playlist to play"
                    )
                )
            )
        )
    )
) {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
    }

    override fun handle(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()
        val guild = event.guild!!
        val member = event.member
        val memberVoiceState = member?.voiceState!!
        val selfVoiceState = guild.selfMember.voiceState!!

        if (!memberVoiceState.inAudioChannel()) {
            event.hook.sendEmbed(guild) {
                embed(GeneralMessages.USER_VOICE_CHANNEL_NEEDED)
            }
                .queue()
            return
        }

        if (selfVoiceState.inAudioChannel() && memberVoiceState.channel!! != selfVoiceState.channel) {
            event.hook.sendEmbed(guild) {
                embed(
                    GeneralMessages.SAME_VOICE_CHANNEL_LOC,
                    Pair("{channel}", selfVoiceState.channel!!.asMention)
                )
            }
            return
        }

        val commandPath = event.fullCommandName.split("\\s".toRegex())

        when (commandPath[1]) {
            "tracks" -> {
                var link = event.getRequiredOption("tracks").asString
                if (!link.isUrl())
                    link = "${SpotifySourceManager.SEARCH_PREFIX}$link"

                handlePlayTracks(event, link)
            }

            "next" -> {
                var link = event.getRequiredOption("tracks").asString

                if (!link.isUrl())
                    link = "${SpotifySourceManager.SEARCH_PREFIX}$link"

                handlePlayTracks(event, link, true)
            }

            "shuffled" -> {
                var link = event.getRequiredOption("tracks").asString
                if (!link.isUrl()) {
                    event.hook.sendEmbed(guild) {
                        embed(ShufflePlayMessages.INVALID_LINK)
                    }
                } else link = URL(link).getDestination()

                when {
                    link.contains("spotify") && !link.matches("(https?://)?(www\\.)?open\\.spotify\\.com/(user/[a-zA-Z0-9-_]+/)?(?<type>album|playlist|artist)/(?<identifier>[a-zA-Z0-9-_]+)(\\?si=.+)?".toRegex()) -> {
                        event.hook.sendEmbed(guild) {
                            embed(
                                ShufflePlayMessages.NOT_PLAYLIST,
                                Pair("{source}", "Spotify")
                            )
                        }.queue()
                        return
                    }

                    link.contains("music.apple.com") && !link.matches("(https?://)?(www\\.)?music\\.apple\\.com/(?<countrycode>[a-zA-Z]{2}/)?(?<type>album|playlist|artist)(/[a-zA-Z\\d\\-]+)?/(?<identifier>[a-zA-Z\\d\\-.]+)(\\?i=(?<identifier2>\\d+))?".toRegex()) -> {
                        event.hook.sendEmbed(guild) {
                            embed(
                                ShufflePlayMessages.NOT_PLAYLIST,
                                Pair("{source}", "Apple Music")
                            )
                        }.queue()
                        return
                    }

                    link.contains("deezer.com") && !link.matches("(https?://)?(www\\.)?deezer\\.com/(?<countrycode>[a-zA-Z]{2}/)?(?<type>album|playlist|artist)/(?<identifier>[0-9]+)".toRegex()) -> {
                        event.hook.sendEmbed(guild) {
                            embed(
                                ShufflePlayMessages.NOT_PLAYLIST,
                                Pair("{source}", "Deezer")
                            )
                        }.queue()
                        return
                    }

                    link.contains("soundcloud.com") && !link.contains("sets") -> {
                        event.hook.sendEmbed(guild) {
                            embed(
                                ShufflePlayMessages.NOT_PLAYLIST,
                                Pair("{source}", "SoundCloud")
                            )
                        }.queue()
                        return
                    }
                }

                handlePlayTracks(event, link, shuffled = true)
            }

            "file" -> {
                val file = event.getRequiredOption("file").asAttachment
                handleLocalTrack(event, file)
            }
        }
    }

    override val help: String
        get() = "Plays a song"

    private fun handlePlayTracks(
        event: SlashCommandInteractionEvent,
        link: String,
        addToBeginning: Boolean = false,
        shuffled: Boolean = false
    ) {
        val guild = event.guild
        val member = event.member!!

        if (link.isUrl() && !Config.YOUTUBE_ENABLED) {
            val linkDestination = link.toUrl()!!.getDestination()
            if (linkDestination.contains("youtube.com") || linkDestination.contains("youtu.be")) {
                event.hook.sendEmbed(guild) {
                    embed(GeneralMessages.NO_YOUTUBE_SUPPORT)
                }
                    .queue()
                return
            }
        }

        event.hook.sendEmbed(guild) {
            embed(FavouriteTracksMessages.FT_ADDING_TO_QUEUE_2)
        }.queue { msg ->
            runBlocking {
                RobertifyAudioManager
                    .loadAndPlay(
                        trackUrl = link,
                        memberVoiceState = member.voiceState!!,
                        botMessage = msg,
                        addToBeginning = addToBeginning,
                        shuffled = shuffled
                    )
            }
        }
    }

    private fun handleLocalTrack(event: SlashCommandInteractionEvent, file: Attachment) {
        val guild = event.guild!!
        val member = event.member!!

        when (file.fileExtension?.lowercase()) {
            "mp3", "ogg", "m4a", "wav", "flac", "webm", "mp4", "aac", "mov" -> {
                if (!Files.exists(Path(Config.AUDIO_DIR))) {
                    try {
                        Files.createDirectory(Paths.get(Config.AUDIO_DIR))
                    } catch (e: Exception) {
                        event.hook.sendEmbed(guild) {
                            embed(PlayMessages.LOCAL_DIR_ERR)
                        }
                            .setActionRow(
                                link(
                                    url = "https://robertify.me/support",
                                    label = "Support Server"
                                )
                            )
                            .queue()
                        logger.error("Could not create audio directory!", e)
                        return
                    }
                }

                val memberVoiceState = member.voiceState!!

                try {
                    val audioManager = RobertifyAudioManager
                    event.hook.sendEmbed(guild) {
                        embed(FavouriteTracksMessages.FT_ADDING_TO_QUEUE_2)
                    }.queue { addingMsg ->
                        file.proxy
                            .downloadToFile(File("${Config.AUDIO_DIR}/${file.fileName}.${file.fileExtension}"))
                            .whenComplete { file, err ->
                                if (err != null) {
                                    logger.error("Error occurred while downloading a file", err)
                                    return@whenComplete
                                }

                                runBlocking {
                                    audioManager.loadAndPlay(
                                        trackUrl = file.path,
                                        memberVoiceState = memberVoiceState,
                                        botMessage = addingMsg
                                    )
                                }
                            }
                    }
                } catch (e: IllegalArgumentException) {
                    logger.error("Error when attempting to download track", e)
                    event.hook.sendEmbed(guild) {
                        embed(PlayMessages.FILE_DOWNLOAD_ERR)
                    }.setActionRow(
                        link(
                            url = "https://robertify.me/support",
                            label = "Support Server"
                        )
                    )
                        .queue()
                }
            }

            else -> event.hook.sendEmbed(guild) {
                embed(PlayMessages.INVALID_FILE)
            }.queue()
        }
    }

    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        if (event.focusedOption.name != "tracks" || (event.subcommandName != "tracks" && event.subcommandName != "next"))
            return
        val query = event.focusedOption.value

        if (query.isBlank() || query.isEmpty())
            return event.replyChoices().queue()

        // If the query seems to be a url, there's no need to load
        // recommendations.
        if (query.trim().matches("https?://[\\w\\W]*".toRegex()))
            return event.replyChoices().queue()

        val choices = runBlocking {
            Robertify.spotifyApi.search.searchTrack(query, limit = 25)
                .mapNotNull { option ->
                    if (option == null) null else Choice(
                        "${option.name} by ${option.artists.first().name} ${if (option.explicit) "[EXPLICIT]" else ""}".coerceAtMost(
                            InteractionLimits.COMMAND_OPTION_CHOICE_LENGTH
                        ),
                        "https://open.spotify.com/track/${option.id}"
                    )
                }
        }

        event.replyChoices(choices).queue()
    }
}