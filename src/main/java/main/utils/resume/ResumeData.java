package main.utils.resume;

import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lombok.Getter;
import lombok.SneakyThrows;
import main.audiohandlers.RobertifyAudioManager;
import main.audiohandlers.sources.deezer.DeezerSourceManager;
import main.audiohandlers.sources.deezer.DeezerTrack;
import main.audiohandlers.sources.spotify.SpotifySourceManager;
import main.audiohandlers.sources.spotify.SpotifyTrack;
import main.constants.BotConstants;
import main.constants.JSONConfigFile;
import main.main.Robertify;
import main.utils.database.mongodb.AbstractMongoDatabase;
import main.utils.database.mongodb.cache.redis.AbstractRedisCache;
import main.utils.database.mongodb.cache.redis.RedisCache;
import main.utils.json.AbstractJSON;
import main.utils.json.AbstractJSONFile;
import main.utils.json.GenericJSONField;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.List;

public class ResumeData extends RedisCache {
    private final static Logger logger = LoggerFactory.getLogger(ResumeData.class);
    private final static int MAX_TRACKS_TO_SAVE = 1000;
    private final static String CACHE_ID = "guildResumeCache";

    public ResumeData() {
        super(CACHE_ID);
    }

    public void addGuild(long gid, long cid, AudioTrack playingTrack, AbstractQueue<AudioTrack> queue) {
        if (playingTrack == null)
            return;

        if (guildHasInfo(gid))
            removeGuild(gid);

        final var guildObj = new JSONObject();
        guildObj.put(Fields.GUILD_ID.toString(), gid);
        guildObj.put(Fields.CHANNEL_ID.toString(), cid);

       guildObj.put(Fields.PLAYING_TRACK.toString(), createAudioTrackObject(playingTrack));

       final var queueArr = new JSONArray();
       int i = 0;
       for (AudioTrack audioTrack : queue) {
           if (audioTrack == null) continue;
           queueArr.put(createAudioTrackObject(audioTrack));
           if (++i == MAX_TRACKS_TO_SAVE) break;
       }

       guildObj.put(Fields.QUEUE.toString(), queueArr);
       updateCache(String.valueOf(gid), guildObj);
    }

    public void removeGuild(long gid) {
        if (!guildHasInfo(gid))
            return;
        removeFromCache(String.valueOf(gid));
    }

    public GuildResumeData getInfo(long gid) {
        if (!guildHasInfo(gid))
            return null;

        final var guildObj = getJSONByGuild(gid);

        return new GuildResumeData(
                guildObj.getLong(Fields.GUILD_ID.toString()),
                guildObj.getLong(Fields.CHANNEL_ID.toString()),
                guildObj.getJSONObject(Fields.PLAYING_TRACK.toString()),
                guildObj.getJSONArray(Fields.QUEUE.toString())
        );
    }

