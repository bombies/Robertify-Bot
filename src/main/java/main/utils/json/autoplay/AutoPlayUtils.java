package main.utils.json.autoplay;

import lavalink.client.player.track.AudioTrack;
import lavalink.client.player.track.AudioTrackInfo;
import lombok.Getter;
import main.audiohandlers.RobertifyAudioManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import okhttp3.HttpUrl;

import java.util.HashMap;

public class AutoPlayUtils {
    private static final String RADIO_LIST_PREFIX = "RD";
    @Getter
    private static final HashMap<Long, String> guildSeeds = new HashMap<>();

    public static String getVideoRadioId(String videoID) {
        return RADIO_LIST_PREFIX + videoID;
    }

    public static String getRecommendedTracksURL(String seedVideoId, String lastVideoId) {
        return new HttpUrl.Builder().scheme("https").host("youtube.com").addPathSegment("watch")
                .addQueryParameter("v", seedVideoId).addQueryParameter("list", getVideoRadioId(lastVideoId))
                .build().url().toString();
    }

    public static void loadRecommendedTracks(Guild guild, TextChannel channel, AudioTrack track) {
        AudioTrackInfo info = track.getInfo();

        if (!guildSeeds.containsKey(guild.getIdLong()))
            guildSeeds.put(guild.getIdLong(), info.getIdentifier());

        String recommendedTracksURL = getRecommendedTracksURL(guildSeeds.get(guild.getIdLong()), info.getIdentifier());

        RobertifyAudioManager.getInstance()
                .loadRecommendedTracks(RobertifyAudioManager.getInstance().getMusicManager(guild), channel, recommendedTracksURL);
    }
}
