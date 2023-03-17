package api.routes.requestchannel;

import api.routes.requestchannel.dto.CreateRequestChannelDto;
import api.routes.requestchannel.dto.ToggleRequestChannelButtonDto;
import api.utils.ApiUtils;
import api.utils.GeneralResponse;
import lombok.NoArgsConstructor;
import main.commands.slashcommands.commands.management.requestchannel.RequestChannelCommand;
import main.commands.slashcommands.commands.management.requestchannel.RequestChannelEditCommand;
import main.utils.json.requestchannel.RequestChannelConfig;
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

    public ResponseEntity<GeneralResponse> toggleButton(ToggleRequestChannelButtonDto toggleRequestChannelButtonDto) {
        final var server = ApiUtils.getGuild(toggleRequestChannelButtonDto.getServer_id());
        final var config = new RequestChannelConfig(server);

        if (!config.isChannelSet())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The request channel hasn't been setup for " + server.getName());

        new RequestChannelEditCommand().handleChannelButtonToggle(server, toggleRequestChannelButtonDto.getButton(), null);
        return ResponseEntity.ok(GeneralResponse.builder()
                .message("Successfully toggled the " + toggleRequestChannelButtonDto.getButton() + " button in " + server.getName())
                .build()
        );
    }
}