    public boolean guildHasInfo(long gid) {
        JSONObject jsonObject = getJSONByGuild(gid);
        if (jsonObject == null)
            return false;
        return !jsonObject.isEmpty();
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

    @SneakyThrows
    public List<AudioTrack> assembleSpotifyTracks(JSONArray trackArr) {
        final List<AudioTrack> spotifyTracks = new ArrayList<>();
        final List<StringBuilder>  ids = new ArrayList<>();
        final List<AudioTrackInfo> trackInfos = new ArrayList<>();

        int i = 0;
        for (var obj : trackArr) {
            if (ids.size() < i + 1)
                ids.add(i, new StringBuilder());

            final var trackObj = (JSONObject) obj;

            logger.debug("Source: {}", trackObj.getString("source"));
            if (!trackObj.getString("source").equals("spotify"))
                continue;

            ids.get(i).append(trackObj.getString("info_identifier")).append(",");
            logger.debug("Added Spotify track with ID {} to id builder {}", trackObj.getString("info_identifier"), i);

            trackInfos.add(getTrackInfo(trackObj));
            i += (ids.get(i).toString().split(",").length == 49) ? 1 : 0;
        }

        SpotifyApi spotifyApi = Robertify.getSpotifyApi();
        for (var idBuilder : ids) {
            Track[] tracks = spotifyApi.getSeveralTracks(idBuilder.toString().split(",")).build().execute();

            for (int j = 0; j < tracks.length; j++) {
                Image[] images = tracks[j].getAlbum().getImages();
                spotifyTracks.add(new SpotifyTrack(
                        trackInfos.get(j),
                        tracks[j].getExternalIds().getExternalIds().getOrDefault("isrc", null),
                        images.length > 0 ? images[0].getUrl() : BotConstants.DEFAULT_IMAGE.toString(),
                        new SpotifySourceManager(RobertifyAudioManager.getInstance().getPlayerManager())
                ));
            }
        }

        return spotifyTracks;
    }

    @SneakyThrows
    public AudioTrack assembleTrack(JSONObject trackObj, boolean assembleSpotify) {
        final AudioTrack track;
        final var trackInfo = getTrackInfo(trackObj);

        String source = trackObj.getString("source");
        switch (source) {
            case "spotify" -> {
                if (assembleSpotify) {
                    Track spotifyTrack = Robertify.getSpotifyApi().getTrack(trackInfo.identifier).build().execute();
                    track = new SpotifyTrack(
                            trackInfo,
                            spotifyTrack.getExternalIds().getExternalIds().getOrDefault("isrc", null),
                            spotifyTrack.getAlbum().getImages()[0].getUrl(),
                            new SpotifySourceManager(RobertifyAudioManager.getInstance().getPlayerManager())
                    );
                } else return null;
            }
            case "deezer" -> {
                api.deezer.objects.Track deezerTrack = Robertify.getDeezerApi().track().getById(Integer.parseInt(trackInfo.identifier)).execute();
                track = new DeezerTrack(
                        trackInfo,
                        deezerTrack.getIsrc(),
                        (deezerTrack.getAlbum().getCoverXl() == null) ? "https://i.imgur.com/VNQvjve.png" : deezerTrack.getAlbum().getCoverXl(),
                        new DeezerSourceManager(RobertifyAudioManager.getInstance().getPlayerManager())
                );
            }
            case "soundcloud" -> track = new SoundCloudAudioTrack(trackInfo, SoundCloudAudioSourceManager.createDefault());
            case "youtube" -> track = new YoutubeAudioTrack(trackInfo, new YoutubeAudioSourceManager());
            default -> track = null;
        }

        return track;
    }

    private AudioTrackInfo getTrackInfo(JSONObject trackObj) {
        return new AudioTrackInfo(
                trackObj.getString("info_title"),
                trackObj.getString("info_author"),
                trackObj.getLong("info_length"),
                trackObj.getString("info_identifier"),
                trackObj.getBoolean("info_isstream"),
                trackObj.getString("info_uri")
        );
    }

    public enum Fields implements GenericJSONField {
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

    public static class GuildResumeData {
        @Getter
        private final long guildId;
        @Getter
        private final long channelId;
        @Getter
        private final AudioTrack playingTrack;
        @Getter
        private final List<AudioTrack> queue;
        @Getter
        private final JSONObject guildObject;

        protected GuildResumeData(long guildId, long channelId, JSONObject playingTrackObj, JSONArray queueObj) {
            this.guildId = guildId;
            this.channelId = channelId;

            ResumeData resumeData = new ResumeData();
            playingTrack = resumeData.assembleTrack(playingTrackObj, true);

            this.queue = new ArrayList<>();

            queue.addAll(resumeData.assembleSpotifyTracks(queueObj));

            for (var obj : queueObj) {
                final var trackObj = (JSONObject) obj;

                AudioTrack track = resumeData.assembleTrack(trackObj, false);

                if (track == null)
                    continue;
                queue.add(track);
            }

            guildObject = new JSONObject();
            guildObject.put(Fields.GUILD_ID.toString(), guildId);
            guildObject.put(Fields.CHANNEL_ID.toString(), channelId);
            guildObject.put(Fields.PLAYING_TRACK.toString(), playingTrackObj);
            guildObject.put(Fields.QUEUE.toString(), queueObj);
        }

        @Override
        public String toString() {
            return "GuildResumeData={guildId={"+ guildId +"},channelId={" + channelId + "},queue={" + queue.toString() + "}}";
        }
    }
}
