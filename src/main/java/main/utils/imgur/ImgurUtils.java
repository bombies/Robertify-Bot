package main.utils.imgur;

import com.github.kskelm.baringo.util.BaringoApiException;
import main.main.Robertify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class ImgurUtils {
    private static final Logger logger = LoggerFactory.getLogger(ImgurUtils.class);

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
            logger.error("[FATAL ERROR] An error occurred!", e);
        }
        return null;
    }
}
