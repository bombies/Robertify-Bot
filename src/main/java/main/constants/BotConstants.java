package main.constants;

import main.main.Config;

public enum BotConstants {
    SPOTIFY_EMOJI("<:spotify:893153435438940181>"),
    ICON_URL("https://cdn-icons-png.flaticon.com/512/174/174872.png"),
    ROBERTIFY_EMBED_TITLE(Config.get(ENV.BOT_NAME));

    private final String str;

    BotConstants(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
