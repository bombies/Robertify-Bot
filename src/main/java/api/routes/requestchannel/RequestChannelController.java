package api.routes.requestchannel;

import api.routes.requestchannel.dto.CreateRequestChannelDto;
import api.routes.requestchannel.dto.ToggleRequestChannelButtonDto;
import api.routes.requestchannel.dto.ToggleRequestChannelButtonsDto;
import api.utils.GeneralResponse;
import api.utils.ReqChannelCreationResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reqchannel")
@AllArgsConstructor
public class RequestChannelController {

    private final RequestChannelService service;

    @PostMapping("")
    public ResponseEntity<ReqChannelCreationResponse> createChannel(@Valid @RequestBody CreateRequestChannelDto createRequestChannelDto) {
        return service.createChannel(createRequestChannelDto);
    }

    @PostMapping("/button")
    public ResponseEntity<GeneralResponse> toggleButtonVisibility(@Valid @RequestBody ToggleRequestChannelButtonDto toggleRequestChannelButtonDto) {
        return service.toggleButton(toggleRequestChannelButtonDto);
    }

    @PostMapping("/buttons")
    public ResponseEntity<GeneralResponse> toggleButtonsVisibility(@Valid @RequestBody ToggleRequestChannelButtonsDto toggleRequestChannelButtonDto) {
        return service.toggleButtons(toggleRequestChannelButtonDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GeneralResponse> deleteChannel(@PathVariable String id) {
        return service.deleteChannel(id);
    }
}
