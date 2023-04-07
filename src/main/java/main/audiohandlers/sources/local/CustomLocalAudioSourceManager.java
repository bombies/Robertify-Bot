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
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

public class CustomLocalAudioSourceManager extends LocalAudioSourceManager {
    private final static String LOAD_PREFIX = "local#";
    public final static String MP3_LOAD_PREFIX = LOAD_PREFIX + "mp3:";
    public final static String M4A_LOAD_PREFIX = LOAD_PREFIX + "m4a:";
    public final static String OGG_LOAD_PREFIX = LOAD_PREFIX + "ogg:";
    public final static String MP4_LOAD_PREFIX = LOAD_PREFIX + "mp4:";
    public final static String WAV_LOAD_PREFIX = LOAD_PREFIX + "wav:";
    public final static String MOV_LOAD_PREFIX = LOAD_PREFIX + "mov:";
    public final static String WEBM_LOAD_PREFIX = LOAD_PREFIX + "webm:";
    public final static String AAC_LOAD_PREFIX = LOAD_PREFIX + "aac:";
    public final static String FLAC_LOAD_PREFIX = LOAD_PREFIX + "flac:";

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        if (!reference.identifier.startsWith(LOAD_PREFIX))
            return null;

        final var byteFileStream = new ByteArrayInputStream(reference.identifier.getBytes(StandardCharsets.UTF_8));
        try {
            final var simulatedFileName = "localaudio/" + UUID.randomUUID() + "." + getFileExtensionFromIdentifier(reference.identifier);
            final var simulatedFile = new File(simulatedFileName);
            IOUtils.copy(byteFileStream, new FileOutputStream(simulatedFileName));
            return handleLoadResult(detectContainerForFile(reference, simulatedFile));
        } catch (IOException e) {
            return null;
        }
    }

    private String getFileExtensionFromIdentifier(String identifier) {
        if (!identifier.startsWith(LOAD_PREFIX))
            return null;
        return identifier.split(":")[0]
                .split("#")[1];
    }

    private MediaContainerDetectionResult detectContainerForFile(AudioReference reference, File file) {
        try (LocalSeekableInputStream inputStream = new LocalSeekableInputStream(file)) {
            int lastDotIndex = file.getName().lastIndexOf('.');
            String fileExtension = lastDotIndex >= 0 ? file.getName().substring(lastDotIndex + 1) : null;

            return new MediaContainerDetection(containerRegistry, reference, inputStream,
                    MediaContainerHints.from(null, fileExtension)).detectContainer();
        } catch (IOException e) {
            throw new FriendlyException("Failed to open file for reading.", SUSPICIOUS, e);
        }
    }
}
