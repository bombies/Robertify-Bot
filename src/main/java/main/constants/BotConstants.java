package main.constants;

public enum BotConstants {
    SPOTIFY_EMOJI("<:spotify:893153435438940181>"),
    ROBERTIFY_EMBED_TITLE(SPOTIFY_EMOJI + "  Robertify");

    private final String str;

    BotConstants(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
