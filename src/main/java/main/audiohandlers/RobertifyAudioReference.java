package main.audiohandlers;

import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

public class RobertifyAudioReference extends AudioReference {
    @Getter @Setter
    private final String spotifyID;

    public RobertifyAudioReference(String identifier, String title) {
        super(identifier, title);
        spotifyID = null;
    }

    public RobertifyAudioReference(String identifier, String title, String spotifyID) {
        super(identifier, title);
        this.spotifyID = spotifyID;
    }
}
