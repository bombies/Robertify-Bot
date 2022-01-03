package main.audiohandlers.sources.spotify;

import com.neovisionaries.i18n.CountryCode;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.track.*;
import main.constants.BotConstants;
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
    private static final Pattern SPOTIFY_ARTIST_REGEX = Pattern.compile("^(?:spotify:(artist:)|(?:http://|https://)[a-z]+\\.spotify\\.com/artist/([a-zA-z0-9]+))(?:.*)$");
    private static final Pattern SPOTIFY_ALBUM_REGEX = Pattern.compile("^(?:spotify:(track:)|(?:http://|https://)[a-z]+\\.spotify\\.com/album/([a-zA-z0-9]+))(?:.*)$");
    private static final Pattern SPOTIFY_PLAYLIST_REGEX = Pattern.compile("^(?:spotify:(track:)|(?:http://|https://)[a-z]+\\.spotify\\.com/)playlist/([a-zA-z0-9]+)(?:.*)$");
    private static final Pattern SPOTIFY_PLAYLIST_REGEX_USER = Pattern.compile("^(?:spotify:(track:)|(?:http://|https://)[a-z]+\\.spotify\\.com/)user/(.*)/playlist/([a-zA-z0-9]+)(?:.*)$");
    private static final Pattern SPOTIFY_SECOND_PLAYLIST_REGEX = Pattern.compile("^(?:spotify:user:)(?:.*)(?::playlist:)(.*)$");

    private final Logger logger = LoggerFactory.getLogger(SpotifyAudioSourceManager.class);

    private final SpotifyApi api = Robertify.getSpotifyApi();
    private final YoutubeAudioSourceManager youtubeManager;
    private final SoundCloudAudioSourceManager soundCloudManager;
    private final List<Function<AudioReference, AudioItem>> loaders;

    public SpotifyAudioSourceManager(YoutubeAudioSourceManager youtubeManager, SoundCloudAudioSourceManager soundCloudManager) {
        this.youtubeManager = youtubeManager;
        this.soundCloudManager = soundCloudManager;
        this.loaders = Arrays.asList(this::getSpotifyTrack, this::getSpotifyAlbum, this::getSpotifyPlaylist, this::getSpotifyArtist);
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
                if (t == null) continue;

                AudioTrackInfo info = new AudioTrackInfo(t.getName(), album.getArtists()[0].getName(), t.getDurationMs(),
                        "ytsearch:" + t.getName() + " " + t.getArtists()[0].getName(), false, null);
                var track = new SpotifyAudioTrack(info, youtubeManager, soundCloudManager, t.getId(), album.getImages()[0].getUrl());
                playlist.add(track);
            }

            return new BasicAudioPlaylist(album.getName(), playlist, playlist.get(0), false);
        } catch (Exception e) {
            logger.error("oops!", e);
            throw new FriendlyException(e.getMessage(), FriendlyException.Severity.FAULT, e);
        }
    }

    private AudioItem getSpotifyArtist(AudioReference reference) {
        Matcher res = SPOTIFY_ARTIST_REGEX.matcher(reference.identifier);

        if (!res.matches())
            return null;

        try {
            List<AudioTrack> playlist = new ArrayList<>();

            Future<Track[]> artistFuture = api.getArtistsTopTracks(res.group(res.groupCount()), CountryCode.US).build().executeAsync();
            Track[] tracks = artistFuture.get();

            for (Track t : tracks) {
                if (t == null) continue;

                AudioTrackInfo info = new AudioTrackInfo(
                        t.getName(), t.getArtists()[0].getName(), t.getDurationMs(),
                        getIdentifier(t.getName(), t.getArtists()[0].getName()),
                        false, null
                );
                var track = new SpotifyAudioTrack(info, youtubeManager, soundCloudManager, t.getId(), t.getAlbum().getImages().length >= 1 ? t.getAlbum().getImages()[0].getUrl() : BotConstants.DEFAULT_IMAGE.toString());
                playlist.add(track);
            }

            return new BasicAudioPlaylist(tracks[0].getArtists()[0].getName(), playlist, playlist.get(0), false);
        } catch (Exception e) {
            logger.error("oops!", e);
            throw new FriendlyException(e.getMessage(), FriendlyException.Severity.FAULT, e);
        }
    }

    private AudioItem getSpotifyPlaylist(AudioReference reference) {
        Matcher res = getSpotifyPlaylistFromString(reference.identifier);

        if (!res.matches())
            return null;

        String playListId = res.group(res.groupCount());

        try {
            final List<AudioTrack> finalPlaylist = new ArrayList<>();

            final Future<Playlist> playlistFuture = api.getPlaylist(playListId).build().executeAsync();

            final Playlist spotifyPlaylist = playlistFuture.get();
            final var playlistPaging = spotifyPlaylist.getTracks();

            int offset = 0;
            do {
                Paging<PlaylistTrack> playlistTrackPaging = playlistPaging;
                if (offset != 0)
                    playlistTrackPaging = api.getPlaylistsItems(playListId).offset(offset).limit(100)
                        .build().executeAsync().get();

                for (PlaylistTrack playlistTrack : playlistTrackPaging.getItems()) {
                    var plTrack = (Track)playlistTrack.getTrack();

                    if (plTrack == null) continue;

                    AudioTrackInfo info = new AudioTrackInfo(
                            playlistTrack.getTrack().getName(), plTrack.getArtists()[0].getName(), playlistTrack.getTrack().getDurationMs(),
                            getIdentifier(plTrack.getName(), plTrack.getArtists()[0].getName()),
                            false, null
                    );

                    var track = new SpotifyAudioTrack(
                            info,
                            youtubeManager,
                            soundCloudManager,
                            plTrack.getId(),
                            plTrack.getAlbum().getImages().length >= 1 ? plTrack.getAlbum().getImages()[0].getUrl() : BotConstants.DEFAULT_IMAGE.toString()
                    );

                    finalPlaylist.add(track);
                }


                offset += 100;
            } while (offset < playlistPaging.getTotal());

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
        if (!res.matches())
            return null;

        try {
            Future<Track> trackFuture = api.getTrack(res.group(res.groupCount())).build().executeAsync();
            Track track = trackFuture.get();
            AudioTrackInfo info = new AudioTrackInfo(
                    track.getName(), track.getArtists()[0].getName(), track.getDurationMs(),
                    getIdentifier(track.getName(), track.getArtists()[0].getName()),
                    false, null
            );

            return new SpotifyAudioTrack(info, youtubeManager, soundCloudManager, track.getId(), track.getAlbum().getImages()[0].getUrl());
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

    private String getIdentifier(String trackName, String artistName) {
        return "ytsearch:" + trackName + " by " + artistName;
    }
}
