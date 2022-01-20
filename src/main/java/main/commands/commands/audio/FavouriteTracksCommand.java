package main.commands.commands.audio;

import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import main.audiohandlers.RobertifyAudioManager;
import main.audiohandlers.sources.deezer.DeezerAudioTrack;
import main.audiohandlers.sources.spotify.SpotifyAudioTrack;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.TrackSource;
import main.utils.database.mongodb.cache.FavouriteTracksCache;

import javax.script.ScriptException;
import java.util.List;

public class FavouriteTracksCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {

        final var config = FavouriteTracksCache.getInstance();
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(ctx.getGuild());
        final var audioPlayer = musicManager.getPlayer();
        final var playingTrack = audioPlayer.getPlayingTrack();

        String id = null;
        TrackSource source = null;

        if (playingTrack instanceof SpotifyAudioTrack sTrack) {
            id = sTrack.getId();
            source = TrackSource.SPOTIFY;
        } else if (playingTrack instanceof DeezerAudioTrack dTrack) {
            id = dTrack.getId();
            source = TrackSource.DEEZER;
        } else if (playingTrack instanceof YoutubeAudioTrack yTrack) {
            id = yTrack.getIdentifier();
            source = TrackSource.YOUTUBE;
        } else if (playingTrack instanceof SoundCloudAudioTrack scTrack) {
            id = scTrack.getIdentifier();
            source = TrackSource.SOUNDCLOUD;
        }

        config.addTrack(ctx.getAuthor().getIdLong(), id, source);
    }

    @Override
    public String getName() {
        return "favouritetracks";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

    @Override
    public String getUsages(String prefix) {
        return "**__Usages__**\n" +
                "`" + prefix + "favouritetracks add` *(Add)*";
    }

    @Override
    public List<String> getAliases() {
        return List.of("favs", "fav", "favoritetracks", "favtracks");
    }
}
