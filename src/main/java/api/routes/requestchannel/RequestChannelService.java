package api.routes.requestchannel;

import api.routes.requestchannel.dto.CreateRequestChannelDto;
import api.utils.ApiUtils;
import api.utils.GeneralResponse;
import lombok.NoArgsConstructor;
import main.commands.slashcommands.commands.management.requestchannel.RequestChannelCommand;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@NoArgsConstructor
public class RequestChannelService {

    public ResponseEntity<GeneralResponse> createChannel(CreateRequestChannelDto createRequestChannelDto) {
        final var server = ApiUtils.getGuild(createRequestChannelDto.getServer_id());

        try {
            new RequestChannelCommand().createRequestChannel(server, null);
            return ResponseEntity.ok(GeneralResponse.builder()
                    .message("Successfully created a request channel for: " + server.getName())
                    .build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
