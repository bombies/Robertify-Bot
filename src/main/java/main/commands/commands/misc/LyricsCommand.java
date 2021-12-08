package main.commands.commands.misc;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import core.GLA;
import genius.SongSearch;
import lombok.SneakyThrows;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.audiohandlers.spotify.SpotifyAudioTrack;
import main.commands.CommandContext;
import main.commands.ICommand;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.List;

public class LyricsCommand implements ICommand {
    private final Logger logger = LoggerFactory.getLogger(LyricsCommand.class);

    @Override @SneakyThrows
    public void handle(CommandContext ctx) throws ScriptException {
        final Member member = ctx.getMember();
        final Message msg = ctx.getMessage();
        final GuildVoiceState memberVoiceState = member.getVoiceState();
        final GuildVoiceState selfVoiceState = ctx.getSelfMember().getVoiceState();



        final List<String> args = ctx.getArgs();

        String query;

        if (args.isEmpty()) {
            if (!memberVoiceState.inVoiceChannel()) {
                msg.replyEmbeds(EmbedUtils.embedMessage("You must be in a voice channel to use this command")
                                .build())
                        .queue();
                return;
            }

            if (!selfVoiceState.inVoiceChannel()) {
                msg.replyEmbeds(EmbedUtils.embedMessage("I must be in a voice channel to use this command")
                                .build())
                        .queue();
                return;
            }

            if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
                msg.replyEmbeds(EmbedUtils.embedMessage("You must be in the same voice channel as I am to use this command")
                                .build())
                        .queue();
                return;
            }

            final GuildMusicManager musicManager = RobertifyAudioManager.getInstance().getMusicManager(ctx.getGuild());
            final AudioPlayer audioPlayer = musicManager.audioPlayer;
            final AudioTrack playingTrack = audioPlayer.getPlayingTrack();

            if (playingTrack == null) {
                msg.replyEmbeds(EmbedUtils.embedMessage("There is nothing playing!").build())
                        .queue();
                return;
            }

            if (!(playingTrack instanceof SpotifyAudioTrack)) {
                msg.replyEmbeds(EmbedUtils.embedMessage("This command is only supported by Spotify tracks!").build())
                        .queue();
                return;
            }

            AudioTrackInfo trackInfo = playingTrack.getInfo();
            query = trackInfo.title + " by " + trackInfo.author;
        } else {
            query = String.join(" ", args);
        }

        String finalQuery = query;
        msg.replyEmbeds(EmbedUtils.embedMessage("Now looking for: `"+query+"`").build())
                .queue(lookingMsg -> {
                    GLA gla = new GLA();
                    SongSearch songSearch = null;
                    try {
                        songSearch = gla.search(finalQuery);
                    } catch (IOException e) {
                        logger.error("[FATAL ERROR] Unexpected error!", e);
                    }

                    if (songSearch.getStatus() == 404 || songSearch.getHits().size() == 0) {
                        lookingMsg.editMessageEmbeds(EmbedUtils.embedMessage("Nothing was found for `"+ finalQuery +"`").build())
                                .queue();
                        return;
                    }

                    SongSearch.Hit hit = songSearch.getHits().get(0);

                    lookingMsg.editMessageEmbeds(EmbedUtils.embedMessageWithTitle(hit.getTitle() + " by " + hit.getArtist().getName(),
                                    hit.fetchLyrics())
                                    .setThumbnail(hit.getImageUrl())
                                    .build())
                            .queue();
                });
    }

    @Override
    public String getName() {
        return "lyrics";
    }

    @Override
    public String getHelp(String guildID) {
        return "Get the lyrics for the song being played\n\n" +
                "**__Usages__**\n" +
                "`lyrics` *(Fetches the lyrics for the song being currently played)*\n" +
                "`lyrics <songname> *(Fetches the lyrics for a specific song)*`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("lyr");
    }
}
