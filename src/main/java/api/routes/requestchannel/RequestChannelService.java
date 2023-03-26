package api.routes.requestchannel;

import api.routes.requestchannel.dto.CreateRequestChannelDto;
import api.routes.requestchannel.dto.ToggleRequestChannelButtonDto;
import api.routes.requestchannel.dto.ToggleRequestChannelButtonsDto;
import api.utils.ApiUtils;
import api.utils.GeneralResponse;
import api.utils.ReqChannelCreationResponse;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.commands.slashcommands.commands.management.requestchannel.RequestChannelCommand;
import main.commands.slashcommands.commands.management.requestchannel.RequestChannelEditCommand;
import main.utils.json.requestchannel.RequestChannelConfig;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

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
        } catch (InsufficientPermissionException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "I don't have the required permissions to create a request channel", e);
        } catch (Exception e) {
            log.error("Unexpected error", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while creating the request channel", e);
        }
    }

    public ResponseEntity<GeneralResponse> deleteChannel(String id) {
        final var server = ApiUtils.getGuild(id);

        try {
            new RequestChannelCommand().deleteRequestChannel(server);
            return ResponseEntity.ok(GeneralResponse.builder()
                    .message("Successfully deleted request channel for " + server.getName() + "!")
                    .build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (InsufficientPermissionException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "I don't have the required permissions to delete a request channel", e);
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

    public ResponseEntity<GeneralResponse> toggleButtons(ToggleRequestChannelButtonsDto toggleRequestChannelButtonDto) {
        final var server = ApiUtils.getGuild(toggleRequestChannelButtonDto.getServer_id());
        final var config = new RequestChannelConfig(server);

        if (!config.isChannelSet())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The request channel hasn't been setup for " + server.getName());

        Arrays.stream(toggleRequestChannelButtonDto.getButtons())
                .forEach(button -> new RequestChannelEditCommand().handleChannelButtonToggle(server, button, null));
        return ResponseEntity.ok(GeneralResponse.builder()
                .message("Successfully toggled the " + Arrays.toString(toggleRequestChannelButtonDto.getButtons()) + " buttons in " + server.getName())
                .build()
        );
    }
}
