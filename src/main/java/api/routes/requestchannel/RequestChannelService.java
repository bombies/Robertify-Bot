package api.routes.requestchannel;

import api.routes.requestchannel.dto.CreateRequestChannelDto;
import api.routes.requestchannel.dto.ToggleRequestChannelButtonDto;
import api.utils.ApiUtils;
import api.utils.GeneralResponse;
import api.utils.ReqChannelCreationResponse;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.commands.slashcommands.commands.management.requestchannel.RequestChannelCommand;
import main.commands.slashcommands.commands.management.requestchannel.RequestChannelEditCommand;
import main.utils.json.requestchannel.RequestChannelConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@NoArgsConstructor
public class RequestChannelService {

    public ResponseEntity<ReqChannelCreationResponse> createChannel(CreateRequestChannelDto createRequestChannelDto) {
        final var server = ApiUtils.getGuild(createRequestChannelDto.getServer_id());

        try {
            final var channel = new RequestChannelCommand().createRequestChannel(server).get();
            return ResponseEntity.ok(ReqChannelCreationResponse.builder()
                            .channel_id(String.valueOf(channel.getChannelId()))
                            .message_id(String.valueOf(channel.getMessageId()))
                            .config(channel.getConfig().toString())
                    .build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while creating the request channel", e);
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
