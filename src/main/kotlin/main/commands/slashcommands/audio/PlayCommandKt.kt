package main.commands.slashcommands.audio

import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager
import dev.minn.jda.ktx.interactions.components.link
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import main.audiohandlers.RobertifyAudioManagerKt
import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.main.ConfigKt
import main.utils.GeneralUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.sendWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.component.interactions.slashcommand.models.SubCommandKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.entities.Message.Attachment
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import org.slf4j.LoggerFactory
import java.io.File
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
                        description = "The name/url of the track/album/playlist to play"
                    )
                )
            ),
            SubCommandKt(
                name = "nexttracks",
                description = "Add songs to the beginning of the queue! Links are accepted by Spotify, Deezer, SoundCloud, etc...",
                options = listOf(
                    CommandOptionKt(
                        name = "tracks",
                        description = "The name/url of the track/album/playlist to add to the top of your queue"
                    )
                )
            ),
            SubCommandKt(
                name = "file",
                description = "Play a local file.",
                options = listOf(
                    CommandOptionKt(
                        type = OptionType.ATTACHMENT,
                        name = "tracks",
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
                var link = event.getRequiredOption("tracks").asString
                if (!GeneralUtilsKt.isUrl(link))
                    link = "${SpotifySourceManager.SEARCH_PREFIX}$link"

                handlePlayTracks(event, link)
            }

            "nexttracks" -> {
                var link = event.getRequiredOption("tracks").asString

                if (!GeneralUtilsKt.isUrl(link))
                    link = "${SpotifySourceManager.SEARCH_PREFIX}$link"

                handlePlayTracks(event, link, true)
            }

            "file" -> {
                val file = event.getRequiredOption("tracks").asAttachment
                handleLocalTrack(event, file)
            }
        }
    }

    override val help: String
        get() = "Plays a song"

    private fun handlePlayTracks(
        event: SlashCommandInteractionEvent,
        link: String,
        addToBeginning: Boolean = false
    ) {
        val guild = event.guild
        val member = event.member!!

        if (GeneralUtilsKt.isUrl(link) && !ConfigKt.YOUTUBE_ENABLED) {
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
                    RobertifyAudioManagerKt
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
                            embed(RobertifyLocaleMessageKt.PlayMessages.LOCAL_DIR_ERR)
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
                    val musicManager = audioManager.getMusicManager(guild)

                    runBlocking {
                        audioManager.joinAudioChannel(memberVoiceState.channel!!, musicManager)
                    }

                    event.hook.sendWithEmbed(guild) {
                        embed(RobertifyLocaleMessageKt.FavouriteTracksMessages.FT_ADDING_TO_QUEUE_2)
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
                        embed(RobertifyLocaleMessageKt.PlayMessages.FILE_DOWNLOAD_ERR)
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
                embed(RobertifyLocaleMessageKt.PlayMessages.INVALID_FILE)
            }.queue()
        }
    }
}