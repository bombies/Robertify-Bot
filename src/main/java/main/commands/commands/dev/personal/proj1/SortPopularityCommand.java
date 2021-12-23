package main.commands.commands.dev.personal.proj1;

import main.commands.CommandContext;
import main.commands.IDevCommand;
import main.main.Robertify;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.Artist;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SortPopularityCommand implements IDevCommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx))
            return;

        final Message msg = ctx.getMessage();
        EmbedBuilder eb = EmbedUtils.embedMessage("Fetching artist info...");
        msg.replyEmbeds(eb.build()).queue(response -> {
            final var spotifyApi = Robertify.getSpotifyApi();
            List<String> artistIDs = new ArrayList<>() {
                {
                    add("1uNFoZAHBGtllmzznpCI3s");
                    add("3TVXtAsR1Inumwj472S9r4");
                    add("1Xyo4u8uXC1ZmMpatF05PJ");
                    add("4q3ewBCX7sLwd24euuV69X");
                    add("2EMAnMvWE2eb56ToJVfCWs");
                    add("4O15NlyKLIASxsJ0PrXPfz");
                    add("4MCBfE4596Uoi2O4DtmEMz");
                    add("5K4W6rqBFWDnAN6FQUkS6x");
                    add("6qqNVTkY8uBg9cP3Jd7DAH");
                    add("4dpARuHxo51G3z768sgnrY");
                    add("699OTQXzgjhIYAHMy9RyPD");
                    add("6eUKZXaKkcviH0Ku9w2n3V");
                    add("5cj0lLjcoR7YOSnhnX0Po5");
                }
            };

            List<Artist> artists = new ArrayList<>();
            for (String id : artistIDs) {
                try {
                    artists.add(spotifyApi.getArtist(id).build().execute());
                } catch (IOException | SpotifyWebApiException | ParseException e) {
                    e.printStackTrace();
                }
            }

            Collections.shuffle(artists);

            Map<Integer, Artist> unsortedArtistPopularity = new LinkedHashMap<>();
            StringBuilder unsortedNames = new StringBuilder();
            StringBuilder unsortedPopularity = new StringBuilder();

            for (Artist artist : artists) {
                int popularity = artist.getPopularity();
                while (unsortedArtistPopularity.containsKey(artist.getPopularity())) {
                    popularity++;
                    if (!unsortedArtistPopularity.containsKey(popularity)) break;
                }

                unsortedArtistPopularity.put(popularity, artist);
                unsortedNames.append(artist.getName()).append("\n");
                unsortedPopularity.append(popularity).append("\n");
            }

            EmbedBuilder eb1 = EmbedUtils.embedMessage("Fetched artists!\n" +
                    "You are now viewing **"+artists.get(0).getName()+"**");
            eb1.addField("Artists", unsortedNames.toString(), true);
            eb1.addField("Popularity", unsortedPopularity.toString(), true);
            eb1.setThumbnail(artists.get(0).getImages()[1].getUrl());
            eb1.setImage(artists.get(0).getImages()[0].getUrl());

            response.editMessageEmbeds(eb1.build()).queue();
            var sortedArtists = sortArtists(unsortedArtistPopularity);
            var sortedNames = new StringBuilder();
            var sortedPopularity = new StringBuilder();

            for (int key : sortedArtists) {
                sortedNames.append(unsortedArtistPopularity.get(key).getName()).append("\n");
                sortedPopularity.append(key).append("\n");
            }

            Artist mostPopular = unsortedArtistPopularity.get(sortedArtists.get(0));
            EmbedBuilder eb2 = EmbedUtils.embedMessage("🥳 Sorted list!\n**"+mostPopular.getName()+"** is the most popular artist in the list!");
            eb2.addField("Artists", sortedNames.toString(), true);
            eb2.addField("Popularity", sortedPopularity.toString(), true);
            eb2.setThumbnail(mostPopular.getImages()[1].getUrl());
            eb2.setImage(mostPopular.getImages()[0].getUrl());

            response.editMessageEmbeds(eb2.build()).queueAfter(5, TimeUnit.SECONDS);
        });

    }

    private List<Integer> sortArtists(Map<Integer, Artist> unsortedMap) {
        // Store the popularity for each artist in a separate array
        Object[] keySet = unsortedMap.keySet().toArray();

        // Using 1 as the first iterator to reference the first element of the array
        for (int i = 1; i < keySet.length; ++i) {
            int key = (int)keySet[i];
            // Referencing the element that precedes the current key
            int j = i - 1;

            /*Checks if the preceding element is greater than the current key,
            * If it is, the preceding element is moved before the current key.
            * */
            while (j >= 0 && (int)keySet[j] > key) {
                keySet[j+1] = keySet[j];
                j -= 1;
            }
            keySet[j+1] = key;
        }

        // Creating a new list for the sorted popularity
        List<Integer> sortedList = new ArrayList<>();

        // For each object in the sorted key set
        for (Object obj : keySet)
            sortedList.add((int) obj);

        return sortedList; // Return the sorted list
    }

    @Override
    public String getName() {
        return "insertionsort";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return List.of("is");
    }
}
