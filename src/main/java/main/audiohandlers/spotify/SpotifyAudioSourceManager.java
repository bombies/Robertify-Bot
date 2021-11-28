package main.audiohandlers.spotify;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.track.*;
import main.main.Robertify;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpotifyAudioSourceManager implements AudioSourceManager, HttpConfigurable {
    private static final Pattern SPOTIFY_TRACK_REGEX = Pattern.compile("^(?:spotify:(track:)|(?:http://|https://)[a-z]+\\.spotify\\.com/track/([a-zA-z0-9]+))(?:.*)$");
    private static final Pattern SPOTIFY_ALBUM_REGEX = Pattern.compile("^(?:spotify:(track:)|(?:http://|https://)[a-z]+\\.spotify\\.com/album/([a-zA-z0-9]+))(?:.*)$");
    private static final Pattern SPOTIFY_PLAYLIST_REGEX = Pattern.compile("^(?:spotify:(track:)|(?:http://|https://)[a-z]+\\.spotify\\.com/)playlist/([a-zA-z0-9]+)(?:.*)$");
    private static final Pattern SPOTIFY_PLAYLIST_REGEX_USER = Pattern.compile("^(?:spotify:(track:)|(?:http://|https://)[a-z]+\\.spotify\\.com/)user/(.*)/playlist/([a-zA-z0-9]+)(?:.*)$");
    private static final Pattern SPOTIFY_SECOND_PLAYLIST_REGEX = Pattern.compile("^(?:spotify:user:)(?:.*)(?::playlist:)(.*)$");

    private final Logger logger = LoggerFactory.getLogger(SpotifyAudioSourceManager.class);

    private final SpotifyApi api = Robertify.getSpotifyApi();
    private final YoutubeAudioSourceManager youtubeManager;
    private final List<Function<AudioReference, AudioItem>> loaders;

    public SpotifyAudioSourceManager(YoutubeAudioSourceManager youtubeManager) {
        this.youtubeManager = youtubeManager;
        this.loaders = Arrays.asList(this::getSpotifyTrack, this::getSpotifyAlbum, this::getSpotifyPlaylist);
    }

    @Override
    public String getSourceName() {
        return "spotify";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager audioPlayerManager, AudioReference audioReference) {
        AudioItem item;
        for (Function<AudioReference, AudioItem> loader : loaders) {
            if ((item = loader.apply(audioReference)) != null)
                return item;
        }
        return null;
    }

    private AudioItem getSpotifyAlbum(AudioReference reference) {
        Matcher res = SPOTIFY_ALBUM_REGEX.matcher(reference.identifier);

        if (!res.matches())
            return null;

        try {
            List<AudioTrack> playlist = new ArrayList<>();

            Future<Album> albumFuture = api.getAlbum(res.group(res.groupCount())).build().executeAsync();
            Album album = albumFuture.get();

            for (TrackSimplified t : album.getTracks().getItems()) {
                // TODO caching!
                /*AudioItem item = youtubeManager.loadItem(null, new AudioReference("ytsearch:" + album.getArtists()[0].getName() + " " + t.getName(), null));
                if (item instanceof AudioPlaylist)
                    playlist.add(((AudioPlaylist) item).getTracks().get(0));*/
                AudioTrackInfo info = new AudioTrackInfo(t.getName(), album.getArtists()[0].getName(), t.getDurationMs(),
                        "ytsearch:" + album.getArtists()[0].getName() + " " + t.getName(), false, null);
                playlist.add(new SpotifyAudioTrack(info, youtubeManager));
            }

            return new BasicAudioPlaylist(album.getName(), playlist, playlist.get(0), false);
        } catch (Exception e) {
            logger.error("oops!", e);
            throw new FriendlyException(e.getMessage(), FriendlyException.Severity.FAULT, e);
        }
    }

    @SuppressWarnings("deprecation")
    private AudioItem getSpotifyPlaylist(AudioReference reference) {
        Matcher res = getSpotifyPlaylistFromString(reference.identifier);

        if (!res.matches()) {
            return null;
        }

        String playListId = res.group(res.groupCount());
//        String userId = res.group(res.groupCount() - 1);

        try {
            final List<AudioTrack> finalPlaylist = new ArrayList<>();

            final Future<Playlist> playlistFuture;

            playlistFuture = api.getPlaylist(playListId).build().executeAsync();

            final Playlist spotifyPlaylist = playlistFuture.get();

            for (PlaylistTrack playlistTrack : spotifyPlaylist.getTracks().getItems()) {
                Track trueTrack = api.getTrack(playlistTrack.getTrack().getId()).build().execute();
                /*AudioItem item = youtubeManager.loadItem(null, new AudioReference("ytsearch:" + playlistTrack.getTrack().getArtists()[0].getName() + " " + playlistTrack.getTrack().getName(), null));
                if (item instanceof AudioPlaylist)
                    finalPlaylist.add(((AudioPlaylist) item).getTracks().get(0));*/
                AudioTrackInfo info = new AudioTrackInfo(playlistTrack.getTrack().getName(), trueTrack.getArtists()[0].getName(), playlistTrack.getTrack().getDurationMs(),
                        "ytsearch:" + trueTrack.getArtists()[0].getName() + " " + playlistTrack.getTrack().getName(), false, null);
                finalPlaylist.add(new SpotifyAudioTrack(info, youtubeManager));
            }
            if (finalPlaylist.isEmpty())
                return null;

            return new BasicAudioPlaylist(spotifyPlaylist.getName(), finalPlaylist, finalPlaylist.get(0), false);
        } catch (IllegalArgumentException e) {
            logger.error("oops!", e);
            throw new FriendlyException("This playlist could not be loaded, make sure that it's public", FriendlyException.Severity.COMMON, e);
        } catch (Exception e) {
            logger.error("oops!", e);
            throw new FriendlyException(e.getMessage(), FriendlyException.Severity.FAULT, e);
        }
    }

    private Matcher getSpotifyPlaylistFromString(String input) {
        Matcher match = SPOTIFY_PLAYLIST_REGEX.matcher(input);
        if (match.matches())
            return match;

        Matcher withUser = SPOTIFY_PLAYLIST_REGEX_USER.matcher(input);
        if (withUser.matches())
            return withUser;

        return SPOTIFY_SECOND_PLAYLIST_REGEX.matcher(input);
    }

    private AudioItem getSpotifyTrack(AudioReference reference) {
        Matcher res = SPOTIFY_TRACK_REGEX.matcher(reference.identifier);
        if (!res.matches()) {
            return null;
        }

        try {
            Future<Track> trackFuture = api.getTrack(res.group(res.groupCount())).build().executeAsync();
            Track track = trackFuture.get();

            /* AudioItem item = youtubeManager.loadItem(null, new AudioReference("ytsearch:" + track.getArtists()[0].getName() + " " + track.getName(), null));
            if (item instanceof AudioPlaylist)
                return ((AudioPlaylist) item).getTracks().get(0);*/
            AudioTrackInfo info = new AudioTrackInfo(track.getName(), track.getArtists()[0].getName(), track.getDurationMs(),
                    "ytsearch:" + track.getArtists()[0].getName() + " " + track.getName(), false, null);

            return new SpotifyAudioTrack(info, youtubeManager);
        } catch (Exception e) {
            logger.error("oops!", e);
            throw new FriendlyException(e.getMessage(), FriendlyException.Severity.FAULT, e);
        }
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return false;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) {
        throw new UnsupportedOperationException("Not supported by this audio source manager");
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) {
        throw new UnsupportedOperationException("Not supported by this audio source manager");
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
        //
    }

    @Override
    public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
        //
    }
}
