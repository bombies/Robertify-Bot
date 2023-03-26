package api.routes.locale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LocaleDto {
    @NotBlank()
    String server_id;
    @NotBlank()
    @Pattern(regexp = "^(en|english|spanish|es|portuguese|pt|russian|ru|dutch|nl|german|de|french|fr)$", flags = { Pattern.Flag.CASE_INSENSITIVE })
    String locale;
}
