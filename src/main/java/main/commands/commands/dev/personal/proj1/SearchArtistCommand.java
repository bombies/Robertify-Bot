package main.commands.commands.dev.personal.proj1;

import lombok.SneakyThrows;
import main.commands.CommandContext;
import main.commands.IDevCommand;
import main.main.Robertify;

import javax.script.ScriptException;
import java.util.List;

public class SearchArtistCommand implements IDevCommand {
    @Override
    @SneakyThrows
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx))
            return;

        final String artistID = "3TVXtAsR1Inumwj472S9r4";
        var spotifyApi = Robertify.getSpotifyApi();
        var drakeAlbums = spotifyApi.getArtistsAlbums(artistID)
                .build().execute()
                .getItems();

        for (var album : drakeAlbums) {
            System.out.println(album.getName());
        }

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
