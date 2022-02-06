package main.commands.commands.audio.autoplay;

import lavalink.client.player.track.AudioTrack;
import lavalink.client.player.track.AudioTrackInfo;
import net.dv8tion.jda.api.entities.Guild;
import okhttp3.HttpUrl;

public class AutoPlayUtils {
    private static final String RADIO_LIST_PREFIX = "RD";

    public static String getVideoRadioId(String videoID) {
        return RADIO_LIST_PREFIX + videoID;
    }

    public static String getRecommendedTracksURL(String seedVideoId, String lastVideoId) {
        return new HttpUrl.Builder().scheme("https").host("youtube.com").addPathSegment("watch")
                .addQueryParameter("v", seedVideoId).addQueryParameter("list", lastVideoId)
                .build().url().toString();
    }

    public static void loadRecommendedTracks(Guild guild, AudioTrack track) {
        AudioTrackInfo info = track.getInfo();
        String recommendedTracksURL = getRecommendedTracksURL(info.getIdentifier(), info.getIdentifier());
    }
}
