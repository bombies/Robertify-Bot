package main.utils.spotify;

import lombok.SneakyThrows;
import main.main.Robertify;
import se.michaelthelin.spotify.SpotifyApi;

public class SpotifyUtils {
    private static final SpotifyApi api = Robertify.getSpotifyApi();

    @SneakyThrows
    public static String getArtworkUrl(String id) {
        return api.getTrack(id).build().execute().getAlbum().getImages()[0].getUrl();
    }
}
