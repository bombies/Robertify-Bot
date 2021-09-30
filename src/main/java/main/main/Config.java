package main.main;

import io.github.cdimascio.dotenv.Dotenv;
import main.constants.ENV;

public class Config {
    private static Dotenv dotenv = Dotenv.load();

    public static String get(ENV key) {
        return dotenv.get(key.toString().toUpperCase());
    }

    public static void reload() {
        dotenv = Dotenv.load();
    }
}
