package api.routes.themes;

import api.routes.themes.dto.ThemeDto;
import lombok.RequiredArgsConstructor;
import main.commands.slashcommands.commands.management.ThemeCommand;
import main.constants.RobertifyTheme;
import main.main.Robertify;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ThemesService {

    public ResponseEntity<ThemeResponse> setTheme(ThemeDto themeDto) {
        final var server = Robertify.shardManager.getGuildById(themeDto.server_id);
        if (server == null)
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "There was no guild with the id: " + themeDto.server_id
            );
        try {
            final var theme = RobertifyTheme.parse(themeDto.theme);
            new ThemeCommand().updateTheme(server, theme);
            return ResponseEntity.ok(ThemeResponse.builder().message("Successfully set the theme for " + server.getName() + " to: " + theme.name()).build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid theme",
                    e
            );
        }
    }
}
