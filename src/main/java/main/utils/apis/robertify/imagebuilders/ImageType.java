package main.utils.apis.robertify.imagebuilders;

public enum ImageType {
    NOW_PLAYING("music/nowplaying"),
    QUEUE("music/queue");

    private final String str;

    ImageType(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
