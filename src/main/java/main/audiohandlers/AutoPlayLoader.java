package main.audiohandlers;

import lavalink.client.io.FriendlyException;
import lavalink.client.io.LoadResultHandler;
import lavalink.client.player.track.AudioPlaylist;
import lavalink.client.player.track.AudioTrack;

import java.util.List;

public class AutoPlayLoader implements LoadResultHandler {

    @Override
    public void trackLoaded(AudioTrack track) {

    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {

    }

    @Override
    public void searchResultLoaded(List<AudioTrack> tracks) {

    }

    @Override
    public void noMatches() {

    }

    @Override
    public void loadFailed(FriendlyException exception) {

    }
}
