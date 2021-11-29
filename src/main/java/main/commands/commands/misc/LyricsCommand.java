package main.commands.commands.misc;

import com.fasterxml.jackson.databind.JsonNode;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lombok.SneakyThrows;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.audiohandlers.spotify.SpotifyAudioTrack;
import main.commands.CommandContext;
import main.commands.ICommand;
import me.duncte123.botcommons.messaging.EmbedUtils;
import me.duncte123.botcommons.web.WebUtils;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.net.URLEncoder;
import java.util.List;

public class LyricsCommand implements ICommand {
    private final Logger logger = LoggerFactory.getLogger(LyricsCommand.class);

    @Override @SneakyThrows
    public void handle(CommandContext ctx) throws ScriptException {
        final Member member = ctx.getMember();
        final Message msg = ctx.getMessage();
        final GuildVoiceState memberVoiceState = member.getVoiceState();
        final GuildVoiceState selfVoiceState = ctx.getSelfMember().getVoiceState();

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

        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(ctx.getGuild());
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
        final String query = trackInfo.title + " by " + trackInfo.author;
        String encode = URLEncoder.encode(query.replaceAll("\\(\\)", ""), "UTF-8");
        final String url = "https://api.happi.dev/v1/music?q="+encode+"&limit=&apikey=58eb91teNRjudggr0fU0GjSu8OuVJEMiSpnAIhOPkkBr7SKZRsRtOfyU&type=track&lyrics=1";

        WebUtils.ins.getJSONObject(url)
                .async(json -> {
                    final int length = json.get("length").asInt();
                    if (length == 0) {
                        msg.replyEmbeds(EmbedUtils.embedMessage("There was nothing found for: " +
                                "`"+trackInfo.title+" by "+trackInfo.author+"`").build())
                                .queue();
                        return;
                    }

                    final JsonNode result = json.get("result").get(0);
                    final String artistID = result.get("id_artist").asText();
                    final String albumID = result.get("id_album").asText();
                    final String trackID = result.get("id_track").asText();

                    WebUtils.ins.getJSONObject("https://api.happi.dev/v1/music/artists/"+artistID+"/albums/"+albumID+"/tracks/"+trackID+"/lyrics?apikey=58eb91teNRjudggr0fU0GjSu8OuVJEMiSpnAIhOPkkBr7SKZRsRtOfyU")
                            .async(lyricJson -> {
                                final JsonNode resultObj = lyricJson.get("result");
                                final String lyrics = resultObj.get("lyrics").asText();
                                ctx.getMessage().replyEmbeds(EmbedUtils.embedMessage(lyrics)
                                                .setTitle(trackInfo.title + " by " + trackInfo.author)
                                                .build())
                                        .queue();
                            });
                });
    }

    @Override
    public String getName() {
        return "lyrics";
    }

    @Override
    public String getHelp(String guildID) {
        return "Get the lyrics for the song being played";
    }

    @Override
    public List<String> getAliases() {
        return List.of("lyr");
    }
}
