package main.utils.apis.robertify.imagebuilders;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

public class QueueImageBuilder extends AbstractImageBuilder {
    private final JSONObject obj;


    public QueueImageBuilder() {
        super(ImageType.QUEUE);
        this.obj = new JSONObject();
    }

    public QueueImageBuilder setPage(int page) {
        obj.put(QueryFields.PAGE.toString(), page);
        return this;
    }

    public QueueImageBuilder addTrack(int index, AudioTrack track) {
        if (!obj.has(QueryFields.TRACKS.toString()))
            obj.put(QueryFields.TRACKS.toString(), new JSONArray());

        final var trackInfo = track.getInfo();
        final var trackArr = obj.getJSONArray(QueryFields.TRACKS.toString());
        trackArr.put(new JSONObject()
                .put(QueryFields.TRACK_INDEX.toString(), index)
                .put(QueryFields.TRACK_NAME.toString(), trackInfo.title)
                .put(QueryFields.TRACK_ARTIST.toString(), trackInfo.author)
                .put(QueryFields.TRACK_DURATION.toString(), trackInfo.length)
        );

        return this;
    }

    public QueueImageBuilder addTrack(int index, String title, String artist, long duration) {
        if (!obj.has(QueryFields.TRACKS.toString()))
            obj.put(QueryFields.TRACKS.toString(), new JSONArray());

        final var trackArr = obj.getJSONArray(QueryFields.TRACKS.toString());
        trackArr.put(new JSONObject()
                .put(QueryFields.TRACK_INDEX.toString(), index)
                .put(QueryFields.TRACK_NAME.toString(), title)
                .put(QueryFields.TRACK_ARTIST.toString(), artist)
                .put(QueryFields.TRACK_DURATION.toString(), duration)
        );

        return this;
    }

    @Override
    public File build() throws SocketTimeoutException, ConnectException {
        if (!obj.has(QueryFields.PAGE.toString()))
            throw new IllegalArgumentException("The page must be provided before building the queue image!");
        if (!obj.has(QueryFields.TRACKS.toString()))
            throw new IllegalArgumentException("The track list must be provided before building the queue image!");

        addQuery(QueryFields.TRACKS, obj.toString());
        return super.build();
    }

    private enum QueryFields implements ImageQueryField {
        PAGE,
        TRACKS,
        TRACK_INDEX,
        TRACK_NAME,
        TRACK_ARTIST,
        TRACK_DURATION;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }
}
