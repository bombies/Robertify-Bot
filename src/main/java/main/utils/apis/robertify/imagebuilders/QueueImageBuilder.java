package main.utils.apis.robertify.imagebuilders;

import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.InputStream;

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
        final var trackInfo = track.getInfo();
        return this.addTrack(index, trackInfo.title, trackInfo.author, trackInfo.length, track instanceof MirroringAudioTrack mt ? mt.getArtworkURL() : null);
    }

    public QueueImageBuilder addTrack(int index, String title, String artist, long duration, @Nullable String trackArtwork) {
        if (!obj.has(QueryFields.TRACKS.toString()))
            obj.put(QueryFields.TRACKS.toString(), new JSONArray());

        final var trackArr = obj.getJSONArray(QueryFields.TRACKS.toString());
        final var trackObj = new JSONObject()
                .put(QueryFields.TRACK_INDEX.toString(), index)
                .put(QueryFields.TRACK_NAME.toString(), title)
                .put(QueryFields.TRACK_ARTIST.toString(), artist)
                .put(QueryFields.TRACK_DURATION.toString(), duration);

        if (trackArtwork != null)
            trackObj.put("artwork", trackArtwork);
        trackArr.put(trackObj);
        return this;
    }

    @Override
    public InputStream build() throws ImageBuilderException {
        if (!obj.has(QueryFields.PAGE.toString()))
            throw new IllegalArgumentException("The page must be provided before building the queue image!");
        if (!obj.has(QueryFields.TRACKS.toString()))
            throw new IllegalArgumentException("The track list must be provided before building the queue image!");

        addQuery(QueryFields.TRACKS, obj.toString());
        final var firstTrack = (JSONObject) obj.getJSONArray(QueryFields.TRACKS.toString()).get(0);
        if (firstTrack.has("artwork"))
            addQuery(QueryFields.NEXT_IMG, firstTrack.getString("artwork"));
        return super.build();
    }

    private enum QueryFields implements ImageQueryField {
        PAGE,
        TRACKS,
        TRACK_INDEX,
        TRACK_NAME,
        TRACK_ARTIST,
        TRACK_DURATION,
        NEXT_IMG;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }
}
