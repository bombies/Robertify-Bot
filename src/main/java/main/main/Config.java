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
        return dotenv.get(key.toString().toUpperCase(), "");
    }

    /**
     * Get a string value from its specific key from the .env file
     * @param key .env key to retrieve.
     * @param defaultValue The value to return if the key specified doesn't have a value.
     * @return The string attached to the key
     */
    public static String get(ENV key, String defaultValue) {
        return dotenv.get(key.toString().toUpperCase(), defaultValue);
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

    public static int getShardCount() {
        return getInt(ENV.SHARD_COUNT);
    }

    public static String getBotToken() {
        return get(ENV.BOT_TOKEN);
    }

    public static long getOwnerID() {
        return getLong(ENV.OWNER_ID);
    }

    public static boolean isPremiumBot() {
        return getBoolean(ENV.PREMIUM_BOT);
    }

    public static boolean loadCommands() {
        return getBoolean(ENV.LOAD_COMMANDS);
    }

    public static boolean loadNeededCommands() {
        return getBoolean(ENV.LOAD_NEEDED_COMMANDS);
    }

    public static String getGatewayUrl() {
        return get(ENV.GATEWAY_URL);
    }

    public static boolean hasGatewayUrl() {
        return hasValue(ENV.GATEWAY_URL);
    }

    public static boolean hasValue(ENV value) {
        final var valueString = get(value);
        return !valueString.isEmpty() && !valueString.isBlank();
    }

    public static boolean isYoutubeEnabled() {
        return hasValue(ENV.YOUTUBE_ENABLED) && getBoolean(ENV.YOUTUBE_ENABLED);
    }

    public static String getEnvironment() {
        return get(ENV.ENVIRONMENT);
    }

    public static String getSentryEnvironment() {
        switch (getEnvironment().toLowerCase()) {
            case "dev", "development" -> { return "development"; }
            case "prod", "production" -> { return "production"; }
            case "staging" -> { return "staging"; }
            default -> throw new IllegalArgumentException("\"" + getEnvironment() + "\" isn't a valid environment!");
        }
    }

    public static boolean isProdEnv() {
        return getEnvironment().equalsIgnoreCase("prod");
    }

    public static boolean isStagingEnv() {
        return getEnvironment().equalsIgnoreCase("staging");
    }

    public static boolean isDevEnv() {
        return getEnvironment().equalsIgnoreCase("dev");
    }

    public static String[] getProviders() {
        final var config = getConfigJSON();
        final var providers = config.getJSONArray("providers");

        return providers.toList()
                .stream()
                .map(Object::toString)
                .toArray(String[]::new);
    }

    public static int getInt(ENV key) {
        return Integer.parseInt(get(key, "-1"));
    }

    public static long getLong(ENV key) {
        return Long.parseLong(get(key, "-1"));
    }

    public static boolean getBoolean(ENV key) {
        return Boolean.parseBoolean(get(key, "false"));
    }

    @SneakyThrows
    public static List<LavaNode> getLavaNodes() {
        try {
            final List<LavaNode> ret = new ArrayList<>();
            JSONObject jsonObject = getConfigJSON();

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
            return new ArrayList<>();
        }
    }

    @SneakyThrows
    private static JSONObject getConfigJSON() {
        File file = new File("./json/config.json");
        if (!file.exists()) {
            file.createNewFile();
            logger.warn("config.json didn't exist, so I created one.");
        }
        return new JSONObject(GeneralUtils.getFileContent(file.getPath()));
    }
}