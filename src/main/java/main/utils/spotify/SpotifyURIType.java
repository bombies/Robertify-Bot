package main.utils.spotify;

import lombok.Getter;

import java.util.regex.Pattern;

public enum SpotifyURIType {
    TRACK(Pattern.compile("spotify:track:[a-zA-Z0-9]{22}")),
    ALBUM(Pattern.compile("spotify:album:[a-zA-Z0-9]{22}")),
    PLAYLIST(Pattern.compile("spotify:playlist:[a-zA-Z0-9]{22}"));

    @Getter
    private final Pattern pattern;

    SpotifyURIType(Pattern pattern) {
        this.pattern = pattern;
    }
}
