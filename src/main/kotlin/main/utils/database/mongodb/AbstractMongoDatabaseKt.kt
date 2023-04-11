package main.utils.database.mongodb

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.*
import main.constants.database.MongoDatabaseKt
import main.utils.json.GenericJSONField
import org.bson.BsonArray
import org.bson.BsonObjectId
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.json.JsonWriterSettings
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory

abstract class AbstractMongoDatabaseKt(db: MongoDatabaseKt, collection: MongoDatabaseKt) {
    companion object {
        private val log = LoggerFactory.getLogger(Companion::class.java)
    }

    private val database: MongoDatabase
    private var collection: MongoCollection<Document>

    init {
        database = MongoConnectionManagerKt.connect(db).database()

        try {
            this.collection = database.getCollection(collection.toString())
        } catch (e: IllegalArgumentException) {
            database.createCollection(collection.toString())
            log.info("Created a new collection with name \"${collection}\" in $db")
            this.collection = database.getCollection(collection.toString())
        }
    }

    abstract fun init()

    fun initAllCaches() {
        //TODO: Implement cache initializations
    }

    fun updateAllCaches() {

    }

    fun addDocument(doc: Document): BsonObjectId =
        collection.insertOne(doc).insertedId?.asObjectId()
            ?: throw NullPointerException("The document could not be inserted due to a null problem!")

    fun addDocument(obj: JSONObject): BsonObjectId = addDocument(Document.parse(obj.toString()))
    
    fun addManyDocument(docs: Collection<Document>) = collection.insertMany(docs.toMutableList())
    
    fun upsertManyDocuments(docs: Collection<Document>) {
        val bulkWriteModels = ArrayList<WriteModel<out Document>>()
        docs.forEach { doc ->
            val id = doc.getObjectId("_id")
            var oldDoc: Document? = null
            
            collection.find().forEach { document -> 
                if (document.getObjectId("_id").equals(id)) 
                    oldDoc = document
            }
            
            if (oldDoc != null)
                bulkWriteModels.add(DeleteOneModel(oldDoc as Bson))
            bulkWriteModels.add(InsertOneModel(doc))
        }
        
        collection.bulkWrite(bulkWriteModels)
    }
    
    fun upsertDocument(oldDoc: Document? = null, doc: Document) {
        when (oldDoc) {
            null -> {
                val id = doc.getObjectId("_id")
                when (val oldDocument = collection.find(Filters.eq("_id", id)).first()) {
                    null -> throw NullPointerException("There is no document to be updated!")
                    else -> {
                        val updatedArr = doc.keys.map { key -> Updates.set(key, doc[key]) }
                        val updates = Updates.combine(updatedArr)
                        collection.updateOne(oldDocument, updates, UpdateOptions().upsert(true))
                    }
                }
            }
            else -> {
                collection.deleteOne(oldDoc)
                collection.insertOne(doc)
            }
        }
    }
    
    fun upsertDocument(obj: JSONObject) = upsertDocument(doc = Document.parse(obj.toString()))
    
    protected fun removeDocument(doc: Document) = collection.deleteOne(doc)

    protected fun removeDocument(key: String, value: String) = when {
        !documentExists(key, value) -> throw NullPointerException("There is no such document found with key $key")
        else -> {
            val iterator = findDocument(key, value)
            while (iterator.hasNext())
                removeDocument(iterator.next())
        }
    }

    protected fun removeSpecificDocument(key: String, value: Any) = when {
        !documentExists(key, value) -> throw NullPointerException("There is no such document with key: $key")
        else -> removeDocument(findDocument(key, value).next())
    }

    protected fun updateDocument(document: Document, updates: Bson) {
        val options = UpdateOptions().upsert(true)
        collection.updateOne(document, updates, options)
    }

    protected open fun updateDocument(document: Document, updates: List<Bson>) {
        val options = UpdateOptions().upsert(true)
        collection.updateOne(document, updates, options)
    }

    protected open fun <T> updateDocument(document: Document, key: String, newValue: T) {
        val updates = Updates.combine(
            Updates.set(key, newValue),
            Updates.currentTimestamp("lastUpdated")
        )
        updateDocument(document, updates)
    }

