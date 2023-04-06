package main.utils.apis.robertify.imagebuilders;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.utils.GeneralUtils;
import main.utils.json.themes.ThemesConfig;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.InputStream;

public class QueueImageBuilder extends AbstractImageBuilder {
    private final JSONObject obj;
    @Nullable
    private final Guild guild;


    public QueueImageBuilder() {
        super(ImageType.QUEUE);
        this.obj = new JSONObject();
        this.guild = null;
    }

    public QueueImageBuilder(@Nullable Guild guild) {
        super(ImageType.QUEUE);
        this.obj = new JSONObject();
        this.guild = guild;
    }

    public QueueImageBuilder setPage(int page) {
        obj.put(QueryFields.PAGE.toString(), page);
        return this;
    }

    public QueueImageBuilder addTrack(int index, AudioTrack track) {
        final var trackInfo = track.getInfo();
        return this.addTrack(index, trackInfo.title, trackInfo.author, trackInfo.length);
    }

    public QueueImageBuilder addTrack(int index, String title, String artist, long duration) {
        if (!obj.has(QueryFields.TRACKS.toString()))
            obj.put(QueryFields.TRACKS.toString(), new JSONArray());

        final var trackArr = obj.getJSONArray(QueryFields.TRACKS.toString());
        final var trackObj = new JSONObject()
                .put(QueryFields.TRACK_INDEX.toString(), index)
                .put(QueryFields.TRACK_NAME.toString(), title)
                .put(QueryFields.TRACK_ARTIST.toString(), artist)
                .put(QueryFields.TRACK_DURATION.toString(), duration);
        trackArr.put(trackObj);
        return this;
    }

    @Override
    public InputStream build() throws ImageBuilderException {
        if (!obj.has(QueryFields.PAGE.toString()))
            throw new IllegalArgumentException("The page must be provided before building the queue image!");
        if (!obj.has(QueryFields.TRACKS.toString()))
            throw new IllegalArgumentException("The track list must be provided before building the queue image!");

        final var trackObj = obj.getJSONArray(QueryFields.TRACKS.toString());
        for (final var obj : trackObj) {
            final var objAsJSON = (JSONObject) obj;
            final var name = objAsJSON.getString(QueryFields.TRACK_NAME.toString());
            final var artist = objAsJSON.getString(QueryFields.TRACK_NAME.toString());

            if (GeneralUtils.textIsRightToLeft(name) || GeneralUtils.textIsRightToLeft(artist))
                throw new ImageBuilderException("Some text has right to left characters which aren't supported!");
        }

        addQuery(QueryFields.TRACKS, obj.toString());
        if (guild != null)
            addQuery(QueryFields.THEME, new ThemesConfig(guild).getTheme().name().toLowerCase());
        return super.build();
    }

    private enum QueryFields implements ImageQueryField {
        PAGE,
        TRACKS,
        TRACK_INDEX,
        TRACK_NAME,
        TRACK_ARTIST,
        TRACK_DURATION,
        THEME;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }
}
