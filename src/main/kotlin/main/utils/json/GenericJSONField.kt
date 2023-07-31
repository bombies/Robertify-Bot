package main.utils.json

import org.json.JSONObject

interface GenericJSONField

fun <T> JSONObject.put(key: GenericJSONField, value: T): JSONObject =
    put(key.toString(), value)