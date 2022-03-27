package main.audiohandlers.sources.deezer;

import api.deezer.DeezerApi;
import api.deezer.http.impl.PaginationRequest;
import api.deezer.objects.Album;
import api.deezer.objects.Playlist;
import api.deezer.objects.Track;
import api.deezer.objects.data.TrackData;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import main.audiohandlers.sources.RobertifyAudioSourceManager;
import main.main.Robertify;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeezerSourceManager extends RobertifyAudioSourceManager {
    public static final String SEARCH_PREFIX = "dzsearch:";

    private static final Pattern DEEZER_TRACK_REGEX = Pattern.compile("^(http|https)://(www\\.)?deezer\\.com/[a-z]{2,3}/track/([0-9]+)(?:.*)$");
    private static final Pattern DEEZER_ARTIST_REGEX = Pattern.compile("^(http|https)://(www\\.)?deezer\\.com/[a-z]{2,3}/artist/([0-9]+)(?:.*)$");
    private static final Pattern DEEZER_ALBUM_REGEX = Pattern.compile("^(http|https)://(www\\.)?deezer\\.com/[a-z]{2,3}/album/([0-9]+)(?:.*)$");
    private static final Pattern DEEZER_PLAYLIST_REGEX = Pattern.compile("^(http|https)://(www\\.)?deezer\\.com/[a-z]{2,3}/playlist/([0-9]+)(?:.*)$");

    private static DeezerApi api;

    private final AudioPlayerManager audioPlayerManager;

    private final List<Function<AudioReference, AudioItem>> loaders;

    public static DeezerApi getApi() {
        return api;
    }

    public AudioPlayerManager getAudioPlayerManager() {
        return this.audioPlayerManager;
    }

    public DeezerSourceManager(AudioPlayerManager audioPlayerManager) {
        super(audioPlayerManager);
        this.audioPlayerManager = audioPlayerManager;
        this.loaders = Arrays.asList(this::getDeezerTrack, this::getDeezerAlbum, this::getDeezerPlaylist, this::getDeezerArtist);
        api = Robertify.getDeezerApi();
    }

    public String getSearchPrefix() {
        return "dzsearch:";
    }

    public String getSourceName() {
        return "deezer";
    }

    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        for (Function<AudioReference, AudioItem> loader : this.loaders) {
            AudioItem item;
            if ((item = loader.apply(reference)) != null)
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
            for (Track track : retrievedAlbum.getTracks().getData()) {
                if (track == null)
                    continue;
                AudioTrackInfo info = new AudioTrackInfo(track.getTitle(), track.getArtist().getName(), TimeUnit.SECONDS.toMillis(track.getDuration()), String.valueOf(track.getId()), false, track.getLink());
                DeezerTrack constructedTrack = new DeezerTrack(info, track.getIsrc(), retrievedAlbum.getCoverXl(), this);
                album.add(constructedTrack);
            }
            return new BasicAudioPlaylist(retrievedAlbum.getTitle(), album, album.get(0), false);
        } catch (Exception e) {
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
            for (Track track : trackData.getData()) {
                if (track == null)
                    continue;
                AudioTrackInfo info = new AudioTrackInfo(track.getTitle(), track.getArtist().getName(), TimeUnit.SECONDS.toMillis(track.getDuration()), String.valueOf(track.getId()), false, track.getLink());
                DeezerTrack constructedTrack = new DeezerTrack(info, track.getIsrc(), (track.getAlbum().getCoverXl() == null) ? "https://i.imgur.com/VNQvjve.png" : track.getAlbum().getCoverXl(), this);
                topTracks.add(constructedTrack);
            }
            return new BasicAudioPlaylist((topTracks.get(0).getInfo()).author, topTracks, topTracks.get(0), false);
        } catch (Exception e) {
            throw new FriendlyException(e.getMessage(), FriendlyException.Severity.FAULT, e);
        }
    }

    private AudioItem getDeezerPlaylist(AudioReference reference) {
        Matcher res = DEEZER_PLAYLIST_REGEX.matcher(reference.identifier);
        if (!res.matches())
            return null;
        long playlistID = Long.parseLong(res.group(res.groupCount()));
        try {
            List<AudioTrack> finalPlaylist = new ArrayList<>();
            Playlist retrievedPlaylist = api.playlist().getById(playlistID).execute();
            PaginationRequest<TrackData> playlistTackRequest = api.playlist().getTracks(playlistID);
            int offset = 0;
            do {
                List<Track> tracks = playlistTackRequest.offset((offset == 0) ? 25 : offset).execute().getData();
                for (Track track : tracks) {
                    if (track == null)
                        continue;
                    AudioTrackInfo info = new AudioTrackInfo(track.getTitle(), track.getArtist().getName(), TimeUnit.SECONDS.toMillis(track.getDuration()), String.valueOf(track.getId()), false, track.getLink());
                    DeezerTrack constructedTrack = new DeezerTrack(info, track.getIsrc(), (track.getAlbum().getCoverXl() == null) ? "https://i.imgur.com/VNQvjve.png" : track.getAlbum().getCoverXl(), this);
                    finalPlaylist.add(constructedTrack);
                }
                offset += 25;
            } while (offset < retrievedPlaylist.getNbTracks());
            if (finalPlaylist.isEmpty())
                return null;
            return new BasicAudioPlaylist(retrievedPlaylist.getTitle(), finalPlaylist, finalPlaylist.get(0), false);
        } catch (Exception e) {
            throw new FriendlyException(e.getMessage(), FriendlyException.Severity.FAULT, e);
        }
    }

    private AudioItem getDeezerTrack(AudioReference reference) {
        Matcher res = DEEZER_TRACK_REGEX.matcher(reference.identifier);
        if (!res.matches())
            return null;
        try {
            Track track = api.track().getById(Integer.parseInt(res.group(res.groupCount()))).execute();
            AudioTrackInfo info = new AudioTrackInfo(track.getTitle(), track.getArtist().getName(), TimeUnit.SECONDS.toMillis(track.getDuration()), String.valueOf(track.getId()), false, track.getLink());
            return new DeezerTrack(info, track.getIsrc(), (track.getAlbum().getCoverXl() == null) ? "https://i.imgur.com/VNQvjve.png" : track.getAlbum().getCoverXl(), this);
        } catch (Exception e) {
            throw new FriendlyException(e.getMessage(), FriendlyException.Severity.FAULT, e);
        }
    }

    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }

    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
        DeezerTrack deezerTrack = (DeezerTrack)track;
        DataFormatTools.writeNullableText(output, deezerTrack.getIsrc());
        DataFormatTools.writeNullableText(output, deezerTrack.getArtworkURL());
    }

    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        return new DeezerTrack(trackInfo, DataFormatTools.readNullableText(input), DataFormatTools.readNullableText(input), this);
    }

    public void shutdown() {}
}
