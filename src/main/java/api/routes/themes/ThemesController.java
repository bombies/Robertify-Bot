package api.routes.themes;

import api.routes.themes.dto.ThemeDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/themes")
@RequiredArgsConstructor
public class ThemesController {
    public final ThemesService themesService;

    @PostMapping()
    public ResponseEntity<ThemeResponse> setTheme(@Valid @RequestBody ThemeDto themeDto) {
        return themesService.setTheme(themeDto);
    }
}
