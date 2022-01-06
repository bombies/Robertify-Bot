package main.audiohandlers.sources.deezer;

import api.deezer.DeezerApi;
import api.deezer.http.impl.PaginationRequest;
import api.deezer.objects.Album;
import api.deezer.objects.Playlist;
import api.deezer.objects.Track;
import api.deezer.objects.data.TrackData;
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

import java.io.DataInput;
import java.io.DataOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeezerAudioSourceManager implements AudioSourceManager, HttpConfigurable {
    private static final Logger logger = LoggerFactory.getLogger(DeezerAudioSourceManager.class);

    private static final Pattern DEEZER_TRACK_REGEX = Pattern.compile("^(http|https)://(www\\.)?deezer\\.com/[a-z]{2,3}/track/([0-9]+)(?:.*)$");
    private static final Pattern DEEZER_ARTIST_REGEX = Pattern.compile("^(http|https)://(www\\.)?deezer\\.com/[a-z]{2,3}/artist/([0-9]+)(?:.*)$");
    private static final Pattern DEEZER_ALBUM_REGEX = Pattern.compile("^(http|https)://(www\\.)?deezer\\.com/[a-z]{2,3}/album/([0-9]+)(?:.*)$");
    private static final Pattern DEEZER_PLAYLIST_REGEX = Pattern.compile("^(http|https)://(www\\.)?deezer\\.com/[a-z]{2,3}/playlist/([0-9]+)(?:.*)$");

    private final DeezerApi api = Robertify.getDeezerApi();
    private final YoutubeAudioSourceManager youtubeManager;
    private final SoundCloudAudioSourceManager soundCloudManager;
    private final List<Function<AudioReference, AudioItem>> loaders;

    public DeezerAudioSourceManager(YoutubeAudioSourceManager youtubeManager, SoundCloudAudioSourceManager soundCloudManager) {
        this.youtubeManager = youtubeManager;
        this.soundCloudManager = soundCloudManager;
        this.loaders = Arrays.asList(this::getDeezerAlbum, this::getDeezerArtist, this::getDeezerPlaylist, this::getDeezerTrack);
    }

    @Override
    public String getSourceName() {
        return "deezer";
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

    private AudioItem getDeezerAlbum(AudioReference reference) {
        Matcher res = DEEZER_ALBUM_REGEX.matcher(reference.identifier);

        if (!res.matches())
            return null;

        try {
            List<AudioTrack> album = new ArrayList<>();
            Album retrievedAlbum = api.album().getById(Integer.parseInt(res.group(res.groupCount()))).execute();

            for (var track : retrievedAlbum.getTracks().getData()) {
                if (track == null) continue;

                AudioTrackInfo info = new AudioTrackInfo(
                        track.getTitle(),
                        track.getArtist().getName(),
                        TimeUnit.SECONDS.toMillis(track.getDuration()),
                        getIdentifier(track.getTitle(), track.getArtist().getName()),
                        false, null
                );
                var constructedTrack = new DeezerAudioTrack(info, youtubeManager, soundCloudManager, track.getId(), retrievedAlbum.getCoverXl());
                album.add(constructedTrack);
            }

            return new BasicAudioPlaylist(retrievedAlbum.getTitle(), album, album.get(0), false);
        } catch (Exception e) {
            logger.error("oops!", e);
            throw new FriendlyException(e.getMessage(), FriendlyException.Severity.FAULT, e);
        }
    }

    private AudioItem getDeezerArtist(AudioReference reference) {
        Matcher res = DEEZER_ARTIST_REGEX.matcher(reference.identifier);

        if (!res.matches())
            return null;

        try {
            List<AudioTrack> topTracks = new ArrayList<>();

            TrackData trackData = api.artist().getArtistTopFiveTracks(Integer.parseInt(res.group(res.groupCount()))).execute();

            for (var track : trackData.getData()) {
                if (track == null) continue;

                AudioTrackInfo info = new AudioTrackInfo(
                        track.getTitle(),
                        track.getArtist().getName(),
                        TimeUnit.SECONDS.toMillis(track.getDuration()),
                        getIdentifier(track.getTitle(), track.getArtist().getName()),
                        false, null
                );

                var constructedTrack = new DeezerAudioTrack(info, youtubeManager, soundCloudManager, track.getId(), track.getAlbum() != null ? track.getAlbum().getCoverXl() : BotConstants.DEFAULT_IMAGE.toString());
                topTracks.add(constructedTrack);
            }

            return new BasicAudioPlaylist(topTracks.get(0).getInfo().author, topTracks, topTracks.get(0), false);
        } catch (Exception e) {
            logger.error("oops!", e);
            throw new FriendlyException(e.getMessage(), FriendlyException.Severity.FAULT, e);
        }
    }

    private AudioItem getDeezerPlaylist(AudioReference reference) {
        Matcher res = DEEZER_PLAYLIST_REGEX.matcher(reference.identifier);

        if (!res.matches())
            return null;

        long playlistID = Long.parseLong(res.group(res.groupCount()));

        try {
            final List<AudioTrack> finalPlaylist = new ArrayList<>();
            Playlist retrievedPlaylist = api.playlist().getById(playlistID).execute();
            final PaginationRequest<TrackData> playlistTackRequest = api.playlist().getTracks(playlistID);

            int offset = 0;
            do {
                List<Track> tracks = playlistTackRequest.offset(offset == 0 ? 25 : offset).execute().getData();

                for (var track : tracks) {
                    if (track == null) continue;

                    AudioTrackInfo info = new AudioTrackInfo(
                            track.getTitle(),
                            track.getArtist().getName(),
                            TimeUnit.SECONDS.toMillis(track.getDuration()),
                            getIdentifier(track.getTitle(), track.getArtist().getName()),
                            false, null
                    );

                    var constructedTrack = new DeezerAudioTrack(info ,youtubeManager, soundCloudManager, track.getId(), track.getAlbum() != null ? track.getAlbum().getCoverXl() : BotConstants.DEFAULT_IMAGE.toString());
                    finalPlaylist.add(constructedTrack);
                }

                offset += 25;
            } while (offset < retrievedPlaylist.getNbTracks());

            if (finalPlaylist.isEmpty()) return null;

            return new BasicAudioPlaylist(retrievedPlaylist.getTitle(), finalPlaylist, finalPlaylist.get(0), false);
        } catch (Exception e) {
            logger.error("oops!", e);
            throw new FriendlyException(e.getMessage(), FriendlyException.Severity.FAULT, e);
        }
    }

    private AudioItem getDeezerTrack(AudioReference reference) {
        Matcher res = DEEZER_TRACK_REGEX.matcher(reference.identifier);

        if (!res.matches())
            return null;

        try {
            Track track = api.track().getById(Integer.parseInt(res.group(res.groupCount()))).execute();
            AudioTrackInfo info = new AudioTrackInfo(
                    track.getTitle(),
                    track.getArtist().getName(),
                    TimeUnit.SECONDS.toMillis(track.getDuration()),
                    getIdentifier(track.getTitle(), track.getArtist().getName()),
                    false, null
            );

            return new DeezerAudioTrack(info, youtubeManager, soundCloudManager, track.getId(), track.getAlbum() != null ? track.getAlbum().getCoverXl() : BotConstants.DEFAULT_IMAGE.toString());
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
        // Nothing to encode
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) {
        return new DeezerAudioTrack(trackInfo, youtubeManager, soundCloudManager, null, null);
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
