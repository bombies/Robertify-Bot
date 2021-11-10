package main.commands.commands.dev.personal.proj1;

import lombok.SneakyThrows;
import main.commands.CommandContext;
import main.commands.IDevCommand;
import main.main.Robertify;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.Album;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class SearchArtistCommand implements IDevCommand {
    @Override
    @SneakyThrows
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx))
            return;

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        AtomicReference<EmbedBuilder> eb = new AtomicReference<>();

        if (args.isEmpty()) {
            eb.set(EmbedUtils.embedMessage("You must provide the name of a Drake album you'd like to search for"));
            msg.replyEmbeds(eb.get().build()).queue();
            return;
        }

        eb.set(EmbedUtils.embedMessage("Linearly searching for `" + String.join(" ", args) + "`..."));
        msg.replyEmbeds(eb.get().build()).queue(response -> {
            final String artistID = "3TVXtAsR1Inumwj472S9r4"; // Drake's spotify artist ID.
            final String albumName = String.join(" ", args); // The album to be searched for
            var spotifyApi = Robertify.getSpotifyApi();

            // Getting the albums Drake was published from Spotify
            AlbumSimplified[] drakeAlbums = new AlbumSimplified[0];
            try {
                drakeAlbums = spotifyApi.getArtistsAlbums(artistID)
                        .build().execute()
                        .getItems();
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                e.printStackTrace();
            }

            // Search for the specific album name in the array of Drake albums retrieved.
            var albumSimplified = linearSearch(drakeAlbums, albumName);

            // Checking if there was an album found with the album name to be search for
            if (albumSimplified == null) {
                // If the above condition is true, the user will be told that the album doesn't exist by Drake.
                eb.set(EmbedUtils.embedMessage("There was no such Drake album called `" + albumName + "`!"));
                response.editMessageEmbeds(eb.get().build()).queue();
                return;
            }

            /* Everything after here is irrelevant for the presentation
            *  It's just the logic for preparing and displaying the output.
            * */
            Album album = null;
            try {
                album = spotifyApi.getAlbum(albumSimplified.getId())
                        .build().execute();
            } catch (IOException | ParseException | SpotifyWebApiException e) {
                e.printStackTrace();
            }

            eb.set(EmbedUtils.embedMessage("Found the album!"));
            eb.get().setThumbnail(album.getImages()[0].getUrl());
            eb.get().setTitle(album.getName() + " by Drake");
            eb.get().addField("Release date", album.getReleaseDate(), true);
            eb.get().addField("Recording Label", album.getLabel(), true);
            eb.get().addBlankField(false);
            eb.get().setImage(album.getImages()[0].getUrl());

            StringBuilder trackListString = new StringBuilder();
            var trackList = album.getTracks();
            for (TrackSimplified track : trackList.getItems())
                trackListString.append("**→** ").append(track.getName()).append("\n");

            eb.get().addField("Track List (" + trackList.getItems().length + ")", trackListString.toString(), false);
            response.editMessageEmbeds(eb.get().build()).queue();
        });
    }

    /**
     * Perform a linear search for a specific array of albums.
     * @param arr Array to be searched
     * @param searchString Name of album to be searched for
     * @return The album that matches the name to be searched for
     */
    private AlbumSimplified linearSearch(AlbumSimplified[] arr, String searchString) {
        for (var element : arr) // For each element in the array
            // If the name of the album is equal to the search string passed regardless of the cases
            if (element.getName().equalsIgnoreCase(searchString))
                // The element is returned if the above condition is true
                return element;
        // If the searchString parameter isn't found in the array a null value is returned.
        return null;
    }

    @Override
    public String getName() {
        return "linearsearch";
    }

    @Override
    public String getHelp(String guildID) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return List.of("ls");
    }
}