    protected open fun updateDocument(document: Document, json: JSONObject) {
        val updates: MutableList<Bson> = java.util.ArrayList()
        for (key in json.keySet()) {
            if (key == "_id") continue
            val value = json[key]
            if (value is JSONObject) {
                updates.add(
                    Updates.combine(
                        Updates.set(key, Document.parse(value.toString()))
                    )
                )
                continue
            } else if (value is JSONArray) {
                updates.add(
                    Updates.combine(
                        Updates.set(key, BsonArray.parse(value.toString()))
                    )
                )
                continue
            }
            updates.add(
                Updates.combine(
                    Updates.set(key, json[key])
                )
            )
        }
        updateDocument(document, updates)
    }

    protected open fun <T> updateDocument(idName: String, idValue: String, key: String, newValue: T) {
        val document = findSpecificDocument(idName, idValue)
            ?: throw NullPointerException("There is no such document with mapping <$idName:$idValue>")
        updateDocument(document, key, newValue)
    }
    
    protected fun documentExists(key: String, value: String): Boolean =
        findDocument(key, value).hasNext()
    
    protected fun documentExists(key: String, value: Any): Boolean =
        findDocument(key, value).hasNext()
    
    protected fun findDocument(key: String, value: String): Iterator<Document> =
        collection.find(Filters.eq(key, value)).iterator()

    protected open fun findDocument(key: String, value: Any): Iterator<Document> {
        return collection.find(Filters.eq(key, value)).iterator()
    }

    protected open fun findSpecificDocument(key: String, value: String): Document? {
        return collection.find(Filters.eq(key, value)).iterator().next()
    }

    protected open fun findSpecificDocument(key: String, value: Document): Document? {
        return collection.find(Filters.eq(key, value)).iterator().next()
    }

    protected open fun findSpecificDocument(key: String, value: JSONObject): Document? {
        return collection.find(Filters.eq(key, value)).iterator().next()
    }

    open fun findSpecificDocument(key: String, value: Any): Document? {
        return collection.find(Filters.eq(key, value)).iterator().next()
    }

    protected open fun findSpecificDocument(key: GenericJSONField, value: Any): Document? {
        return findSpecificDocument(key.toString(), value)
    }

    protected open fun findDocument(key: String, value: Document): Iterator<Document> {
        return collection.find(Filters.eq(key, value)).iterator()
    }

    protected open fun findDocument(key: String, value: JSONObject): Iterator<Document> {
        return findDocument(key, Document.parse(value.toString()))
    }

    protected open fun <T> getDocument(key: String, value: T, indented: Boolean): String {
        val doc: Document = when (value) {
            is String -> findSpecificDocument(key, value)
            is Document -> findSpecificDocument(key, value)
            is JSONObject -> findSpecificDocument(key, value)
            is Long -> findSpecificDocument(key, value)
            is Int -> findSpecificDocument(key, value)
            else -> throw IllegalArgumentException("Invalid value type!")
        } ?: throw NullPointerException("There is no such document that matches the key: $key")

        return documentToJSON(doc, indented)
    }

    protected open fun <T> getDocuments(key: String, value: T): String {
        return getDocuments(key, value, false) ?: throw NullPointerException("Couldn't find any documents where they key ($key) matches the value (${value.toString()})")
    }

    protected open fun <T> getDocuments(key: String, value: T, indented: Boolean): String? {
        val doc: Iterator<Document> = when (value) {
            is String -> findDocument(key, value)
            is Document -> findDocument(key, value)
            is JSONObject -> findDocument(key, value)
            else -> throw IllegalArgumentException("Invalid value type!")
        }
        val sb = StringBuilder()
        while (doc.hasNext()) sb.append(documentToJSON(doc.next(), indented)).append("\n")
        return sb.toString()
    }

    protected open fun documentToJSON(doc: Document, indented: Boolean): String {
        return doc.toJson(
            JsonWriterSettings.builder()
                .indent(indented)
                .build()
        )
    }

    open fun documentToJSON(doc: Document): String {
        return documentToJSON(doc, false)
    }

    protected open fun <TItem> eq(key: String, value: TItem): Bson {
        return Filters.eq(key, value)
    }

    @SafeVarargs
    protected fun <TItem> `in`(key: String, vararg values: TItem): Bson {
        return Filters.`in`(key, *values)
    }
}