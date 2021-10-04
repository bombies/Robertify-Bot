package main.utils.spotify;

import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.requests.data.tracks.GetTrackRequest;
import lombok.Getter;
import lombok.SneakyThrows;
import main.main.Robertify;

public class SpotifyTrack {
    @Getter
    private String id;
    @Getter
    private Track track;

    @SneakyThrows
    public SpotifyTrack(String id) {
        this.id = id;
        this.track = getTrackRequest(id).execute();
    }

    public SpotifyTrack() {

    }

    public String getTrackName() {
        return track.getName();
    }

    private static GetTrackRequest getTrackRequest(String id) {
        return Robertify.getSpotifyApi().getTrack(id).build();
    }




}
