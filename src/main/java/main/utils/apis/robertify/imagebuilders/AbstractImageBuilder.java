package main.utils.apis.robertify.imagebuilders;

import lombok.SneakyThrows;
import me.duncte123.botcommons.web.WebUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public abstract class AbstractImageBuilder {
    final Logger logger = LoggerFactory.getLogger(AbstractImageBuilder.class);

    private final ImageType imageType;
    private final URIBuilder uri;
    private final WebUtils webUtils;

    @SneakyThrows
    AbstractImageBuilder(ImageType imageType) {
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
        final var img_dir = Path.of("./built_images");
        if (!Files.exists(img_dir))
            Files.createDirectory(img_dir);

        final var imageFile = new File(img_dir + "/" + UUID.randomUUID() + ".png");
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
