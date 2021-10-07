package main.constants;

public enum BotConstants {
    SPOTIFY_EMOJI("<:spotify:893153435438940181>"),
    SPOTIFY_ICON_URL("https://cdn-icons-png.flaticon.com/512/174/174872.png"),
    ROBERTIFY_EMBED_TITLE("Robertify");

    private final String str;

    BotConstants(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
