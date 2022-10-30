package main.utils.spotify;

import lombok.SneakyThrows;
import main.main.Robertify;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Image;

public class SpotifyUtils {
    private static final SpotifyApi api = Robertify.getSpotifyApi();

    @SneakyThrows
    public static String getArtworkUrl(String id) {
        final var images = api.getTrack(id).build().execute().getAlbum().getImages();
        return findBestImage(images).getUrl();
    }

    private static Image findBestImage(Image[] images) {
        Image bestImage = images[0];
        for (final var image : images)
            if (image.getHeight() > bestImage.getHeight())
                bestImage = image;
        return bestImage;
    }
}
