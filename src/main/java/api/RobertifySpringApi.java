package api;

import main.constants.ENV;
import main.main.Config;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Collections;

@SpringBootApplication
public class RobertifySpringApi {
    public static void start(String[] args) {
        final var app = new SpringApplication(RobertifySpringApi.class);
        app.setDefaultProperties(Collections.singletonMap(
                "server.port", Config.get(ENV.SPRING_API_PORT, "8080")
        ));
        app.run(args);
    }
}
