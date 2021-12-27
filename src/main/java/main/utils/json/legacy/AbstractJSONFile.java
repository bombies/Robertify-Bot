package main.utils.json.legacy;

import lombok.Getter;
import lombok.SneakyThrows;
import main.constants.ENV;
import main.constants.JSONConfigFile;
import main.main.Config;
import main.utils.GeneralUtils;
import main.utils.json.AbstractJSON;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class AbstractJSONFile implements AbstractJSON {
    /**
     * The JSON file itself
     */
    @Getter
    private final JSONConfigFile file;

    public AbstractJSONFile(JSONConfigFile file) {
        this.file = file;
    }

    /**
     * Initialize JSON config.
     */
    public abstract void initConfig();

    /**
     * Attempts to make the specified JSON configuration file if it doesn't already exist.
     */
    @SneakyThrows
    public void makeConfigFile() {
        boolean newConfig = false;

        if (!Files.exists(Config.getPath(ENV.JSON_DIR, this.file))) {
            new File(String.valueOf(Config.getPath(ENV.JSON_DIR, this.file)))
                    .createNewFile();
            newConfig = true;
        }

        if (!newConfig) throw new IllegalStateException("File already exists.");
    }

    protected JSONObject getJSONObject() {
        return new JSONObject(getJSON());
    }

    /**
     * Get the JSON config in a string
     * @return Stringified JSON config
     */
    public String getJSON() {
        return GeneralUtils.getFileContent(Config.getPath(ENV.JSON_DIR, file));
    }

    /**
     * Set the file content of a specific JSON configuration file
     * @param object JSONObject containing all the JSON info
     */
    @SneakyThrows
    public synchronized void setJSON(JSONObject object) {
        GeneralUtils.setFileContent(Config.getPath(ENV.JSON_DIR, file), object.toString(4));
    }

    /**
     * Get the path in which the JSON file is location
     * @return Stringified path of JSON file
     */
    public String getPath() {
        return file.toString();
    }

    /**
     * Initialize the directory for all the JSON file if it hasn't already been created.
     */
    @SneakyThrows
    public static void initDirectory() {
        if (!Files.exists(Path.of(Config.get(ENV.JSON_DIR) + "/")))
            Files.createDirectories(Path.of(Config.get(ENV.JSON_DIR)));
    }

}
