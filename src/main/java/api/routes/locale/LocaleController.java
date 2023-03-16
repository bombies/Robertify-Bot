package api.routes.locale;

import api.routes.locale.dto.LocaleDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/locale")
@RequiredArgsConstructor
public class LocaleController {

    private final LocaleService localeService;

    @PostMapping("")
    public ResponseEntity<LocaleResponse> setLocale(@Valid @RequestBody LocaleDto localeDto) {
        return localeService.setLocale(localeDto);
    }
}
