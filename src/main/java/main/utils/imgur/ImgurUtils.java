package main.utils.imgur;

import com.github.kskelm.baringo.util.BaringoApiException;
import main.main.Robertify;
import se.michaelthelin.spotify.model_objects.specification.Image;

import java.io.File;
import java.io.IOException;

public class ImgurUtils {
    public static String upload(File img) {
        try {
            return Robertify.baringo.imageService().uploadLocalImage(
                    "image/png",
                    img.getName(),
                    null,
                    "RobertifyImg",
                    "Image uploaded from Robertify Bot"
            ).getLink();
        } catch (BaringoApiException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
