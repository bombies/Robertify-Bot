package main.utils.json.autoplay;

import lavalink.client.player.track.AudioTrack;
import lombok.Getter;
import main.audiohandlers.RobertifyAudioManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class AutoPlayUtils {
    private static final Logger logger = LoggerFactory.getLogger(AutoPlayUtils.class);

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
        loadRecommendedTracks(guild, channel, track.getInfo().getIdentifier());
    }

    public static void loadRecommendedTracks(Guild guild, TextChannel channel, String identifier) {
        if (!guildSeeds.containsKey(guild.getIdLong()))
            guildSeeds.put(guild.getIdLong(), identifier);

        String recommendedTracksURL = getRecommendedTracksURL(guildSeeds.get(guild.getIdLong()), identifier);

        RobertifyAudioManager.getInstance()
                .loadRecommendedTracks(RobertifyAudioManager.getInstance().getMusicManager(guild), channel, recommendedTracksURL);
    }
}
