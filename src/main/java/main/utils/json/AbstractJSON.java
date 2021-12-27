package main.utils.json;

import org.json.JSONArray;

public interface AbstractJSON {

    /**
     * Check if a JSON Array has a specific object
     * @param array JSON Array to be searched
     * @param object Object to search for
     * @return True if the object is found, else vice versa.
     */
    default boolean arrayHasObject(JSONArray array, Object object) {
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
    default boolean arrayHasObject(JSONArray array, GenericJSONField field, Object object) {
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
    default int getIndexOfObjectInArray(JSONArray array, Object object) {
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
    default int getIndexOfObjectInArray(JSONArray array, GenericJSONField field, Object object) {
        if (!arrayHasObject(array, field, object))
            throw new NullPointerException("There was no such object found in the array!");

        for (int i = 0; i < array.length(); i++)
            if (array.getJSONObject(i).get(field.toString()).equals(object))
                return i;
        return -1;
    }
}
