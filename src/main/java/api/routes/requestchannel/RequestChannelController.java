package api.routes.requestchannel;

import api.routes.requestchannel.dto.CreateRequestChannelDto;
import api.utils.ApiUtils;
import api.utils.GeneralResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import main.commands.slashcommands.commands.management.requestchannel.RequestChannelCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reqchannel")
@AllArgsConstructor
public class RequestChannelController {

    private final RequestChannelService service;

    @PostMapping("")
    public ResponseEntity<GeneralResponse> createChannel(@Valid @RequestBody CreateRequestChannelDto createRequestChannelDto) {
        return service.createChannel(createRequestChannelDto);
    }
}
