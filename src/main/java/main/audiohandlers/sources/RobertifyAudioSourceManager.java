package main.audiohandlers.sources;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;

public abstract class RobertifyAudioSourceManager implements AudioSourceManager {
    public static String ISRC_PATTERN = "%ISRC%";
    public static String QUERY_PATTERN = "%QUERY%";
    private final AudioPlayerManager audioPlayerManager;

    public RobertifyAudioSourceManager(AudioPlayerManager audioPlayerManager) {
        this.audioPlayerManager = audioPlayerManager;
    }

    public AudioPlayerManager getAudioPlayerManager() {
        return this.audioPlayerManager;
    }

    public abstract String getSearchPrefix();
}
