package main.utils.json;

import lombok.SneakyThrows;
import main.constants.ENV;
import main.constants.JSONConfigFile;
import main.main.Config;
import main.utils.GeneralUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class JSONConfig {
    /**
     * The JSON file itself
     */
    private final JSONConfigFile file;

    public JSONConfig(JSONConfigFile file) {
        this.file = file;
    }

    /**
     * Initialize JSON config.
     */
    public abstract void initConfig();

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
    public void setJSON(JSONObject object) {
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
     * Check if a JSON Array has a specific object
     * @param array JSON Array to be searched
     * @param object Object to search for
     * @return True if the object is found, else vice versa.
     */
    public boolean arrayHasObject(JSONArray array, Object object) {
        for (int i = 0; i < array.length(); i++)
            if (array.get(i).equals(object))
                return true;
        return false;
    }

    /**
     * Check if a JSON Array has a specific object inside a JSONObject
     * @param array Array to be searched
     * @param field JSON field to search for in each JSONObject
     * @param object Object to be searched for
     * @return True if the object is found, else vice versa.
     */
    public boolean arrayHasObject(JSONArray array, IJSONField field, Object object) {
        for (int i = 0; i < array.length(); i++)
            if (array.getJSONObject(i).get(field.toString()).equals(object))
                return true;
        return false;
    }

    /**
     * Gets the index of a specific object in a JSON array.
     * @param array Array to be searched
     * @param object Object to search for
     * @return Index where the object is found. -1 Will be returned
     * if some unexpected error occurs
     * @throws NullPointerException Thrown when the object couldn't be found in the array
     */
    public int getIndexOfObjectInArray(JSONArray array, Object object) {
        if (!arrayHasObject(array, object))
            throw new NullPointerException("There was no such object found in the array!");

        for (int i = 0; i < array.length(); i++)
            if (array.get(i).equals(object))
                return i;
        return -1;
    }

    /**
     * Gets the index of a specific JSONObject where the object to be searched is found.
     * @param array Array to be searched
     * @param field JSON field to be searched for in each JSONObject
     * @param object Object to be searched for/
     * @return Index where the object is found. -1 Will be returned
     *      * if some unexpected error occurs
     * @throws NullPointerException Thrown when the object couldn't be found in the array
     */
    public int getIndexOfObjectInArray(JSONArray array, IJSONField field, Object object) {
        if (!arrayHasObject(array, field, object))
            throw new NullPointerException("There was no such object found in the array!");

        for (int i = 0; i < array.length(); i++)
            if (array.getJSONObject(i).get(field.toString()).equals(object))
                return i;
        return -1;
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
