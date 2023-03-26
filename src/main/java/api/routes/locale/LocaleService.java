package api.routes.locale;

import api.utils.ApiUtils;
import api.routes.locale.dto.LocaleDto;
import lombok.NoArgsConstructor;
import main.commands.slashcommands.commands.management.LanguageCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@NoArgsConstructor
public class LocaleService {

    public ResponseEntity<LocaleResponse> setLocale(LocaleDto locale) {
        final var guild = ApiUtils.getGuild(locale.getServer_id());
        new LanguageCommand().setLocale(guild, locale.getLocale());
        return ResponseEntity.ok(LocaleResponse.builder()
                .message("You have set the locale in " + guild.getName() + " to: " + locale.getLocale())
                .build()
        );
    }
}
