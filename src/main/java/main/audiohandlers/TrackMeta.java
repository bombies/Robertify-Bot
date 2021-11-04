package main.audiohandlers;

import lombok.Data;

@Data
public class TrackMeta {
    private final String name;
    private final String artist;
    private final long duration;
}
