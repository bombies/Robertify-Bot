package main.commands.slashcommands.commands.audio;

import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import lombok.SneakyThrows;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.audio.PlayCommand;
import main.constants.ENV;
import main.main.Config;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.ftp.FtpClient;
import main.utils.locale.LocaleManager;
import main.utils.locale.LocaleMessage;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class PlaySlashCommand extends AbstractSlashCommand {
    final Logger logger = LoggerFactory.getLogger(PlaySlashCommand.class);

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName(new PlayCommand().getName())
                        .setDescription("Play a song! Links are accepted by Spotify, Deezer, SoundCloud, etc...")
                        .addSubCommands(
                                SubCommand.of(
                                        "tracks",
                                        "Play a song! Links are accepted by Spotify, Deezer, SoundCloud, etc...",
                                        List.of(
                                                CommandOption.of(
                                                        OptionType.STRING,
                                                        "tracks",
                                                        "The name/url of the track/album/playlist to play",
                                                        true
                                                )
                                        )
                                ),
                                SubCommand.of(
                                        "file",
                                        "Play a local file!",
                                        List.of(
                                                CommandOption.of(
                                                        OptionType.ATTACHMENT,
                                                        "track",
                                                        "The local audio file of the track to be played!",
                                                        true
                                                )
                                        )
                                ),
                                SubCommand.of(
                                        "nexttracks",
                                        "Add songs to the beginning of the queue! Links are accepted by Spotify, Deezer, SoundCloud, etc...",
                                        List.of(
                                                CommandOption.of(
                                                        OptionType.STRING,
                                                        "tracks",
                                                        "The name/url of the track/album/playlist to play",
                                                        true
                                                )
                                        )
                                )
                        )
                        .setPossibleDJCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Plays a song";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;
        sendRandomMessage(event);

        event.deferReply().queue();

        final var guild = event.getGuild();

        EmbedBuilder eb;

        final Member member = event.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();

        if (!memberVoiceState.inAudioChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.USER_VOICE_CHANNEL_NEEDED);
            event.getHook().sendMessageEmbeds(eb.build()).setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getGuildChannel())).queue();
            return;
        }

        if (selfVoiceState.inAudioChannel() && !memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()))
                            .build())
                    .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getGuildChannel()))
                    .queue();
            return;
        }

        final var commandPath = event.getFullCommandName().split("\\s");

        switch (commandPath[1]) {
            case "tracks" -> {
                String link = event.getOption("tracks").getAsString();
                if (!GeneralUtils.isUrl(link))
                    link = SpotifySourceManager.SEARCH_PREFIX + link;

                handlePlayTracks(event, guild, member, link, false);
            }
            case "file" -> {
                final var file = event.getOption("track").getAsAttachment();
                event.getHook()
                        .sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DISABLED_COMMAND).build())
                        .queue();
