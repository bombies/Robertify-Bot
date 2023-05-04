package main.commands.slashcommands.audio

import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager
import dev.minn.jda.ktx.interactions.components.link
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import main.audiohandlers.RobertifyAudioManagerKt
import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.main.ConfigKt
import main.main.RobertifyKt
import main.utils.GeneralUtilsKt.getDestination
import main.utils.GeneralUtilsKt.isUrl
import main.utils.GeneralUtilsKt.toUrl
import main.utils.RobertifyEmbedUtilsKt.Companion.sendWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.component.interactions.slashcommand.models.SubCommandKt
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
                        description = "The name/url of the track/album/playlist to play",
                        autoComplete = true
                    )
                )
            ),
            SubCommandKt(
                name = "next",
                description = "Add songs to the beginning of the queue! Links are accepted by Spotify, Deezer, SoundCloud, etc...",
                options = listOf(
                    CommandOptionKt(
                        name = "tracks",
                        description = "The name/url of the track/album/playlist to add to the top of your queue",
                        autoComplete = true
                    )
                )
            ),
            SubCommandKt(
                name = "shuffled",
                description = "Play a playlist/album shuffled right off the bat!",
                options = listOf(
                    CommandOptionKt(
                        name = "tracks",
                        description = "The playlist/album to play"
                    )
                )
            ),
            SubCommandKt(
                name = "file",
                description = "Play a local file.",
                options = listOf(
                    CommandOptionKt(
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

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()
        val guild = event.guild!!
        val member = event.member
        val memberVoiceState = member?.voiceState!!
        val selfVoiceState = guild.selfMember.voiceState!!

        if (!memberVoiceState.inAudioChannel()) {
            event.hook.sendWithEmbed(guild) {
                embed(GeneralMessages.USER_VOICE_CHANNEL_NEEDED)
            }
                .queue()
            return
        }

        if (selfVoiceState.inAudioChannel() && memberVoiceState.channel!! != selfVoiceState.channel) {
            event.hook.sendWithEmbed(guild) {
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
                    event.hook.sendWithEmbed(guild) {
                        embed(ShufflePlayMessages.INVALID_LINK)
                    }
                } else link = URL(link).getDestination()

                when {
                    link.contains("spotify") && !link.matches("(https?://)?(www\\.)?open\\.spotify\\.com/(user/[a-zA-Z0-9-_]+/)?(?<type>album|playlist|artist)/(?<identifier>[a-zA-Z0-9-_]+)(\\?si=[a-zA-Z0-9]+)?".toRegex()) -> {
                        event.hook.sendWithEmbed(guild) {
                            embed(
                                ShufflePlayMessages.NOT_PLAYLIST,
                                Pair("{source}", "Spotify")
                            )
                        }.queue()
                        return
                    }

                    link.contains("music.apple.com") && !link.matches("(https?://)?(www\\.)?music\\.apple\\.com/(?<countrycode>[a-zA-Z]{2}/)?(?<type>album|playlist|artist)(/[a-zA-Z\\d\\-]+)?/(?<identifier>[a-zA-Z\\d\\-.]+)(\\?i=(?<identifier2>\\d+))?".toRegex()) -> {
                        event.hook.sendWithEmbed(guild) {
                            embed(
                                ShufflePlayMessages.NOT_PLAYLIST,
                                Pair("{source}", "Apple Music")
                            )
                        }.queue()
                        return
                    }

                    link.contains("deezer.com") && !link.matches("(https?://)?(www\\.)?deezer\\.com/(?<countrycode>[a-zA-Z]{2}/)?(?<type>album|playlist|artist)/(?<identifier>[0-9]+)".toRegex()) -> {
                        event.hook.sendWithEmbed(guild) {
                            embed(
                                ShufflePlayMessages.NOT_PLAYLIST,
                                Pair("{source}", "Deezer")
                            )
                        }.queue()
                        return
                    }

                    link.contains("soundcloud.com") && !link.contains("sets") -> {
                        event.hook.sendWithEmbed(guild) {
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

        if (link.isUrl() && !ConfigKt.YOUTUBE_ENABLED) {
            val linkDestination = link.toUrl()!!.getDestination()
            if (linkDestination.contains("youtube.com") || linkDestination.contains("youtu.be")) {
                event.hook.sendWithEmbed(guild) {
                    embed(GeneralMessages.NO_YOUTUBE_SUPPORT)
                }
                    .queue()
                return
            }
        }

        event.hook.sendWithEmbed(guild) {
            embed(FavouriteTracksMessages.FT_ADDING_TO_QUEUE_2)
        }.queue { msg ->
            runBlocking {
                launch {
                    RobertifyAudioManagerKt
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
    }

    private fun handleLocalTrack(event: SlashCommandInteractionEvent, file: Attachment) {
        val guild = event.guild!!
        val channel = event.channel.asGuildMessageChannel()
        val member = event.member!!

        when (file.fileExtension?.lowercase()) {
            "mp3", "ogg", "m4a", "wav", "flac", "webm", "mp4", "aac", "mov" -> {
                if (!Files.exists(Path(ConfigKt.AUDIO_DIR))) {
                    try {
                        Files.createDirectory(Paths.get(ConfigKt.AUDIO_DIR))
                    } catch (e: Exception) {
                        event.hook.sendWithEmbed(guild) {
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
                    val audioManager = RobertifyAudioManagerKt
                    event.hook.sendWithEmbed(guild) {
                        embed(FavouriteTracksMessages.FT_ADDING_TO_QUEUE_2)
                    }.queue { addingMsg ->
                        file.proxy
                            .downloadToFile(File("${ConfigKt.AUDIO_DIR}/${file.fileName}.${file.fileExtension}"))
                            .whenComplete { file, err ->
                                if (err != null) {
                                    logger.error("Error occurred while downloading a file", err)
                                    return@whenComplete
                                }

                                runBlocking {
                                    audioManager.loadAndPlay(
                                        trackUrl = file.path,
                                        memberVoiceState = memberVoiceState,
                                        messageChannel = channel,
                                        botMessage = addingMsg
                                    )
                                }
                            }
                    }
                } catch (e: IllegalArgumentException) {
                    logger.error("Error when attempting to download track", e)
                    event.hook.sendWithEmbed(guild) {
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

            else -> event.hook.sendWithEmbed(guild) {
                embed(PlayMessages.INVALID_FILE)
            }.queue()
        }
    }

    override suspend fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        if (event.focusedOption.name != "tracks" || (event.subcommandName != "tracks" && event.subcommandName != "next"))
            return
        val query = event.focusedOption.value

        if (query.isBlank() || query.isEmpty())
            return

        // If the query seems to be a url, there's no need to load
        // recommendations.
        if (query.trim().matches("https?://[\\w\\W]*".toRegex()))
            return

        event.replyChoices(RobertifyKt.spotifyApi.search.searchTrack(query, limit = 25)
            .mapNotNull { option ->
                if (option == null) null else Choice(
                    "${option.name} by ${option.artists.first().name} ${if (option.explicit) "[EXPLICIT]" else ""}",
                    "https://open.spotify.com/track/${option.id}"
                )
            })
            .queue()
    }
}