package main.utils.apis.robertify.imagebuilders;

import lombok.SneakyThrows;
import main.constants.ENV;
import main.main.Config;
import me.duncte123.botcommons.web.WebUtils;
import okhttp3.OkHttpClient;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URL;
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

    protected String findQuery(ImageQueryField key) {
        return uri.getQueryParams()
                .stream()
                .filter(param -> param.getName().equalsIgnoreCase(key.toString()))
                .findFirst()
                .map(NameValuePair::getValue)
                .orElse(null);
    }

    @SneakyThrows
    public InputStream build() throws ImageBuilderException {
        final var url = new URL(uri.build().toString());
        logger.debug("Built image URL:\n{}", url);

        try {
            return httpClient.newCall(webUtils.prepareGet(url.toString()).build())
                    .execute()
                    .body()
                    .byteStream();
        } catch (SocketTimeoutException | ConnectException e) {
            throw new ImageBuilderException(e);
        } catch (IOException e) {
            logger.error("Unexpected error", e);
        }
        return null;
    }

    public static String getRandomFileName() {
        return UUID.randomUUID() + ".png";
    }

    public interface ImageQueryField {}
}
