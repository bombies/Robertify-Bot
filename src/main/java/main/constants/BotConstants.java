package main.constants;

import main.main.Config;

public enum BotConstants {
    SPOTIFY_EMOJI("<:spotify:893153435438940181>"),
    ICON_URL(Config.get(ENV.ICON_URL)),
    ROBERTIFY_LOGO("https://i.imgur.com/KioK108.png"),
    ROBERTIFY_LOGO_TRANSPARENT("https://i.imgur.com/IbaIX5e.png"),
    ROBERTIFY_CHRISTMAS_LOGO("https://i.imgur.com/kVyBLi7.png"),
    ROBERTIFY_CHRISTMAS_LOGO_TRANSPARENT("https://i.imgur.com/eSoNR0X.png"),
    ROBERTIFY_EMBED_TITLE(Config.get(ENV.BOT_NAME)),
    BANNED_MESSAGE("You are banned from using commands in this server!"),
    DEFAULT_IMAGE("https://i.imgur.com/VNQvjve.png"),
    USER_AGENT("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36");

    private final String str;

    BotConstants(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
