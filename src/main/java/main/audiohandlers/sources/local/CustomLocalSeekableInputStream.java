package main.audiohandlers.sources.local;

import com.sedmelluq.discord.lavaplayer.tools.io.ExtendedBufferedInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class CustomLocalSeekableInputStream extends SeekableInputStream {
    private static final Logger log = LoggerFactory.getLogger(com.sedmelluq.discord.lavaplayer.source.local.LocalSeekableInputStream.class);

    private final ByteArrayInputStream inputStream;
    private final ExtendedBufferedInputStream bufferedStream;
    private long position;

    /**
     * @param file File to create a stream for.
     */
    public CustomLocalSeekableInputStream(ByteArrayInputStream file) {
        super(file.available(), 0);

        try {
            inputStream = file;
            bufferedStream = new ExtendedBufferedInputStream(inputStream);
            channel = inputStream.getChannel();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param contentLength   Total stream length
     * @param maxSkipDistance Maximum distance that should be skipped by reading and discarding
     */
    public CustomLocalSeekableInputStream(long contentLength, long maxSkipDistance) {
        super(contentLength, maxSkipDistance);
    }

    @Override
    public int read() throws IOException {
        int result = bufferedStream.read();
        if (result >= 0) {
            position++;
        }

        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = bufferedStream.read(b, off, len);
        position += read;
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = bufferedStream.skip(n);
        position += skipped;
        return skipped;
    }

    @Override
    public int available() throws IOException {
        return bufferedStream.available();
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public long getPosition() {
        return position;
    }

    @Override
    public boolean canSeekHard() {
        return true;
    }

    @Override
    public List<AudioTrackInfoProvider> getTrackInfoProviders() {
        return Collections.emptyList();
    }

    @Override
    protected void seekHard(long position) throws IOException {
        channel.position(position);
        this.position = position;
        bufferedStream.discardBuffer();
    }
}
