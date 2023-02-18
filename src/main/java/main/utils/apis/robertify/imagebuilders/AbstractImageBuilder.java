package main.utils.apis.robertify.imagebuilders;

import lombok.SneakyThrows;
import main.constants.ENV;
import main.main.Config;
import me.duncte123.botcommons.web.WebUtils;
import okhttp3.OkHttpClient;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public abstract class AbstractImageBuilder {
    final Logger logger = LoggerFactory.getLogger(AbstractImageBuilder.class);

    private final URIBuilder uri;
    private final OkHttpClient httpClient;
    private final WebUtils webUtils;

    @SneakyThrows
    AbstractImageBuilder(ImageType imageType) {
        final long DEFAULT_TIMEOUT = 2;

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .build();
        this.webUtils = WebUtils.ins;

        final var segments = new ArrayList<>(List.of("api", "images"));
        segments.addAll(imageType.getSegments());
        this.uri = new URIBuilder(Config.get(ENV.ROBERTIFY_WEB_HOSTNAME))
                .setPathSegments(segments);
    }


    protected void addQuery(ImageQueryField key, String value) {
        uri.addParameter(key.toString(), value);
    }

    @SneakyThrows
    public File build() throws SocketTimeoutException, ConnectException {
        final var img_dir = Path.of("./built_images");
        if (!Files.exists(img_dir))
            Files.createDirectory(img_dir);

        final var imageFile = new File(img_dir + "/" + UUID.randomUUID() + ".png");
        final var url = new URL(uri.build().toString());
        logger.debug("Built image URL:\n{}", url.toString());

        try(final var is = httpClient.newCall(webUtils.prepareGet(url.toString()).build())
                .execute()
                .body()
                .byteStream();
            final var os = new FileOutputStream(imageFile)){
            final var b = new byte[2048];
            int length;
            while ((length = is.read(b)) != -1)
                os.write(b, 0, length);
            return imageFile;
        } catch (SocketTimeoutException | ConnectException e) {
            throw e;
        } catch (IOException e) {
            logger.error("Unexpected error", e);
        }
        return null;
    }

    public interface ImageQueryField {}
}
