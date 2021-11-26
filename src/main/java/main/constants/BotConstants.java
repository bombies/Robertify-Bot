package main.constants;

import main.main.Config;

public enum BotConstants {
    SPOTIFY_EMOJI("<:spotify:893153435438940181>"),
    ICON_URL(Config.get(ENV.ICON_URL)),
    ROBERTIFY_EMBED_TITLE(Config.get(ENV.BOT_NAME)),
    BANNED_MESSAGE("You are banned from using commands in this server!"),
    PREVIOUS_EMOJI("<:rewind:913595299203805194>"),
    REWIND_EMOJI("<:back:913595555811307541>"),
    PLAY_EMOJI("<:play:913594529603547237>"),
    END_EMOJI("<:next:913594937298280528>"),
    PAUSE_EMOJI("<:pause:913595131058356256>"),
    STOP_EMOJI("<:stop:913601140879425557>");

    private final String str;

    BotConstants(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
