package main.utils.apis.robertify.imagebuilders;

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

    private enum QueryFields implements ImageQueryField {
        ARTIST,
        TITLE,
        ALBUM_IMAGE,
        DURATION,
        CURRENT_TIME;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }
}
