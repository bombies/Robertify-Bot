package main.utils.genius;

public class GeniusLyrics {
    private final GeniusAPI gla;
    private final String path;
    private final String id;

    public GeniusLyrics(GeniusAPI gla, String id, String path) {
        this.path = path;
        this.gla = gla;
        this.id = id;
    }

    public String getText() {
        return new GeniusLyricsParser(this.gla).get(this.id);
    }

    @Override
    public String toString() {
        return this.path;
    }
}
