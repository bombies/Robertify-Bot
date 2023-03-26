package api.utils;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReqChannelCreationResponse {
    @NotBlank()
    String channel_id;
    @NotBlank()
    String message_id;
    Object config;
}
