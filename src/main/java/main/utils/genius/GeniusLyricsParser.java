package main.utils.genius;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

public class GeniusLyricsParser {
    private static final String GENIUS_EMBED_URL_HEAD = "https://genius.com/songs/";
    private static final String GENIUS_EMBED_URL_TAIL = "/embed.js";
    private final GeniusAPI gla;

    public GeniusLyricsParser(GeniusAPI gla) {
        this.gla = gla;
    }

    public String get(String id) {
        return parseLyrics(id);
    }

    private String parseLyrics(String id) {
        try {
            URLConnection connection = new URL(GENIUS_EMBED_URL_HEAD + id + GENIUS_EMBED_URL_TAIL).openConnection();
            connection.setRequestProperty("User-Agent", "1.1.2");

            Scanner scanner = new Scanner(connection.getInputStream());
            scanner.useDelimiter("\\A");
            StringBuilder raw = new StringBuilder();
            while (scanner.hasNext()) {
                raw.append(scanner.next());
            }
            if (raw.toString().equals("")) {
                return null;
            }
            return getReadable(raw.toString());
        } catch (IOException e) {
            return null;
        }
    }

    private String getReadable(String rawLyrics) {
        //Remove start
        rawLyrics = rawLyrics.replaceAll("[\\S\\s]*<div class=\\\\\\\\\\\\\"rg_embed_body\\\\\\\\\\\\\">[ (\\\\\\\\n)]*", "");
        //Remove end
        rawLyrics = rawLyrics.replaceAll("[ (\\\\\\\\n)]*<\\\\/div>[\\S\\s]*", "");
        //Remove tags between
        rawLyrics = rawLyrics.replaceAll("<[^<>]*>", "");
        //Unescape spaces
        rawLyrics = rawLyrics.replaceAll("\\\\\\\\n","\n");
        //Unescape '
        rawLyrics = rawLyrics.replaceAll("\\\\'", "'");
        //Unescape "
        rawLyrics = rawLyrics.replaceAll("\\\\\\\\\\\\\"", "\"");
        return rawLyrics;
    }
}
