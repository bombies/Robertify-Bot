package main.constants;

public enum TrackSource {
    SPOTIFY,
    DEEZER,
    YOUTUBE,
    SOUNDCLOUD;

    public static TrackSource parse(String name) {
        switch (name.toLowerCase()) {
            case "youtube" -> {
                return YOUTUBE;
            }
            case "spotify" -> {
                return SPOTIFY;
            }
            case "deezer" -> {
                return DEEZER;
            }
            case "soundcloud" -> {
                return SOUNDCLOUD;
            }
            default -> throw new NullPointerException("There is no such source");
        }
    }
}
