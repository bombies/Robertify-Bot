package main.audiohandlers.sources.spotify;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lombok.Getter;
import lombok.SneakyThrows;
import main.audiohandlers.sources.RobertifyAudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.model_objects.specification.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SpotifyTrack extends RobertifyAudioTrack {
    private static final Logger logger = LoggerFactory.getLogger(SpotifyTrack.class);

    private final SpotifySourceManager spotifySourceManager;
    @Getter
    private final SpotifyArtist artist;

    public SpotifyTrack(String title, String identifier, String isrc,
                        Image[] images, String uri, ArtistSimplified[] artists,
                        Integer trackDuration, SpotifyArtist artist, SpotifySourceManager spotifySourceManager) {
        this(new AudioTrackInfo(title,
                        (artists.length == 0) ? "unknown" : artists[0].getName(), trackDuration
                        .longValue(), identifier, false, "https://open.spotify.com/track/" + identifier), isrc,

                (images.length == 0) ? null : images[0].getUrl(), artist, spotifySourceManager);
    }

    public SpotifyTrack(AudioTrackInfo trackInfo, String isrc, String artworkURL, SpotifyArtist artist, SpotifySourceManager spotifySourceManager) {
        super(trackInfo, isrc, artworkURL, artist.id, artist.genres, spotifySourceManager);
        this.artist = artist;
        this.spotifySourceManager = spotifySourceManager;
    }

    public SpotifyTrack(AudioTrackInfo trackInfo, String isrc, String artworkURL, String artist, SpotifySourceManager spotifySourceManager) {
        super(trackInfo, isrc, artworkURL, artist, spotifySourceManager);
        this.artist = assembleArtist(artist);
        this.spotifySourceManager = spotifySourceManager;
    }

    @SneakyThrows
    public static SpotifyTrack of(TrackSimplified track, Album album, SpotifySourceManager spotifySourceManager) {
        final var artist = SpotifySourceManager.getApi().getArtist(track.getArtists()[0].getId()).build().execute();
        return new SpotifyTrack(
                track.getName(),
                track.getId(),
                null,
                album.getImages(),
                track.getUri(),
                track.getArtists(),
                track.getDurationMs(),
                new SpotifyArtist(artist.getId(), Arrays.asList(artist.getGenres())),
                spotifySourceManager
        );
    }

    @SneakyThrows
    public static SpotifyTrack of(Track track, SpotifySourceManager spotifySourceManager) {
        final var artist = SpotifySourceManager.getApi().getArtist(track.getArtists()[0].getId()).build().execute();
        return new SpotifyTrack(
                track.getName(),
                track.getId(),
                track.getExternalIds().getExternalIds().getOrDefault("isrc", null),
                track.getAlbum().getImages(), track.getUri(), track.getArtists(),
                track.getDurationMs(),
                new SpotifyArtist(artist.getId(), Arrays.asList(artist.getGenres())),
                spotifySourceManager
        );
    }

    public String getISRC() {
        return this.isrc;
    }

    public String getArtworkURL() {
        return this.artworkURL;
    }

    public AudioSourceManager getSourceManager() {
        return (AudioSourceManager)this.spotifySourceManager;
    }

    protected AudioTrack makeShallowClone() {
        return new SpotifyTrack(getInfo(), this.isrc, this.artworkURL, this.artist, this.spotifySourceManager);
    }

    public String stringifyArtist() {
        return artist.getId()  + ":" + artist.getGenres();
    }

    public static SpotifyArtist assembleArtist(String str) {
        final var split = str.split(":");
        final var genreStr = split[1].replaceAll("[\\[\\]\\s]", "").strip();
        final var genres = Arrays.stream(genreStr.split(","))
                .toList();
        return new SpotifyArtist(split[0], genres);
    }

    public static class SpotifyArtist {
        @Getter
        private final String id;
        @Getter
        private final List<String> genres;

        public SpotifyArtist(String id, List<String> genres) {
            this.id = id;
            this.genres = genres;
        }
    }
}
