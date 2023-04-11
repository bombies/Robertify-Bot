package main.utils.database.mongodb.cache.redis

import com.mongodb.client.MongoCollection
import main.utils.database.mongodb.AbstractMongoDatabaseKt
import main.utils.json.GenericJSONFieldKt
import org.bson.Document
import org.json.JSONArray
import org.json.JSONObject
import java.util.function.Consumer

abstract class DatabaseRedisCacheKt protected constructor(cacheID: String, val mongoDB: AbstractMongoDatabaseKt) : RedisCacheKt(cacheID) {
    
    val collection = mongoDB.collection
    
    fun init() = mongoDB.init()
    
    fun addToCache(identifier: String, obj: JSONObject) {
        val objectID = mongoDB.addDocument(obj)
        setex(identifier, 3600, obj.put("_id", objectID.value.toString()).toString())
    }


    open fun updateCache(identifier: String, oldDoc: Document, document: Document) {
        del(identifier)
        setex(identifier, 3600, mongoDB.documentToJSON(document))
        mongoDB.upsertDocument(oldDoc, document)
    }


    override fun updateCache(identifier: String, document: Document) {
        del(identifier)
        setex(identifier, 3600, mongoDB.documentToJSON(document))
        mongoDB.upsertDocument(doc = document)
    }


    open fun updateCacheNoDB(identifier: String, document: Document) {
        del(identifier)
        setex(identifier, 3600, mongoDB.documentToJSON(document))
    }


    open fun updateCacheNoDB(identifier: String, json: JSONObject) {
        del(identifier)
        setex(identifier, 3600, json.toString())
    }


    override fun updateCache(identifier: String, `object`: JSONObject) {
        del(identifier)
        setex(identifier, 3600, `object`.toString())
        mongoDB.upsertDocument(`object`)
    }

    override fun updateCacheObjects(objects: HashMap<String, JSONObject>) {
        val documents = HashMap<String, Document>()
        objects.forEach { (key: String, `object`: JSONObject) ->
            documents[key] = Document.parse(`object`.toString())
        }
        updateCache(documents)
    }

    override fun updateCache(documents: HashMap<String, Document>) {
        for ((key, value) in documents) {
            del(key)
            setex(key, 3600, mongoDB.documentToJSON(value))
        }
        mongoDB.upsertManyDocuments(documents.values.stream().toList())
    }

    open fun <T> updateCache(obj: JSONObject, identifier: GenericJSONFieldKt, identifierValue: T) {
        updateCache(obj, identifier.toString(), identifierValue)
    }

    open fun <T> updateCache(obj: JSONObject, identifier: String, identifierValue: T) {
        require(obj.has(identifier)) { "The JSON object must have the identifier passed!" }
        val document = mongoDB.findSpecificDocument(identifier, identifierValue)
            ?: throw NullPointerException("There was no document found with that identifier value!")
        updateCache(identifierValue.toString(), document, Document.parse(obj.toString()))
    }

    override fun getJSON(id: String): JSONObject? {
        return JSONObject(get(id))
    }

    override fun getJSONByGuild(gid: String): JSONObject? {
        return getJSON(gid)
    }

    private fun collectionToJSON(collection: MongoCollection<Document>): JSONObject {
        val collectionObj = JSONObject()
        val documentArr = JSONArray()
        collection.find().forEach(Consumer { doc: Document ->
            documentArr.put(
                JSONObject(doc.toJson())
            )
        })
        collectionObj.put(CacheField.DOCUMENTS.toString(), documentArr)
        return collectionObj
    }

    override fun getCache(id: String): JSONObject {
        return JSONObject(get(id))
    }


    internal enum class CacheField(private val str: String) : GenericJSONFieldKt {
        DOCUMENTS("documents"),
        GUILD_ID("guild_id"),
        ID("_id");

        override fun toString(): String {
            return str
        }
    }


    enum class Fields(private val str: String) : GenericJSONFieldKt {
        DOCUMENTS("documents");

        override fun toString(): String {
            return str
        }
    }

}