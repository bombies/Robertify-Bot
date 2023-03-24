package api.routes.requestchannel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ToggleRequestChannelButtonsDto {
    @NotBlank()
    String server_id;
    @NotEmpty()
    @Pattern(regexp = "^(previous|rewind|stop|pnp|skip|favourite|loop|shuffle|disconnect|filters)$", flags = { Pattern.Flag.CASE_INSENSITIVE})
    String[] buttons;
}
