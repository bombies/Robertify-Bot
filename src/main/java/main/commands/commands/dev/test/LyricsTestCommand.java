package main.commands.commands.dev.test;

import lombok.SneakyThrows;
import main.commands.CommandContext;
import main.commands.commands.ITestCommand;
import main.utils.GeneralUtils;
import main.utils.genius.GeniusAPI;
import main.utils.genius.GeniusSongSearch;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.File;
import java.util.List;

public class LyricsTestCommand implements ITestCommand {
    private Logger logger = LoggerFactory.getLogger(LyricsTestCommand.class);

    @Override @SneakyThrows
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        final List<String> args = ctx.getArgs();
        final TextChannel channel = ctx.getChannel();

        if (args.isEmpty()) {
            channel.sendMessage("You must provide args").queue();
            return;
        }

        String query = String.join(" ", args);

        GeniusAPI gla = new GeniusAPI();
        GeniusSongSearch songSearch = gla.search(query);

        logger.info("Response code: {}", songSearch.getStatus());

        if (songSearch.getStatus() == 404 || songSearch.getHits().size() == 0) {
            channel.sendMessage("Nothing was found for `"+query+"`").queue();
            return;
        }

        GeniusSongSearch.Hit hit = songSearch.getHits().get(0);

        try {
            channel.sendMessageEmbeds(EmbedUtils.embedMessageWithTitle(hit.getTitle(), hit.fetchLyrics()).build()).queue();
        } catch (IllegalArgumentException e) {
            File file = new File("lyricstest.txt");
            file.createNewFile();
            GeneralUtils.setFileContent(file, hit.fetchLyrics());
            channel.sendMessage("Lyrics fetched.").addFile(file).queue(msg -> file.delete());
        }
    }

    @Override
    public String getName() {
        return "lyricstest";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return List.of("lt");
    }
}
