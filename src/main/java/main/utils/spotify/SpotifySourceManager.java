package main.utils.spotify;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.*;
import lombok.Data;
import lombok.NonNull;
import main.audiohandlers.AudioTrackFactory;
import main.audiohandlers.TrackMeta;
import main.main.Robertify;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.data.playlists.GetPlaylistRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetPlaylistsItemsRequest;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SpotifySourceManager implements AudioSourceManager {
    private static final String SPOTIFY_DOMAIN = "open.spotify.com";
    private static final int EXPECTED_PATH_COMPONENTS = 4;

    private final SpotifyApi api = Robertify.getSpotifyApi();
    private final AudioTrackFactory audioTrackFactory;

    @Inject
    public SpotifySourceManager(AudioTrackFactory audioTrackFactory) {
        this.audioTrackFactory = Preconditions.checkNotNull(audioTrackFactory, "Track factory must not be null!");
    }

    @Override
    public String getSourceName() {
        return "spotify";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager audioPlayerManager, AudioReference audioReference) {
        try {
            URL url = new URL(audioReference.identifier);

            if (!StringUtils.equals(url.getHost(), SPOTIFY_DOMAIN)) {
                return null;
            }

            AudioItem audioItem = null;
            audioItem = handleAsPlaylist(url);

            if (audioItem == null) {
                audioItem = handleAsTrack(url);
            }

            return audioItem;

        } catch (MalformedURLException e) {
            return null;
        }
    }

    private AudioTrack handleAsTrack(URL url) {
        Path path = Paths.get(url.getPath());

        if (path.getNameCount() < 2) {
            return null;
        }

        if (!StringUtils.equals(path.getName(0).toString(), "track")) {
            return null;
        }

        String trackId = path.getName(1).toString();

        Track track;
        try {
            track = api.getTrack(trackId).build().execute();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to fetch track from Spotify API.", e);
        }

        TrackMeta songMetadata = getSongMetadata(track);

        return audioTrackFactory.getAudioTrack(songMetadata);
    }

    private BasicAudioPlaylist handleAsPlaylist(URL url) {
        PlaylistKey playlistKey;
        try {
            playlistKey = extractPlaylistId(url);
        } catch (IllegalArgumentException e) {
            return null;
        }

        GetPlaylistRequest playlistRequest = api.getPlaylist(playlistKey.getPlaylistId()).build();

        Playlist playlist;
        try {
            playlist = playlistRequest.execute();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to fetch playlist from Spotify API.", e);
        }

        List<PlaylistTrack> playlistTracks = getAllPlaylistTracks(playlist);

        List<TrackMeta> songMetadata = getSongMetadata(playlistTracks);
        List<AudioTrack> audioTracks = audioTrackFactory.getAudioTrack(songMetadata);

        return new BasicAudioPlaylist(playlist.getName(), audioTracks, null, false);
    }

    private List<PlaylistTrack> getAllPlaylistTracks(Playlist playlist) {
        List<PlaylistTrack> playlistTracks = new ArrayList<>();

        Paging<PlaylistTrack> currentPage = playlist.getTracks();

        do {
            playlistTracks.addAll(List.of(currentPage.getItems()));

            if (currentPage.getNext() == null) {
                currentPage = null;
            } else {

                try {
                    URI nextPageUri = new URI(currentPage.getNext());
//                    List<NameValuePair> queryPairs = URLEncodedUtils.parse(nextPageUri, StandardCharsets.UTF_8);

                    GetPlaylistsItemsRequest.Builder b = api.getPlaylistsItems(playlist.getId());
//                    for (NameValuePair queryPair : queryPairs) {
//                        b = b.parameter(queryPair.getName(), queryPair.getValue());
//                    }
                    currentPage = b.build().execute();
                } catch (Exception e) {
                    throw new IllegalStateException("Unable to query Spotify for playlist tracks.", e);
                }
            }
        } while (currentPage != null);

        return playlistTracks;
    }

    private PlaylistKey extractPlaylistId(URL url) {
        Path path = Paths.get(url.getPath());
        if (path.getNameCount() < EXPECTED_PATH_COMPONENTS) {
            throw new IllegalArgumentException("Not enough path components.");
        }

        if (!StringUtils.equals(path.getName(2).toString(), "playlist")) {
            throw new IllegalArgumentException("URL doesn't appear to be a playlist.");
        }

        String userId = path.getName(1).toString();
        if (StringUtils.isBlank(userId)) {
            throw new IllegalArgumentException("User ID is blank.");
        }

        String playlistId = path.getName(3).toString();
        if (StringUtils.isBlank(playlistId)) {
            throw new IllegalArgumentException("Playlist ID is blank.");
        }

        return new PlaylistKey(userId, playlistId);
    }

    private List<TrackMeta> getSongMetadata(List<PlaylistTrack> playlistTracks) {

        List<TrackMeta> songMetadata = playlistTracks.stream().map(PlaylistTrack::getTrack)
                .map(track -> getSongMetadata((Track) track)).collect(Collectors.toList());

        return songMetadata;
    }

    private TrackMeta getSongMetadata(Track track) {
        String firstArtistName = track.getArtists().length == 0 ? "" : track.getArtists()[0].getName();

        return new TrackMeta(track.getName(), firstArtistName, track.getDurationMs());
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return false;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
        throw new UnsupportedOperationException("encodeTrack is unsupported.");
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        throw new UnsupportedOperationException("decodeTrack is unsupported.");
    }

    @Override
    public void shutdown() {

    }

    @Data
    private static class PlaylistKey {
        @NonNull
        private final String userId;

        @NonNull
        private final String playlistId;
    }
}
