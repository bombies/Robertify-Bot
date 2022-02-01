package main.constants;

import java.util.regex.Pattern;

public abstract class SourcePlaylistPatterns {
    private static final Pattern SPOTIFY_ARTIST_REGEX = Pattern.compile("^(?:spotify:(artist:)|(?:http://|https://)[a-z]+\\.spotify\\.com/artist/([a-zA-z0-9]+))(?:.*)$");
    private static final Pattern SPOTIFY_ALBUM_REGEX = Pattern.compile("^(?:spotify:(track:)|(?:http://|https://)[a-z]+\\.spotify\\.com/album/([a-zA-z0-9]+))(?:.*)$");
    private static final Pattern SPOTIFY_PLAYLIST_REGEX = Pattern.compile("^(?:spotify:(track:)|(?:http://|https://)[a-z]+\\.spotify\\.com/)playlist/([a-zA-z0-9]+)(?:.*)$");
    private static final Pattern SPOTIFY_PLAYLIST_REGEX_USER = Pattern.compile("^(?:spotify:(track:)|(?:http://|https://)[a-z]+\\.spotify\\.com/)user/(.*)/playlist/([a-zA-z0-9]+)(?:.*)$");
    private static final Pattern DEEZER_ARTIST_REGEX = Pattern.compile("^(http|https)://(www\\.)?deezer\\.com/[a-z]{2,3}/artist/([0-9]+)(?:.*)$");
    private static final Pattern DEEZER_ALBUM_REGEX = Pattern.compile("^(http|https)://(www\\.)?deezer\\.com/[a-z]{2,3}/album/([0-9]+)(?:.*)$");
    private static final Pattern DEEZER_PLAYLIST_REGEX = Pattern.compile("^(http|https)://(www\\.)?deezer\\.com/[a-z]{2,3}/playlist/([0-9]+)(?:.*)$");
    private static final Pattern YOUTUBE_PLAYLIST_REGEX = Pattern.compile("^(http|https)://(www\\.)?youtube\\.com/playlist\\?list=(.+)$");
    private static final Pattern SOUNDCLOUD_PLAYLIST_REGEX = Pattern.compile("^(http|https)://(www\\.)?soundcloud\\.com/(.+)/sets/(.*)$");

    public static boolean isPlaylistLink(String url) {
        return SPOTIFY_ARTIST_REGEX.matcher(url).matches() ||
                SPOTIFY_ALBUM_REGEX.matcher(url).matches() ||
                SPOTIFY_PLAYLIST_REGEX.matcher(url).matches() ||
                SPOTIFY_PLAYLIST_REGEX_USER.matcher(url).matches() ||
                DEEZER_ALBUM_REGEX.matcher(url).matches() ||
                DEEZER_ARTIST_REGEX.matcher(url).matches() ||
                DEEZER_PLAYLIST_REGEX.matcher(url).matches() ||
                YOUTUBE_PLAYLIST_REGEX.matcher(url).matches() ||
                SOUNDCLOUD_PLAYLIST_REGEX.matcher(url).matches();
    }
}
