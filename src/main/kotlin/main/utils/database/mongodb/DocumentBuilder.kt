package main.utils.database.mongodb

import main.utils.component.InvalidBuilderException
import main.utils.json.GenericJSONField
import org.bson.Document
import org.json.JSONObject

class DocumentBuilder private constructor() {
    private var obj: JSONObject = JSONObject()

    companion object {
        fun create(): DocumentBuilder = DocumentBuilder()
    }

    fun <T> addField(key: String, value: T): DocumentBuilder {
        obj.put(key, value)
        return this
    }

    fun <T> addField(key: GenericJSONField, value: T): DocumentBuilder {
        obj.put(key.toString(), value)
        return this
    }

    fun setObj(obj: JSONObject): DocumentBuilder {
        this.obj = obj
        return this
    }

    fun build(): Document = when {
        obj.isEmpty ->
            throw InvalidBuilderException("You cannot create a document with no fields!")
        else -> Document.parse(obj.toString())
    }

}