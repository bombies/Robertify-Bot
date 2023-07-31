package main.utils.json

import org.bson.types.ObjectId
import org.json.JSONArray
import org.json.JSONObject

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
 * @param field [GenericJSONField] to search for in each [JSONObject]
 * @param object Object to be searched for
 * @return True if the object is found, else vice versa.
 */
fun arrayHasObject(array: JSONArray, field: GenericJSONField, `object`: Any) =
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
fun getIndexOfObjectInArray(array: JSONArray, field: GenericJSONField, `object`: Any): Int {
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

fun JSONArray.has(obj: Any): Boolean =
    any { it.equals(obj) }

fun JSONArray.has(field: GenericJSONField, obj: Any) =
    any { if (it is JSONObject) it.get(field.toString()).equals(obj) else false }

fun JSONArray.indexOf(field: GenericJSONField, `object`: Any): Int {
    check(has(field, `object`)) { "There was no such object found in the array!" }
    for (i in 0 until length())
        if (get(i) is JSONObject && getJSONObject(i).get(field.toString()).equals(`object`))
            return i
    return -1
}

fun JSONArray.remove(field: GenericJSONField, value: Any) {
    check(arrayHasObject(this, field, value)) { "There was no such object found in the array!" }

    mapIndexed { index, curVal -> Pair(index, curVal) }
        .filter { curVal ->
            curVal.second is JSONObject && (curVal.second as JSONObject).get(field.toString()).equals(value)
        }
        .forEach {
            remove(it.first)
        }
}

fun JSONArray.remove(obj: Any): Int {
    check(has(obj)) { "There was no such object found in the array!" }
    return indexOf(obj)
}