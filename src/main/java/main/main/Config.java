package main.main;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.SneakyThrows;
import main.constants.ENV;
import main.constants.JSONConfigFile;
import main.utils.GeneralUtils;
import main.utils.lavalink.LavaNode;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Config {
    private final static Logger logger = LoggerFactory.getLogger(Config.class);
    private static Dotenv dotenv = Dotenv.load();

    /**
     * Get a string value from its specific key from the .env file
     * @param key .env key to retrieve.
     * @return The string attached to the key
     */
    public static String get(ENV key) {
        return dotenv.get(key.toString().toUpperCase());
    }

    /**
     * Get a specific path from the .env file
     * @param dir The directory of the path
     * @param file The specific file to get in the directory
     * @return The path of the key
     */
    public static Path getPath(ENV dir, JSONConfigFile file) {
        return Path.of(dotenv.get(dir.toString().toUpperCase()) + "/" + file.toString());
    }

    /**
     * Reload the .env file to use the new values if it was updated after compilation and execution
     */
    public static void reload() {
        dotenv = Dotenv.load();
    }

    @SneakyThrows
    public static List<LavaNode> getLavaNodes() {
        try {
            final List<LavaNode> ret = new ArrayList<>();
            File file = new File("./json/config.json");

            JSONObject jsonObject = new JSONObject(GeneralUtils.getFileContent(file.getPath()));

            for (final var obj : jsonObject.getJSONArray("nodes")) {
                final var actualObj = (JSONObject) obj;
                ret.add(new LavaNode(
                        actualObj.getString("host"),
                        actualObj.getString("port"),
                        actualObj.getString("password")
                ));
            }

            return ret;
        } catch (NullPointerException e) {
            Files.createFile(Path.of("./json/config.json"));
            logger.warn("config.json didn't exist, so I created one.");
            return new ArrayList<>();
        }
    }

    private static class YamlConfig {
        List<LavaNode> nodes;

        public YamlConfig(List<LavaNode> nodes) {
            this.nodes = nodes;
        }
    }
}
