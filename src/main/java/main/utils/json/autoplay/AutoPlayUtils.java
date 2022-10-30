package main.utils.json.autoplay;

import com.neovisionaries.i18n.CountryCode;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;
import lombok.SneakyThrows;
import main.audiohandlers.RobertifyAudioManager;
import main.audiohandlers.sources.autoplay.AutoPlaySourceManager;
import main.audiohandlers.sources.spotify.SpotifyTrack;
import main.main.Robertify;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import okhttp3.HttpUrl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.model_objects.specification.Recommendations;

import java.util.HashMap;

public class AutoPlayUtils {
    private static final Logger logger = LoggerFactory.getLogger(AutoPlayUtils.class);

    private static final String RADIO_LIST_PREFIX = "RD";
    @Getter
    private static final HashMap<Long, String> guildSeeds = new HashMap<>();

    @SneakyThrows
    public static Recommendations getSpotifyRecommendations(String seedArtist, String seedGenre, String seedTrack) {
        return Robertify.getSpotifyApi().getRecommendations()
                .limit(10)
                .market(CountryCode.US)
                .seed_artists(seedArtist)
                .seed_genres(seedGenre)
                .seed_tracks(seedTrack)
                .build().execute();
    }

    public static Recommendations getSpotifyRecommendations(SpotifyTrack track) {
        logger.debug("Attempting to load recommendations with info:\nArtist ID: {}\nGenres: {}\nTrack ID: {}", track.getArtist().getId(),
                track.getArtist().getGenres().toString(),
                track.getIdentifier());
        return getSpotifyRecommendations(
                track.getArtist().getId(),
                track.getArtist().getGenres().toString().replaceAll("[\\[\\]\\s]", ""),
                track.getIdentifier()
        );
    }

    public static void loadRecommendedTracks(Guild guild, TextChannel channel, SpotifyTrack track) {
        if (!guildSeeds.containsKey(guild.getIdLong()))
            guildSeeds.put(guild.getIdLong(), track.getIdentifier());

        final var recommendations = getSpotifyRecommendations(track);
        final var jsonQuery = createAudioTrackObject(recommendations);
        logger.info("Loaded recommendations. Now attempting to load through LavaLink.");
        logger.info("Query: {}", AutoPlaySourceManager.SEARCH_PREFIX + jsonQuery);
        RobertifyAudioManager.getInstance()
                .loadRecommendedTracks(RobertifyAudioManager.getInstance().getMusicManager(guild), channel, AutoPlaySourceManager.SEARCH_PREFIX + jsonQuery);
    }

    private static JSONArray createAudioTrackObject(Recommendations recommendations) {
        final var ret = new JSONArray();
        for (final var track : recommendations.getTracks()) {
            ret.put(
                    new JSONObject()
                            .put("info_identifier", track.getId())
                            .put("info_author", track.getArtists()[0].getName())
                            .put("info_title", track.getName())
                            .put("info_length", track.getDurationMs())
                            .put("info_uri", "https://open.spotify.com/track/" + track.getId())
                            .put("info_isstream", false)
                            .put("source", "spotify")
            );
        }
        return ret;
    }
}
