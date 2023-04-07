package main.audiohandlers.sources.local;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerHints;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalSeekableInputStream;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

public class CustomLocalAudioSourceManager extends LocalAudioSourceManager {
    public final static String MP3_LOAD_PREFIX = "local:";
    public final static String M4A_LOAD_PREFIX = "local:";
    public final static String OGG_LOAD_PREFIX = "local:";
    public final static String MP4_LOAD_PREFIX = "local:";
    public final static String WAV_LOAD_PREFIX = "local:";
    public final static String OGG_LOAD_PREFIX = "local:";
    public final static String MP3_LOAD_PREFIX = "local:";
    public final static String MP3_LOAD_PREFIX = "local:";

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        if (!reference.identifier.startsWith(MP3_LOAD_PREFIX))
            return null;

        final var revertedFileStream = new LocalSeekableInputStream() new ByteArrayInputStream(reference.identifier.getBytes(StandardCharsets.UTF_8));

        return handleLoadResult();
    }

    private MediaContainerDetectionResult detectContainerForFile(AudioReference reference, ByteArrayInputStream file, String fileExtension) {
        try (file) {
            return new MediaContainerDetection(containerRegistry, reference, file,
                    MediaContainerHints.from(null, fileExtension)).detectContainer();
        } catch (IOException e) {
            throw new FriendlyException("Failed to open file for reading.", SUSPICIOUS, e);
        }
    }
}
