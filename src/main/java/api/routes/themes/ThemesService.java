package api.routes.themes;

import api.routes.themes.dto.ThemeDto;
import lombok.RequiredArgsConstructor;
import main.commands.slashcommands.commands.management.ThemeCommand;
import main.constants.RobertifyTheme;
import main.main.Robertify;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ThemesService {

    public ResponseEntity<ThemeResponse> setTheme(ThemeDto themeDto) {
        final var server = Robertify.shardManager.getGuildById(themeDto.serverId);
        if (server == null)
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ThemeResponse.builder().message("There was no guild with the id: " + themeDto.serverId).build());
        try {
            final var theme = RobertifyTheme.parse(themeDto.theme);
            new ThemeCommand().updateTheme(server, theme);
            return ResponseEntity.ok(ThemeResponse.builder().message("Successfully set the theme for " + server.getName() + " to: " + theme.name()).build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ThemeResponse.builder().message("Invalid theme!").build());
        }
    }
}
