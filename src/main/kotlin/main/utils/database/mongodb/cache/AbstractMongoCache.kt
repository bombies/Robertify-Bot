package main.utils.database.mongodb.cache

import com.mongodb.client.MongoCollection
import main.utils.database.mongodb.AbstractMongoDatabase
import main.utils.database.mongodb.databases.GuildDB
import main.utils.json.AbstractJSON
import main.utils.json.GenericJSONField
import org.bson.Document
import org.json.JSONArray
import org.json.JSONObject

abstract class AbstractMongoCache protected constructor(val mongoDB: AbstractMongoDatabase) : AbstractJSON, AbstractMongoDatabase(mongoDB) {
    private var cache: JSONObject?

    init {
        cache = JSONObject().put("documents", JSONArray())
    }

    override fun init() = mongoDB.init()

    fun addToCache(document: Document) {
        addDocument(document)
        updateCache();
    }

    fun updateCache() {
        cache = collectionToJSON(collection)
    }

    fun updateCache(collection: MongoCollection<Document>) {
        this.collection = collection
        cache = collectionToJSON(collection)
    }

    private fun documentUpdate(document: Document) {
        val id = document.getObjectId("_id")
        val collectionArr = cache?.getJSONArray(CacheField.DOCUMENTS.toString())

        checkNotNull(collectionArr) { "The cache has not been initialized yet!" }

        try {
            collectionArr.remove(getIndexOfObjectInArray(collectionArr, id))
        } catch (ignored: IllegalStateException) {}

        collectionArr.put(JSONObject(document.toJson()))
    }

    fun updateCache(document: Document) {
        documentUpdate(document)
        upsertDocument(doc = document)
    }

    fun updateCache(documents: List<Document>) {
        documents.forEach { documentUpdate(it) }
        upsertManyDocuments(documents)
    }

    fun updateGuild(obj: JSONObject, gid: Long? = null) = when (gid) {
        null -> updateCache(obj, GuildDB.Field.GUILD_ID, obj.getLong(GuildDB.Field.GUILD_ID.toString()))
        else -> updateCache(obj, GuildDB.Field.GUILD_ID, gid)
    }

    fun <T> updateCache(obj: JSONObject, identifier: GenericJSONField, value: T) {
        updateCache(obj, identifier.toString(), value)
    }

    fun <T> updateCache(obj: JSONObject, identifier: String, value: T) {
        require(obj.has(identifier)) { "The JSON object must have the \"$identifier\" identifier!" }
        val document = findSpecificDocument(identifier, value)
        checkNotNull(document) { "There was no document found with the \"$identifier\" identifier!" }
        updateCache(Document.parse(obj.toString()))
    }

    private fun collectionToJSON(collection: MongoCollection<Document>): JSONObject {
        val collectionObj = JSONObject()
        val documentArr = JSONArray()

        collection.find().forEach { documentArr.put(JSONObject(it.toJson())) }
        collectionObj.put(CacheField.DOCUMENTS.toString(), documentArr)
        return collectionObj
    }

    fun getCache(): JSONArray  {
        val c = cache
        requireNotNull(c) { " The cache hasn't been initialized yet! " }
        return c.getJSONArray(CacheField.DOCUMENTS.toString())
    }

    enum class CacheField(private val str: String): GenericJSONField {
        DOCUMENTS("documents"),
        GUILD_ID("guild_id"),
        ID("_id");

        override fun toString(): String = str
    }
}