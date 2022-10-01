package main.commands.slashcommands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.audio.PlayCommand;
import main.constants.ENV;
import main.main.Config;
import main.main.Listener;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class PlaySlashCommand extends AbstractSlashCommand {
    final Logger logger = LoggerFactory.getLogger(PlaySlashCommand.class);

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName(new PlayCommand().getName())
                        .setDescription("Play a song! Links are accepted by Spotify, YouTube, SoundCloud, etc...")
                        .addSubCommands(
                                SubCommand.of(
                                        "tracks",
                                        "Play a song! Links are accepted by Spotify, YouTube, SoundCloud, etc...",
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
                                        "Add songs to the beginning of the queue! Links are accepted by Spotify, YouTube, SoundCloud, etc...",
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
        final TextChannel channel = event.getChannel().asTextChannel();

        final Member member = event.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();

        if (!memberVoiceState.inAudioChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.USER_VOICE_CHANNEL_NEEDED);
            event.getHook().sendMessageEmbeds(eb.build()).setEphemeral(true).queue();
            return;
        }

        if (selfVoiceState.inAudioChannel() && !memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()))
                            .build())
                    .queue();
            return;
        }

        switch (event.getSubcommandName()) {
            case "tracks" -> {
                String link = event.getOption("tracks").getAsString();
                if (!GeneralUtils.isUrl(link))
                    link = "ytmsearch:" + link;

                handlePlayTracks(event, guild, member, link, false);
            }
            case "file" -> {
                final var file = event.getOption("track").getAsAttachment();
                handleLocalTrack(event, file);
            }
            case "nexttracks" -> {
                String link = event.getOption("tracks").getAsString();
                if (!GeneralUtils.isUrl(link))
                    link = "ytmsearch:" + link;

                handlePlayTracks(event, guild, member, link, true);
            }
        }
    }

    private void handlePlayTracks(SlashCommandInteractionEvent event, Guild guild, Member member, String link, boolean addToBeginning) {
        if (GeneralUtils.isUrl(link) && !Config.isYoutubeEnabled() && (link.contains("youtube.com") || link.contains("youtu.be"))) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NO_YOUTUBE_SUPPORT).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.FavouriteTracksMessages.FT_ADDING_TO_QUEUE_2).build())
                .setEphemeral(false)
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
        final var channel = event.getChannel().asTextChannel();
        final var member = event.getMember();

        switch (audioFile.getFileExtension().toLowerCase()) {
            case "mp3", "ogg", "m4a", "wav", "flac", "webm", "mp4", "aac", "mov" -> {
                if (!Files.exists(Path.of(Config.get(ENV.AUDIO_DIR) + "/"))) {
                    try {
                        Files.createDirectories(Paths.get(Config.get(ENV.AUDIO_DIR)));
                    } catch (Exception e) {
                        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PlayMessages.LOCAL_DIR_ERR).build())
                                .setActionRow(Button.of(ButtonStyle.LINK, "https://robertify.me/support", "Support Server"))
                                .setEphemeral(true)
                                .queue();

                        logger.error("[FATAL ERROR] Could not create audio directory!", e);
                        return;
                    }
                }

                final var selfVoiceState = guild.getSelfMember().getVoiceState();
                final var memberVoiceState = member.getVoiceState();

                try {
                    if (!Files.exists(Path.of(Config.get(ENV.AUDIO_DIR) + "/" + audioFile.getFileName()))) {
                        final var trackFile = new File(Config.get(ENV.AUDIO_DIR) + "/" + audioFile.getFileName());
                        audioFile.getProxy().downloadToFile(trackFile)
                                .thenAccept(file -> channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.FavouriteTracksMessages.FT_ADDING_TO_QUEUE_2).build()).queue(addingMsg -> {
                                    RobertifyAudioManager.getInstance()
                                            .loadAndPlayLocal(channel, file.getPath(), selfVoiceState, memberVoiceState, addingMsg, false);
                                }))
                                .exceptionally(e -> {
                                    logger.error("[FATAL ERROR] Error when attempting to download track", e);
                                    event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PlayMessages.FILE_DOWNLOAD_ERR).build())
                                            .setActionRow(Button.of(ButtonStyle.LINK, "https://robertify.me/support", "Support Server"))
                                            .queue();
                                    return null;
                                });
                    } else {
                        File localAudioFile = new File(Config.get(ENV.AUDIO_DIR) + "/" + audioFile.getFileName());
                        channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.FavouriteTracksMessages.FT_ADDING_TO_QUEUE_2).build()).queue(addingMsg -> {
                            RobertifyAudioManager.getInstance()
                                    .loadAndPlayLocal(channel, localAudioFile.getPath(), selfVoiceState, memberVoiceState, addingMsg, false);
                        }, new ErrorHandler().handle(ErrorResponse.MISSING_PERMISSIONS, e -> RobertifyAudioManager.getInstance()
                                .loadAndPlayLocal(channel, localAudioFile.getPath(), selfVoiceState, memberVoiceState, null, false)));
                    }
                } catch (IllegalArgumentException e) {
                    logger.error("[FATAL ERROR] Error when attempting to download track", e);
                    event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PlayMessages.FILE_DOWNLOAD_ERR).build())
                            .setActionRow(Button.of(ButtonStyle.LINK, "https://robertify.me/support", "Support Server"))
                            .queue();
                }
            }
            default -> event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PlayMessages.INVALID_FILE).build())
                    .queue();
        }
    }
}
