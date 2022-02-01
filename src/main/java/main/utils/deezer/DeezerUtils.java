package main.utils.deezer;

import api.deezer.DeezerApi;
import lombok.SneakyThrows;
import main.main.Robertify;

public class DeezerUtils {
    private final static DeezerApi api = Robertify.getDeezerApi();

    @SneakyThrows
    public static String getArtworkUrl(Integer id) {
        return api.track().getById(id).execute().getAlbum().getCoverXl();
    }

}
