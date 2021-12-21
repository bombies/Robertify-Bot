package main.audiohandlers.spotify;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.*;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioTrackExecutor;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import main.audiohandlers.RobertifyAudioReference;
import main.utils.database.sqlite3.AudioDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SpotifyAudioTrack extends DelegatedAudioTrack {
    private final YoutubeAudioSourceManager manager;
    private final String spotifyID;

    private final Logger logger = LoggerFactory.getLogger(SpotifyAudioTrack.class);

    public SpotifyAudioTrack(AudioTrackInfo trackInfo, YoutubeAudioSourceManager manager, String spotifyID) {
        super(trackInfo);
        this.manager = manager;
        this.spotifyID = spotifyID;
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        AudioItem item = manager.loadItem(null, new RobertifyAudioReference(trackInfo.identifier, null, spotifyID));

        AudioDB audioDB = new AudioDB();

        if (item instanceof AudioPlaylist playlist) {
            AudioTrack track = playlist.getTracks().get(0);

//            logger.info("[FROM SPOTIFY] {} - {} [{}]", trackInfo.title, trackInfo.author, trackInfo.length);

            for (AudioTrack audioTrack : playlist.getTracks()) {

//                logger.info("{} - {} [{}]", audioTrack.getInfo().title, audioTrack.getInfo().author, audioTrack.getDuration());

                if (audioTrack.getDuration() >= trackInfo.length - 7000
                        && audioTrack.getDuration() <= trackInfo.length + 5000
                        && (audioTrack.getInfo().author.toLowerCase().contains(trackInfo.author.toLowerCase())
                        || audioTrack.getInfo().title.toLowerCase().contains(trackInfo.title.toLowerCase()))
                ) {
                    track = audioTrack;
                    break;
                }
            }

            ((YoutubeAudioTrack) track).process(executor);

            if (!audioDB.isTrackCached(spotifyID)) {
                try {
                    audioDB.addTrack(spotifyID, track.getInfo().identifier);
                } catch (Exception e) {
                    logger.error("Error occurred: {}", e.getMessage());
                }
            }
        }
    }

    @Override
    public AudioTrack makeClone() {
        return new SpotifyAudioTrack(trackInfo, manager, spotifyID);
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return super.makeShallowClone();
    }

    @Override
    public long getPosition() {
        return super.getPosition();
    }

    @Override
    public void setPosition(long position) {
        super.setPosition(position);
    }

    @Override
    public long getDuration() {
        return super.getDuration();
    }

    @Override
    public void assignExecutor(AudioTrackExecutor executor, boolean applyPrimordialState) {
        super.assignExecutor(executor, applyPrimordialState);
    }

    @Override
    public AudioTrackExecutor getActiveExecutor() {
        return super.getActiveExecutor();
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public AudioTrackState getState() {
        return super.getState();
    }

    @Override
    public String getIdentifier() {
        return super.getIdentifier();
    }

    @Override
    public boolean isSeekable() {
        return super.isSeekable();
    }

    @Override
    public void setMarker(TrackMarker marker) {
        super.setMarker(marker);
    }

    @Override
    public AudioFrame provide() {
        return super.provide();
    }

    @Override
    public AudioFrame provide(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
        return super.provide(timeout, unit);
    }

    @Override
    public boolean provide(MutableAudioFrame targetFrame) {
        return super.provide(targetFrame);
    }

    @Override
    public boolean provide(MutableAudioFrame targetFrame, long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
        return super.provide(targetFrame, timeout, unit);
    }

    @Override
    public AudioTrackInfo getInfo() {
        return super.getInfo();
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return super.getSourceManager();
    }

    @Override
    public AudioTrackExecutor createLocalExecutor(AudioPlayerManager playerManager) {
        return super.createLocalExecutor(playerManager);
    }

    @Override
    public void setUserData(Object userData) {
        super.setUserData(userData);
    }

    @Override
    public Object getUserData() {
        return super.getUserData();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public <T> T getUserData(Class<T> klass) {
        return super.getUserData(klass);
    }
}
