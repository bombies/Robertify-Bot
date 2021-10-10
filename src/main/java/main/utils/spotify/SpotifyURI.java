package main.utils.spotify;

import lombok.Getter;
import lombok.SneakyThrows;
import main.exceptions.InvalidSpotifyURIException;

import java.util.regex.Pattern;

public class SpotifyURI {
    private static final Pattern URI_REGEX = Pattern.compile("spotify:(track|album|playlist):[a-zA-Z0-9]{22}");

    @Getter
    private final String id;
    @Getter
    private final SpotifyURIType type;

    public SpotifyURI(String uri) throws InvalidSpotifyURIException {
        var uriBuilder = new StringBuilder(uri);

        if (SpotifyURIType.TRACK.getPattern().matcher(uri).matches()) {
            type = SpotifyURIType.TRACK;
        } else if (SpotifyURIType.ALBUM.getPattern().matcher(uri).matches()) {
            type = SpotifyURIType.ALBUM;
        } else if (SpotifyURIType.PLAYLIST.getPattern().matcher(uri).matches()) {
            type = SpotifyURIType.PLAYLIST;
        } else {
            if (!uri.contains("spotify.com"))
                throw new InvalidSpotifyURIException("Unsupported URI! Supported: spotify:track, spotify:album, spotify:playlist");

            String[] split;
            uriBuilder.delete(0, uriBuilder.length());
            uriBuilder.append("spotify:");

            if (uri.contains("track")) {
                type = SpotifyURIType.TRACK;
                uriBuilder.append("track:");
                split = uri.split("/track/");
            }
            else if (uri.contains("album")) {
                type = SpotifyURIType.ALBUM;
                uriBuilder.append("album:");
                split = uri.split("/album/");
            }
            else if (uri.contains("playlist")) {
                type = SpotifyURIType.PLAYLIST;
                uriBuilder.append("playlist:");
                split = uri.split("/playlist/");
            }
            else throw new InvalidSpotifyURIException("The link provided doesn't specify which link type it belongs to!");

            String id = split[1].replaceAll("\\?[a-zA-Z0-9~!@#$%^&*()\\-_=;:'\"|\\\\,./]*", "");

            if (id.length() != 22)
                throw new InvalidSpotifyURIException("The link provided doesn't have a valid ID");

            uriBuilder.append(id);
        }
        id = parseId(uriBuilder.toString());
    }

    public static SpotifyURI parse(String uri) throws InvalidSpotifyURIException {
        return new SpotifyURI(uri);
    }

    private static String parseId(String s) {
        String[] split = s.split(":");
        return split[split.length - 1];
    }

    @Override
    public String toString() {
        return "spotify:" + type.name().toLowerCase() + ":" + id;
    }
}
