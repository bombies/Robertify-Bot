package api.routes.themes.dto;

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
public class ThemeDto {
    @NotBlank()
    public String server_id;
    @NotBlank()
    @Pattern(regexp = "^(green|gold|red|yellow|orange|dark|light|blue|light_blue|lightblue|pink|purple|mint|pastel_yellow|pastel_purple|pastel_red|baby_blue)$")
    public String theme;
}
