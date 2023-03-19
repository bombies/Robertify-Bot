package api.routes.themes;

import api.utils.ApiUtils;
import api.routes.themes.dto.ThemeDto;
import lombok.RequiredArgsConstructor;
import main.commands.slashcommands.commands.management.ThemeCommand;
import main.constants.RobertifyTheme;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ThemesService {

    public ResponseEntity<ThemeResponse> setTheme(ThemeDto themeDto) {
        final var server = ApiUtils.getGuild(themeDto.getServer_id());
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
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal Server Error",
                    e
            );
        }
    }
}
