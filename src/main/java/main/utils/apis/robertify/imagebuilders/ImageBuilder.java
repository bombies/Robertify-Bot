package main.utils.apis.robertify.imagebuilders;

import lombok.SneakyThrows;
import me.duncte123.botcommons.web.ContentType;
import me.duncte123.botcommons.web.WebUtils;
import okhttp3.Response;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

public abstract class ImageBuilder {
    final Logger logger = LoggerFactory.getLogger(ImageBuilder.class);

    private final ImageType imageType;
    private final URIBuilder uri;
    private final WebUtils webUtils;

    @SneakyThrows
    ImageBuilder(ImageType imageType) {
        this.imageType = imageType;
        this.webUtils = WebUtils.ins;
        this.uri = new URIBuilder("https://dev.robertify.me/api/images")
                .appendPath(imageType.toString());
    }


    protected void addQuery(ImageQueryField key, String value) {
        uri.addParameter(key.toString(), value);
    }

    @SneakyThrows
    public File build() {
        final var imageFile = new File("./built_image.png");
        final var url = new URL(uri.toString());
        try(final var is = url.openStream();
            final var os = new FileOutputStream(imageFile)){
            final var b = new byte[2048];
            int length;

            while ((length = is.read(b)) != -1)
                os.write(b, 0, length);
            return imageFile;
        } catch (IOException e) {
            logger.error("Unexpected error", e);
        }
        return null;
    }

    public interface ImageQueryField {}
}
