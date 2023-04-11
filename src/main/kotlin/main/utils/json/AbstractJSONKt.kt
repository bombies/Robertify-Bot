package main.utils.json

import org.bson.types.ObjectId
import org.json.JSONArray
import org.json.JSONObject

interface AbstractJSONKt {

    /**
     * Check if a [JSONArray] has a specific object
     * @param array [JSONArray] to be searched
     * @param object Object to search for
     * @return True if the object is found, else vice versa.
     */
    fun arrayHasObject(array: JSONArray, `object`: Any): Boolean =
        array.any { it.equals(`object`) }

    /**
     * Check if a [JSONArray] has a specific object inside a [JSONObject]
     * @param array [JSONArray] to be searched
     * @param field [GenericJSONFieldKt] to search for in each [JSONObject]
     * @param object Object to be searched for
     * @return True if the object is found, else vice versa.
     */
    fun arrayHasObject(array: JSONArray, field: GenericJSONFieldKt, `object`: Any) =
        array.any { if (it is JSONObject) it.get(field.toString()).equals(`object`) else false }

    fun arrayHasObject(array: JSONArray, id: ObjectId): Boolean =
        array.any { if (it is JSONObject) it.getJSONObject("_id").getString("\$oid").equals(id.toString()) else false }


    /**
     * Gets the index of a specific object in a JSON array.
     * @param array Array to be searched
     * @param object Object to search for
     * @return Index where the object is found. -1 Will be returned
     * if some unexpected error occurs
     * @throws IllegalStateException Thrown when the object couldn't be found in the array
     */
    fun getIndexOfObjectInArray(array: JSONArray, `object`: Any): Int {
        check(arrayHasObject(array, `object`)) { "There was no such object found in the array!" }
        return array.indexOf(`object`)
    }

    /**
     * Gets the index of a specific JSONObject where the object to be searched is found.
     * @param array Array to be searched
     * @param field JSON field to be searched for in each JSONObject
     * @param object Object to be searched for/
     * @return Index where the object is found. -1 Will be returned
     *          if some unexpected error occurs
     * @throws IllegalStateException when the object couldn't be found in the array
     */
    fun getIndexOfObjectInArray(array: JSONArray, field: GenericJSONFieldKt, `object`: Any): Int {
        check(arrayHasObject(array, field, `object`)) { "There was no such object found in the array!" }
        for (i in 0 until array.length())
            if (array.get(i) is JSONObject && array.getJSONObject(i).get(field.toString()).equals(`object`))
                return i
        return -1
    }

    fun getIndexOfObjectInArray(array: JSONArray, id: ObjectId): Int {
        check(arrayHasObject(array, id)) { "There was no such object found in the array!" }
        for (i in 0 until array.length())
            if (array.get(i) is JSONObject && array.getJSONObject(i).getJSONObject("_id").equals(id.toString()))
                return i
        return -1
    }
}