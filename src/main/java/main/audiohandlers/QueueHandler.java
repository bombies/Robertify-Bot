package main.audiohandlers;

import com.google.common.collect.ImmutableList;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueueHandler {
    private final ConcurrentLinkedQueue<AudioTrack> queue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<AudioTrack> savedQueue = new ConcurrentLinkedQueue<>();
    private final Stack<AudioTrack> previousTracks = new Stack<>();
    @Getter @Setter
    private AudioTrack lastPlayedTrackBuffer;
    @Getter @Setter
    private boolean trackRepeating = false;
    @Getter @Setter
    private boolean queueRepeating = false;

    public boolean add(AudioTrack audioTrack) {
        return queue.offer(audioTrack);
    }

    public boolean addAll(Collection<AudioTrack> audioTracks) {
        return queue.addAll(audioTracks);
    }

    public int size() {
        return queue.size();
    }

    public int previousTracksSize() {
        return previousTracks.size();
    }

    public ImmutableList<AudioTrack> contents() {
        return ImmutableList.copyOf(queue);
    }

    public ImmutableList<AudioTrack> previousTracksContent() {
        return ImmutableList.copyOf(previousTracks);
    }

    public void setSavedQueue(Collection<AudioTrack> tracks) {
        if (!isQueueRepeating())
            return;
        savedQueue.addAll(tracks);
    }

    public void loadSavedQueue() {
        queue.clear();
        queue.addAll(savedQueue);
    }

    public void pushPastTrack(AudioTrack track) {
        previousTracks.push(track);
    }

    public boolean remove(AudioTrack audioTrack) {
        return queue.remove(audioTrack);
    }

    public boolean removeAll(Collection<AudioTrack> audioTracks) {
        return queue.removeAll(audioTracks);
    }

    public AudioTrack poll() {
        return queue.poll();
    }

    public AudioTrack popPreviousTrack() {
        return previousTracks.pop();
    }

    public void clear() {
        queue.clear();
    }

    public void clearSavedQueue() {
        savedQueue.clear();
    }

    public void clearPreviousTracks() {
        previousTracks.clear();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public boolean isPreviousTracksEmpty() {
        return previousTracks.empty();
    }

    public void addToBeginning(AudioTrack audioTrack) {
        final ConcurrentLinkedQueue<AudioTrack> newQueue = new ConcurrentLinkedQueue<>();
        newQueue.offer(audioTrack);
        newQueue.addAll(queue);
        queue.clear();
        queue.addAll(newQueue);
    }

    public void addToBeginning(List<AudioTrack> audioTrack) {
        final ConcurrentLinkedQueue<AudioTrack> newQueue = new ConcurrentLinkedQueue<>();
        newQueue.addAll(audioTrack);
        newQueue.addAll(queue);
        queue.clear();
        queue.addAll(newQueue);
    }

}
