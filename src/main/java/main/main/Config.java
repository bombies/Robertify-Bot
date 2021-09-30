package main.main;

import io.github.cdimascio.dotenv.Dotenv;
import main.constants.ENV;
import main.constants.JSONConfigFile;

import java.nio.file.Path;

public class Config {
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
}
