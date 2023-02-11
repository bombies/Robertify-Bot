package main.utils.apis.robertify.imagebuilders;

import java.util.Arrays;
import java.util.List;

public enum ImageType {
    NOW_PLAYING("music", "nowplaying"),
    QUEUE("music", "queue");

    private final String[] segments;

    ImageType(String... segments) {
        this.segments = segments;
    }

    public List<String> getSegments() {
        return Arrays.stream(segments).toList();
    }
}
