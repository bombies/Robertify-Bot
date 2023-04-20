package main.utils.database.mongodb

import main.utils.component.InvalidBuilderExceptionKt
import main.utils.json.GenericJSONFieldKt
import org.bson.Document
import org.json.JSONObject

class DocumentBuilderKt private constructor() {
    private val obj: JSONObject = JSONObject()

    companion object {
        fun create(): DocumentBuilderKt = DocumentBuilderKt()
    }

    fun <T> addField(key: String, value: T): DocumentBuilderKt {
        obj.put(key, value)
        return this
    }

    fun <T> addField(key: GenericJSONFieldKt, value: T): DocumentBuilderKt {
        obj.put(key.toString(), value)
        return this
    }

    fun build(): Document = when {
        obj.isEmpty ->
            throw InvalidBuilderExceptionKt("You cannot create a document with no fields!")
        else -> Document.parse(obj.toString())
    }

}