package api.routes.hello;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.main.Robertify;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    final Logger logger = LoggerFactory.getLogger(HelloController.class);


    @GetMapping("/hello")
    String getHello() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(new HelloModel("John"));
    }

    @PostMapping("/discordTest")
    String sendTestMessage() {
        final var bot = Robertify.getShardManager();
        if (bot == null)
            throw new NullPointerException("The bot hasn't been setup yet.");

        logger.info("Starting execution");

        final var message = bot.getGuildById(304828928223084546L)
                .getTextChannelById(842795162513965066L)
                .sendMessage("This is sent from the API.")
                .complete();

        return new JSONObject()
                .put("success", true)
                .put("message_sent", message.getContentRaw())
                .toString();
    }
}