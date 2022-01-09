package main.commands.commands.misc;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lombok.SneakyThrows;
import main.audiohandlers.lavalink.LavaLinkGuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.audiohandlers.sources.RobertifyAudioTrack;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.genius.GeniusAPI;
import main.utils.genius.GeniusSongSearch;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class LyricsCommand implements ICommand {
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
            if (!memberVoiceState.inVoiceChannel()) {
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be in a voice channel to use this command")
                                .build())
                        .queue();
                return;
            }

            if (!selfVoiceState.inVoiceChannel()) {
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "I must be in a voice channel to use this command")
                                .build())
                        .queue();
                return;
            }

            if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as I am to use this command")
                                .build())
                        .queue();
                return;
            }

            final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(ctx.getGuild());
            final var audioPlayer = musicManager.getPlayer();
            final AudioTrack playingTrack = audioPlayer.getPlayingTrack();

            if (playingTrack == null) {
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "There is nothing playing!").build())
                        .queue();
                return;
            }

            if (!(playingTrack instanceof RobertifyAudioTrack)) {
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "This command is only supported by Spotify/Deezer tracks!").build())
                        .queue();
                return;
            }

            AudioTrackInfo trackInfo = playingTrack.getInfo();
            query = trackInfo.title + " by " + trackInfo.author;
        } else {
            query = String.join(" ", args);
        }

        AtomicReference<String> finalQuery = new AtomicReference<>(query);
        msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Now looking for: `"+query+"`").build())
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
                        lookingMsg.editMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "I do not have permission to execute this request!").build())
                                .queue();
                        return;
                    }

                    if (songSearch.getStatus() == 404 || songSearch.getHits().size() == 0) {
                        lookingMsg.editMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Nothing was found for `"+ finalQuery +"`").build())
                                .queue();
                        return;
                    }

                    GeniusSongSearch.Hit hit = songSearch.getHits().get(0);

                    lookingMsg.editMessageEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(guild, hit.getTitle() + " by " + hit.getArtist().getName(),
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
    public String getHelp(String prefix) {
        return """
                Get the lyrics for the song being played

                **__Usages__**
                `lyrics` *(Fetches the lyrics for the song being currently played)*
                `lyrics <songname>` *(Fetches the lyrics for a specific song)*""";
    }

    @Override
    public List<String> getAliases() {
        return List.of("lyr");
    }
}
