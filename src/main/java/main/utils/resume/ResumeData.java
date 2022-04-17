package main.utils.resume;

import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lombok.Getter;
import main.audiohandlers.RobertifyAudioManager;
import main.audiohandlers.sources.deezer.DeezerSourceManager;
import main.audiohandlers.sources.deezer.DeezerTrack;
import main.audiohandlers.sources.spotify.SpotifySourceManager;
import main.audiohandlers.sources.spotify.SpotifyTrack;
import main.constants.JSONConfigFile;
import main.utils.json.AbstractJSONFile;
import main.utils.json.GenericJSONField;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.List;

public class ResumeData extends AbstractJSONFile {

    public ResumeData() {
        super(JSONConfigFile.RESUME_DATA);
    }

    @Override
    public void initConfig() {
        try {
            makeConfigFile();
        } catch (IllegalStateException e) {
            return;
        }

        final var obj = new JSONObject();
        final var guilds = new JSONArray();

        obj.put(Fields.GUILDS.toString(), guilds);


        setJSON(obj);
    }

    public void addGuild(long gid, long cid, AudioTrack playingTrack, AbstractQueue<AudioTrack> queue) {
        if (guildHasInfo(gid))
            removeGuild(gid);

        final var guildObj = new JSONObject();
        guildObj.put(Fields.GUILD_ID.toString(), gid);
        guildObj.put(Fields.CHANNEL_ID.toString(), cid);

       guildObj.put(Fields.PLAYING_TRACK.toString(), createAudioTrackObject(playingTrack));

       final var queueArr = new JSONArray();
       queue.forEach(track -> queueArr.put(createAudioTrackObject(track)));

       guildObj.put(Fields.QUEUE.toString(), queueArr);

       final var obj = getJSONObject();
       obj.getJSONArray(Fields.GUILDS.toString())
                       .put(guildObj);

       setJSON(obj);
    }

    public void removeGuild(long gid) {
        if (!guildHasInfo(gid))
            return;

        final var obj = getJSONObject();
        final var arr = obj.getJSONArray(Fields.GUILDS.toString());

        arr.remove(getIndexOfObjectInArray(arr, Fields.GUILD_ID, gid));

        setJSON(obj);
    }

    public GuildResumeData getInfo(long gid) {
        if (!guildHasInfo(gid))
            return null;

        final var guildArr = getJSONObject().getJSONArray(Fields.GUILDS.toString());
        final var guildObj = guildArr.getJSONObject(getIndexOfObjectInArray(guildArr, Fields.GUILD_ID, gid));

        return new GuildResumeData(
                guildObj.getLong(Fields.GUILD_ID.toString()),
                guildObj.getLong(Fields.CHANNEL_ID.toString()),
                guildObj.getJSONObject(Fields.PLAYING_TRACK.toString()),
                guildObj.getJSONArray(Fields.QUEUE.toString())
        );
    }

    private boolean guildHasInfo(long gid) {
        JSONObject jsonObject = getJSONObject();

        return arrayHasObject(jsonObject.getJSONArray(Fields.GUILDS.toString()), Fields.GUILDS, gid);
    }

    private JSONObject createAudioTrackObject(AudioTrack track) {
        final var ret = new JSONObject();
        AudioTrackInfo info = track.getInfo();
        ret.put("info_identifier", info.identifier);
        ret.put("info_author", info.author);
        ret.put("info_title", info.title);
        ret.put("info_length", info.length);
        ret.put("info_uri", info.uri);
        ret.put("info_isstream", info.isStream);
        ret.put("source", track.getSourceManager().getSourceName());
        return ret;
    }

    public enum Fields implements GenericJSONField {
        GUILDS("guilds"),
        GUILD_ID("guild_id"),
        CHANNEL_ID("channel_id"),
        PLAYING_TRACK("playing_track"),
        QUEUE("queue");

        private final String str;

        Fields(String str){
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    protected static class GuildResumeData {
        @Getter
        private final long guildId;
        @Getter
        private final long channelId;
        @Getter
        private final AudioTrack playingTrack;
        @Getter
        private final List<AudioTrack> queue;

        public GuildResumeData(long guildId, long channelId, JSONObject playingTrackObj, JSONArray queueObj) {
            this.guildId = guildId;
            this.channelId = channelId;

            playingTrack = assembleTrack(playingTrackObj);

            this.queue = new ArrayList<>();
            for (var obj : queueObj) {
                final var trackObj = (JSONObject) obj;

                AudioTrack track = assembleTrack(trackObj);

                if (track == null)
                    continue;
                queue.add(track);
            }
        }

        private AudioTrack assembleTrack(JSONObject trackObj) {
            final AudioTrack track;
            final var trackInfo = getTrackInfo(trackObj);

            String source = trackObj.getString("source");
            switch (source) {
                case "spotify" -> track = new SpotifyTrack(trackInfo, null, null, new SpotifySourceManager(RobertifyAudioManager.getInstance().getPlayerManager()));
                case "deezer" -> track = new DeezerTrack(trackInfo, null, null, new DeezerSourceManager(RobertifyAudioManager.getInstance().getPlayerManager()));
                case "soundcloud" -> track = new SoundCloudAudioTrack(trackInfo, SoundCloudAudioSourceManager.createDefault());
                case "youtube" -> track = new YoutubeAudioTrack(trackInfo, new YoutubeAudioSourceManager(true));
                default -> track = null;
            }

            return track;
        }

        private AudioTrackInfo getTrackInfo(JSONObject trackObj) {
            return new AudioTrackInfo(
                    trackObj.getString("info_title"),
                    trackObj.getString("info_author"),
                    trackObj.getLong("info_length"),
                    trackObj.getString("info_indentifier"),
                    trackObj.getBoolean("info_isstream"),
                    trackObj.getString("info_uri")
            );
        }

    }
}
