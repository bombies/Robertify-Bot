package main.utils.apis.robertify.imagebuilders;

import main.utils.GeneralUtils;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.InputStream;

public class NowPlayingImageBuilder extends AbstractImageBuilder {
    public NowPlayingImageBuilder() {
        super(ImageType.NOW_PLAYING);
    }

    public NowPlayingImageBuilder setArtistName(String name) {
        addQuery(QueryFields.ARTIST, name);
        return this;
    }

    public NowPlayingImageBuilder setTitle(String title) {
        addQuery(QueryFields.TITLE, title);
        return this;
    }

    public NowPlayingImageBuilder setAlbumImage(String albumImage) {
        addQuery(QueryFields.ALBUM_IMAGE, albumImage);
        return this;
    }

    public NowPlayingImageBuilder setDuration(long duration) {
        addQuery(QueryFields.DURATION, String.valueOf(duration));
        return this;
    }

    public NowPlayingImageBuilder setCurrentTime(long currentTime) {
        addQuery(QueryFields.CURRENT_TIME, String.valueOf(currentTime));
        return this;
    }

    public NowPlayingImageBuilder setUser(String userName, @Nullable String userAvatar) {
        addQuery(QueryFields.REQUESTER, new JSONObject()
                .put(QueryFields.USER_NAME.toString(), userName)
                .put(QueryFields.USER_IMAGE.toString(), userAvatar != null ? userAvatar : "https://i.imgur.com/t0Y0EbT.png")
                .toString()
        );
        return this;
    }

    public NowPlayingImageBuilder isLiveStream(boolean val) {
        addQuery(QueryFields.LIVESTREAM, String.valueOf(val));
        return this;
    }

    @Override
    public InputStream build() throws ImageBuilderException {
        final var artist = findQuery(QueryFields.ARTIST);
        final var title = findQuery(QueryFields.TITLE);
        final var requester = findQuery(QueryFields.REQUESTER) != null ?
                new JSONObject(findQuery(QueryFields.REQUESTER)).getString(QueryFields.USER_IMAGE.toString())
                : null;

        if (GeneralUtils.textIsRightToLeft(artist)
                || GeneralUtils.textIsRightToLeft(title)
                || GeneralUtils.textIsRightToLeft(requester)
        )
            throw new ImageBuilderException("Some text has right to left characters which aren't supported!");

        return super.build();
    }

    private enum QueryFields implements ImageQueryField {
        ARTIST,
        TITLE,
        ALBUM_IMAGE,
        DURATION,
        REQUESTER,
        USER_NAME,
        USER_IMAGE,
        CURRENT_TIME,
        LIVESTREAM;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }
}
