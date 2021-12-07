package main.constants;

import main.main.Config;

public enum BotConstants {
    SPOTIFY_EMOJI("<:spotify:893153435438940181>"),
    ICON_URL(Config.get(ENV.ICON_URL)),
    ROBERTIFY_EMBED_TITLE(Config.get(ENV.BOT_NAME)),
    BANNED_MESSAGE("You are banned from using commands in this server!"),
    DEFAULT_IMAGE("https://i.imgur.com/VNQvjve.png");

    private final String str;

    BotConstants(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
