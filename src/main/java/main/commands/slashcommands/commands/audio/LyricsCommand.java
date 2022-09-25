package main.commands.slashcommands.commands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lombok.SneakyThrows;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.genius.GeniusAPI;
import main.utils.genius.GeniusSongSearch;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class LyricsCommand extends AbstractSlashCommand implements ICommand {
    private final Logger logger = LoggerFactory.getLogger(LyricsCommand.class);

    @Override @SneakyThrows
    public void handle(CommandContext ctx) throws ScriptException {
        final Member member = ctx.getMember();
        final Message msg = ctx.getMessage();
        final GuildVoiceState memberVoiceState = member.getVoiceState();
        final GuildVoiceState selfVoiceState = ctx.getSelfMember().getVoiceState();
        final var guild = ctx.getGuild();

        final List<String> args = ctx.getArgs();

        String query;

        if (args.isEmpty()) {
            if (!memberVoiceState.inAudioChannel()) {
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.USER_VOICE_CHANNEL_NEEDED)
                                .build())
                        .queue();
                return;
            }

            if (!selfVoiceState.inAudioChannel()) {
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.VOICE_CHANNEL_NEEDED)
                                .build())
                        .queue();
                return;
            }

            if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL)
                                .build())
                        .queue();
                return;
            }

            final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(ctx.getGuild());
            final var audioPlayer = musicManager.getPlayer();
            final AudioTrack playingTrack = audioPlayer.getPlayingTrack();

            if (playingTrack == null) {
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NOTHING_PLAYING).build())
                        .queue();
                return;
            }

            if (!playingTrack.getSourceManager().getSourceName().equals("spotify") && !playingTrack.getSourceManager().getSourceName().equals("deezer")) {
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LyricsMessages.LYRICS_SOURCE_NOT_SUPPORTED).build())
                        .queue();
                return;
            }

            AudioTrackInfo trackInfo = playingTrack.getInfo();
            query = trackInfo.title + " by " + trackInfo.author;
        } else {
            query = String.join(" ", args);
        }

        AtomicReference<String> finalQuery = new AtomicReference<>(query);
        ctx.getChannel().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LyricsMessages.LYRICS_SEARCHING, Pair.of("{query}", query)).build())
                .queue(lookingMsg -> {

                    GeniusAPI geniusAPI = new GeniusAPI();
                    GeniusSongSearch songSearch;
                    try {
                        songSearch = geniusAPI.search(finalQuery.get());
                    } catch (IOException e) {
                        logger.error("[FATAL ERROR] Unexpected error!", e);
                        return;
                    }

                    if (songSearch.getStatus() == 403) {
                        lookingMsg.editMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SELF_INSUFFICIENT_PERMS).build())
                                .queue();
                        return;
                    }

                    if (songSearch.getStatus() == 404 || songSearch.getHits().size() == 0) {
                        lookingMsg.editMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LyricsMessages.LYRICS_NOTHING_FOUND, Pair.of("{query}", finalQuery.get())).build())
                                .queue();
                        return;
                    }

                    final GeniusSongSearch.Hit hit = songSearch.getHits().get(0);
                    final String lyrics = hit.fetchLyrics();

                    try {

                        lookingMsg.editMessageEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(guild, RobertifyLocaleMessage.LyricsMessages.LYRICS_EMBED_TITLE,
                                                lyrics, Pair.of("{title}", hit.getTitle()), Pair.of("{author}", hit.getArtist().getName())
                                        )
                                        .setThumbnail(hit.getImageUrl())
                                        .build())
                                .queue();
                    } catch (IllegalArgumentException e) {
                        final int numOfChars = lyrics.length();

                        lookingMsg.editMessageEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(guild, RobertifyLocaleMessage.LyricsMessages.LYRICS_EMBED_TITLE,
                                            lyrics.substring(0, 4096), Pair.of("{title}", hit.getTitle()), Pair.of("{author}", hit.getArtist().getName())
                                        ).build())
                                .queue();

                        int i = 4096;
                        do {
                            ctx.getChannel().sendMessageEmbeds(
                                    RobertifyEmbedUtils.embedMessage(guild, lyrics.substring(i, Math.min(i + 4096, numOfChars))).build()
                            ).queue();

                            i += 4096;
                        } while (i < numOfChars);

                    }
                });
    }

    @Override
    public String getName() {
        return "lyrics";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+ GeneralUtils.listToString(getAliases()) +"`\n\n" +
                """
                Get the lyrics for the song being played

                **__Usages__**
                `lyrics` *(Fetches the lyrics for the song being currently played)*
                `lyrics <songname>` *(Fetches the lyrics for a specific song)*""";
    }

    @Override
    public List<String> getAliases() {
        return List.of("lyr");
    }

    @Override
    public boolean isPremium() {
        return true;
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("lyrics")
                        .setDescription("Get the lyrics for the song being played!")
                        .addOptions(
                                CommandOption.of(
                                        OptionType.STRING,
                                        "song",
                                        "The song to lookup lyrics for",
                                        false
                                )
                        )
                        .setPossibleDJCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Get the lyrics for the song being played!";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;
        sendRandomMessage(event);

        final Member member = event.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
        final var guild = event.getGuild();
        final String query;

        if (event.getOption("song") == null) {
            if (!memberVoiceState.inAudioChannel()) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.USER_VOICE_CHANNEL_NEEDED)
                                .build())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            if (!selfVoiceState.inAudioChannel()) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.VOICE_CHANNEL_NEEDED)
                                .build())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL)
                                .build())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
            final var audioPlayer = musicManager.getPlayer();
            final AudioTrack playingTrack = audioPlayer.getPlayingTrack();

            if (playingTrack == null) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NOTHING_PLAYING).build())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            if (!playingTrack.getSourceManager().getSourceName().equals("spotify") && !playingTrack.getSourceManager().getSourceName().equals("deezer")) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LyricsMessages.LYRICS_SOURCE_NOT_SUPPORTED).build())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            AudioTrackInfo trackInfo = playingTrack.getInfo();
            query = trackInfo.title + " by " + trackInfo.author;
        } else {
            query = event.getOption("song").getAsString();
        }

        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LyricsMessages.LYRICS_NOW_SEARCHING).build())
                .queue(x -> {
                    AtomicReference<String> finalQuery = new AtomicReference<>(query);
                    event.getChannel().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LyricsMessages.LYRICS_SEARCHING, Pair.of("{query}", query)).build())
                            .queue(lookingMsg -> {
                                GeniusAPI geniusAPI = new GeniusAPI();
                                GeniusSongSearch songSearch;
                                try {
                                    songSearch = geniusAPI.search(finalQuery.get());
                                } catch (IOException e) {
                                    logger.error("[FATAL ERROR] Unexpected error!", e);
                                    return;
                                }

                                if (songSearch.getStatus() == 403) {
                                    lookingMsg.editMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SELF_INSUFFICIENT_PERMS).build())
                                            .queue();
                                    return;
                                }

                                if (songSearch.getStatus() == 404 || songSearch.getHits().size() == 0) {
                                    lookingMsg.editMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LyricsMessages.LYRICS_NOTHING_FOUND, Pair.of("{query}", finalQuery.get())).build())
                                            .queue();
                                    return;
                                }

                                final GeniusSongSearch.Hit hit = songSearch.getHits().get(0);
                                final String lyrics = hit.fetchLyrics();

                                try {

                                    lookingMsg.editMessageEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(guild, RobertifyLocaleMessage.LyricsMessages.LYRICS_EMBED_TITLE,
                                                            lyrics, Pair.of("{title}", hit.getTitle()), Pair.of("{author}", hit.getArtist().getName())
                                                    )
                                                    .setThumbnail(hit.getImageUrl())
                                                    .build())
                                            .queue();
                                } catch (IllegalArgumentException e) {
                                    final int numOfChars = lyrics.length();

                                    lookingMsg.editMessageEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(guild, RobertifyLocaleMessage.LyricsMessages.LYRICS_EMBED_TITLE,
                                                    lyrics.substring(0, 4096), Pair.of("{title}", hit.getTitle()), Pair.of("{author}", hit.getArtist().getName())
                                            ).build())
                                            .queue();

                                    int i = 4096;
                                    do {
                                        event.getChannel().sendMessageEmbeds(
                                                RobertifyEmbedUtils.embedMessage(guild, lyrics.substring(i, Math.min(i + 4096, numOfChars))).build()
                                        ).queue();

                                        i += 4096;
                                    } while (i < numOfChars);

                                }
                            });
                });
    }
}
