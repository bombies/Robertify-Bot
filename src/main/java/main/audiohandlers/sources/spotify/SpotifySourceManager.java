package main.audiohandlers.sources.spotify;

import com.neovisionaries.i18n.CountryCode;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import main.audiohandlers.sources.RobertifyAudioSourceManager;
import main.main.Robertify;
import main.utils.spotify.SpotifyAuthorizationUtils;
import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.exceptions.detailed.NotFoundException;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;
import se.michaelthelin.spotify.model_objects.specification.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpotifySourceManager extends RobertifyAudioSourceManager {
    public static final String SEARCH_PREFIX = "spsearch:";

    private static final Pattern SPOTIFY_TRACK_REGEX = Pattern.compile("^(?:spotify:(track:)|(?:http://|https://)[a-z]+\\.spotify\\.com/track/([a-zA-z0-9]+))(?:.*)$");
    private static final Pattern SPOTIFY_ARTIST_REGEX = Pattern.compile("^(?:spotify:(artist:)|(?:http://|https://)[a-z]+\\.spotify\\.com/artist/([a-zA-z0-9]+))(?:.*)$");
    private static final Pattern SPOTIFY_ALBUM_REGEX = Pattern.compile("^(?:spotify:(track:)|(?:http://|https://)[a-z]+\\.spotify\\.com/album/([a-zA-z0-9]+))(?:.*)$");
    private static final Pattern SPOTIFY_PLAYLIST_REGEX = Pattern.compile("^(?:spotify:(track:)|(?:http://|https://)[a-z]+\\.spotify\\.com/)playlist/([a-zA-z0-9]+)(?:.*)$");
    private static final Pattern SPOTIFY_PLAYLIST_REGEX_USER = Pattern.compile("^(?:spotify:(track:)|(?:http://|https://)[a-z]+\\.spotify\\.com/)user/(.*)/playlist/([a-zA-z0-9]+)(?:.*)$");
    private static final Pattern SPOTIFY_SECOND_PLAYLIST_REGEX = Pattern.compile("^(?:spotify:user:)(?:.*)(?::playlist:)(.*)$");

    private static final Logger log = LoggerFactory.getLogger(SpotifySourceManager.class);
    private static SpotifyApi api;
    private final AudioPlayerManager audioPlayerManager;
    private final List<Function<AudioReference, AudioItem>> loaders;

    public SpotifySourceManager(AudioPlayerManager audioPlayerManager) {
        super(audioPlayerManager);
        this.audioPlayerManager = audioPlayerManager;
        this.loaders = Arrays.asList(this::getSpotifyTrack, this::getSpotifyAlbum, this::getSpotifyPlaylist, this::getSpotifyArtist);
        api = Robertify.getSpotifyApi();
    }

    public AudioPlayerManager getAudioPlayerManager() {
        return this.audioPlayerManager;
    }

    public String getSourceName() {
        return "spotify";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        try {
            if (reference.identifier.startsWith(SEARCH_PREFIX))
                return this.getSearch(reference.identifier.substring(SEARCH_PREFIX.length()).trim());

            for (Function<AudioReference, AudioItem> loader : this.loaders) {
                AudioItem item;
                if ((item = loader.apply(reference)) != null)
                    return item;
            }
        } catch (IOException | ParseException | SpotifyWebApiException e) {
            if (e instanceof NotFoundException)
                return AudioReference.NO_TRACK;
            if (e instanceof UnauthorizedException) {
                Robertify.getSpotifyTokenRefreshScheduler()
                        .scheduleAtFixedRate(
                                SpotifyAuthorizationUtils.doTokenRefresh(),
                                0, 1,
                                TimeUnit.HOURS
                        );
                loadItem(manager, reference);
            } else throw new RuntimeException(e);
        }
        return AudioReference.NO_TRACK;
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }

    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
        SpotifyTrack spotifyTrack = (SpotifyTrack)track;
        DataFormatTools.writeNullableText(output, spotifyTrack.getISRC());
        DataFormatTools.writeNullableText(output, spotifyTrack.getArtworkURL());
        DataFormatTools.writeNullableText(output, spotifyTrack.stringifyArtist());
    }

    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        return new SpotifyTrack(
                trackInfo,
                DataFormatTools.readNullableText(input),
                DataFormatTools.readNullableText(input),
                DataFormatTools.readNullableText(input),
                this
        );
    }

    @Override
    public void shutdown() {}

    public AudioItem getSearch(String query) throws IOException, ParseException, SpotifyWebApiException {
        Paging<Track> searchResult = api.searchTracks(query).build().execute();
        if (searchResult.getItems().length == 0)
            return AudioReference.NO_TRACK;
        ArrayList<AudioTrack> tracks = new ArrayList<>();
        for (Track item : searchResult.getItems())
            tracks.add(SpotifyTrack.of(item, this));
        return new BasicAudioPlaylist("Search results for: " + query, tracks, null, true);
    }

    private AudioItem getSpotifyAlbum(AudioReference reference) {
        Matcher res = SPOTIFY_ALBUM_REGEX.matcher(reference.identifier);
        if (!res.matches())
            return null;
        try {
            final List<AudioTrack> playlist = new ArrayList<>();
            final Future<Album> albumFuture = api.getAlbum(res.group(res.groupCount())).build().executeAsync();
            final Album album = albumFuture.get();
            for (TrackSimplified t : album.getTracks().getItems()) {
                if (t != null) {
                    final var info = new AudioTrackInfo(t.getName(), album.getArtists()[0].getName(), t.getDurationMs(), t.getId(), false, "https://open.spotify.com/track/" + t.getId());
                    SpotifyTrack track = new SpotifyTrack(
                            info,
                            t.getExternalUrls().getExternalUrls().getOrDefault("isrc", null),
                            album.getImages()[0].getUrl(),
                            new SpotifyTrack.SpotifyArtist(album.getArtists()[0].getId(), List.of(" ")),
                            this
                    );
                    playlist.add(track);
                }
            }
            return new BasicAudioPlaylist(album.getName(), playlist, playlist.get(0), false);
        } catch (Exception e) {
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
                if (t != null) {
                    AudioTrackInfo info = new AudioTrackInfo(t.getName(), t.getArtists()[0].getName(), t.getDurationMs(), t.getId(), false, "https://open.spotify.com/track/" + t.getId());
                    final var artist = t.getArtists()[0];
                    SpotifyTrack track = new SpotifyTrack(
                            info,
                            t.getExternalIds().getExternalIds().getOrDefault("isrc", null),
                            ((t.getAlbum().getImages()).length >= 1) ? t.getAlbum().getImages()[0].getUrl() : "https://i.imgur.com/VNQvjve.png",
                            new SpotifyTrack.SpotifyArtist(artist.getId(), List.of(" ")),
                            this
                    );
                    playlist.add(track);
                }
            }
            return new BasicAudioPlaylist(tracks[0].getArtists()[0].getName(), playlist, playlist.get(0), false);
        } catch (Exception e) {
            throw new FriendlyException(e.getMessage(), FriendlyException.Severity.FAULT, e);
        }
    }

    private AudioItem getSpotifyPlaylist(AudioReference reference) {
        Matcher res = getSpotifyPlaylistFromString(reference.identifier);
        if (!res.matches())
            return null;
        String playListId = res.group(res.groupCount());
        try {
            List<AudioTrack> finalPlaylist = new ArrayList<>();
            Future<Playlist> playlistFuture = api.getPlaylist(playListId).build().executeAsync();
            Playlist spotifyPlaylist = playlistFuture.get();
            Paging<PlaylistTrack> playlistPaging = spotifyPlaylist.getTracks();
            int offset = 0;
            do {
                Paging<PlaylistTrack> playlistTrackPaging = playlistPaging;
                if (offset != 0)
                    playlistTrackPaging = api.getPlaylistsItems(playListId).offset(offset).limit(100).build().executeAsync().get();
                for (PlaylistTrack playlistTrack : playlistTrackPaging.getItems()) {
                    Track plTrack = (Track)playlistTrack.getTrack();
                    if (plTrack != null) {
                        AudioTrackInfo info = new AudioTrackInfo(plTrack.getName(), plTrack.getArtists()[0].getName(), playlistTrack.getTrack().getDurationMs(), plTrack.getId(), false, "https://open.spotify.com/track/" + plTrack.getId());
                        final var artist = plTrack.getArtists()[0];
                        SpotifyTrack track = new SpotifyTrack(
                                info,
                                plTrack.getExternalIds().getExternalIds().getOrDefault("isrc", null),
                                ((plTrack.getAlbum().getImages()).length >= 1) ? plTrack.getAlbum().getImages()[0].getUrl() : "https://i.imgur.com/VNQvjve.png",
                                new SpotifyTrack.SpotifyArtist(artist.getId(), List.of(" ")),
                                this
                        );
                        finalPlaylist.add(track);
                    }
                }
                offset += 100;
            } while (offset < playlistPaging.getTotal());
            if (finalPlaylist.isEmpty())
                return null;
            return new BasicAudioPlaylist(spotifyPlaylist.getName(), finalPlaylist, finalPlaylist.get(0), false);
        } catch (IllegalArgumentException e) {
            throw new FriendlyException("This playlist could not be loaded, make sure that it's public", FriendlyException.Severity.COMMON, e);
        } catch (Exception e) {
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
            AudioTrackInfo info = new AudioTrackInfo(track.getName(), track.getArtists()[0].getName(), track.getDurationMs(), track.getId(), false, "https://open.spotify.com/track/" + track.getId());
            final var artist = track.getArtists()[0];

            return new SpotifyTrack(
                    info,
                    track.getExternalIds().getExternalIds().getOrDefault("isrc", null),
                    track.getAlbum().getImages()[0].getUrl(),
                    new SpotifyTrack.SpotifyArtist(artist.getId(), List.of(" ")),
                    this
            );
        } catch (Exception e) {
            throw new FriendlyException(e.getMessage(), FriendlyException.Severity.FAULT, e);
        }
    }

    public static SpotifyApi getApi() {
        return api;
    }

    public String getSearchPrefix() {
        return "spsearch:";
    }
}
