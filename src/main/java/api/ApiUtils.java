package api;

import main.main.Robertify;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ApiUtils {

    public static Guild getGuild(@NotNull String id) {
        final var server = Robertify.shardManager.getGuildById(id);
        if (server == null)
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "There was no guild with the id: " + id
            );
        return server;
    }
}