//                handleLocalTrack(event, file);
            }
            case "nexttracks" -> {
                String link = event.getOption("tracks").getAsString();
                if (!GeneralUtils.isUrl(link))
                    link = SpotifySourceManager.SEARCH_PREFIX + link;

                handlePlayTracks(event, guild, member, link, true);
            }
        }
    }

    @SneakyThrows
    private void handlePlayTracks(SlashCommandInteractionEvent event, Guild guild, Member member, String link, boolean addToBeginning) {
        if (GeneralUtils.isUrl(link) && !Config.isYoutubeEnabled()) {
            final var linkDestination = GeneralUtils.getLinkDestination(link);
            if (linkDestination.contains("youtube.com") || linkDestination.contains("youtu.be")) {
                event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NO_YOUTUBE_SUPPORT).build())
                        .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getGuildChannel()))
                        .queue();
                return;
            }
        }

        event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.FavouriteTracksMessages.FT_ADDING_TO_QUEUE_2).build())
                .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel()))
                .queue(msg -> RobertifyAudioManager.getInstance()
                        .loadAndPlay(
                                link,
                                guild.getSelfMember().getVoiceState(),
                                member.getVoiceState(),
                                msg,
                                event,
                                addToBeginning
                        ));
    }

    public void handleLocalTrack(SlashCommandInteractionEvent event, Message.Attachment audioFile) {
        final var guild = event.getGuild();
        final var channel = event.getChannel().asGuildMessageChannel();
        final var member = event.getMember();

        switch (audioFile.getFileExtension().toLowerCase()) {
            case "mp3", "ogg", "m4a", "wav", "flac", "webm", "mp4", "aac", "mov" -> {
                if (!Files.exists(Path.of(Config.get(ENV.AUDIO_DIR) + "/"))) {
                    try {
                        Files.createDirectories(Paths.get(Config.get(ENV.AUDIO_DIR)));
                    } catch (Exception e) {
                        event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PlayMessages.LOCAL_DIR_ERR).build())
                                .setActionRow(Button.of(ButtonStyle.LINK, "https://robertify.me/support", "Support Server"))
                                .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getGuildChannel()))
                                .queue();

                        logger.error("[FATAL ERROR] Could not create audio directory!", e);
                        return;
                    }
                }

                final var selfVoiceState = guild.getSelfMember().getVoiceState();
                final var memberVoiceState = member.getVoiceState();

                try {
                    RobertifyAudioManager.getInstance()
                            .joinAudioChannel(
                                    memberVoiceState.getChannel(),
                                    RobertifyAudioManager.getInstance().getMusicManager(guild)
                            );
                    event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.FavouriteTracksMessages.FT_ADDING_TO_QUEUE_2).build())
                            .queue(addingMsg -> {
                                audioFile.getProxy()
                                        .download()
                                        .whenComplete((file, err) -> {
                                            if (err != null) {
                                                logger.error("Error occurred while downloading a file", err);
                                                return;
                                            }

                                            try {
                                                final var streamText = new String(file.readAllBytes(), StandardCharsets.UTF_8);
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                        });

//                                if (!file.exists()) {
//                                    audioFile.
//                                    audioFile.getProxy().downloadToFile(file)
//                                            .thenAccept(downloadedFile -> {
//                                                RobertifyAudioManager.getInstance()
//                                                        .loadAndPlayLocal(
//                                                                channel,
//                                                                downloadedFile.getAbsolutePath(),
//                                                                selfVoiceState,
//                                                                memberVoiceState,
//                                                                addingMsg,
//                                                                false
//                                                        );
//                                            })
//                                            .exceptionally(e -> {
//                                                logger.error("[FATAL ERROR] Error when attempting to download track", e);
//                                                addingMsg.editMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PlayMessages.FILE_DOWNLOAD_ERR).build())
//                                                        .setActionRow(Button.of(ButtonStyle.LINK, "https://robertify.me/support", "Support Server"))
//                                                        .queue();
//                                                return null;
//                                            });
//                                } else {
//                                    RobertifyAudioManager.getInstance()
//                                            .loadAndPlayLocal(
//                                                    channel,
//                                                    file.getAbsolutePath(),
//                                                    selfVoiceState,
//                                                    memberVoiceState,
//                                                    addingMsg,
//                                                    false
//                                            );
//                                }
                            }, new ErrorHandler().handle(ErrorResponse.MISSING_PERMISSIONS, e -> {
                                event.getHook().sendMessage(LocaleManager.getLocaleManager(event.getGuild()).getMessage(RobertifyLocaleMessage.GeneralMessages.NO_EMBED_PERMS))
                                        .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getGuildChannel()))
                                        .queue();
                            }));
                } catch (IllegalArgumentException e) {
                    logger.error("[FATAL ERROR] Error when attempting to download track", e);
                    event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PlayMessages.FILE_DOWNLOAD_ERR).build())
                            .setActionRow(Button.of(ButtonStyle.LINK, "https://robertify.me/support", "Support Server"))
                            .setEphemeral(true)
                            .queue();
                }
            }
            default ->
                    event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PlayMessages.INVALID_FILE).build())
                            .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getGuildChannel()))
                            .queue();
        }
    }
}
