package main.utils.database.mongodb.cache.redis

import com.mongodb.client.MongoCollection
import kotlinx.coroutines.coroutineScope
import main.main.Config
import org.bson.Document
import org.json.JSONArray
import org.json.JSONObject
import java.util.function.Consumer

abstract class RedisCache protected constructor(cacheID: String) {

    companion object {
        private val jedis = RedisDB.jedis
    }

    protected val cacheID = "$cacheID#${Config.MONGO_DATABASE_NAME}#"

    protected suspend fun hsetJSON(identifier: String, hash: HashMap<String, JSONObject>) = coroutineScope {
        val newHash = HashMap<String, String>()
        hash.forEach { (key: String, `val`: JSONObject) ->
            newHash[key] = `val`.toString()
        }

        hset(identifier, newHash)
    }

    protected suspend fun hset(identifier: String, hash: HashMap<String, String>) = coroutineScope {
        jedis.hset(cacheID + identifier, hash)
    }

    protected suspend fun hset(hash: HashMap<String, String>) {
        hset(cacheID, hash)
    }

    protected suspend fun hget(identifier: String, key: String): String? = coroutineScope {
        return@coroutineScope jedis.hget(cacheID + identifier, key)
    }

    protected suspend fun hgetJSON(identifier: String, key: String): JSONObject {
        return JSONObject(hget(identifier, key))
    }

    protected suspend fun hgetAll(key: String): Map<String, String> = coroutineScope {
        return@coroutineScope jedis.hgetAll(key)
    }

    protected suspend fun setex(identifier: String, seconds: Long, value: String): String = coroutineScope {
        jedis.setex(cacheID + identifier, seconds, value)
    }

    protected open suspend fun set(identifier: String, value: String) {
        jedis.set(cacheID + identifier, value)
    }

    protected suspend fun set(value: String): String = coroutineScope {
        jedis.set(cacheID, value)
    }

    protected suspend fun setex(identifier: String, seconds: Long, value: JSONObject) {
        setex(identifier, seconds, value.toString())
    }

    protected suspend fun setex(identifier: Long, seconds: Long, value: JSONObject) {
        setex(identifier.toString(), seconds, value.toString())
    }

    protected open suspend fun get(identifier: String): String? = coroutineScope {
        val item = jedis.get(cacheID + identifier)
        return@coroutineScope item.ifBlank { null }
    }

    protected open suspend fun get(identifier: Long): String? {
        return get(identifier.toString())
    }

    protected suspend fun get(): String? = coroutineScope {
        return@coroutineScope jedis.get(cacheID)
    }

    protected suspend fun del(identifier: String): Long = coroutineScope {
        return@coroutineScope jedis.del(cacheID + identifier)
    }

    protected suspend fun del(identifier: Long): Long {
        return del(identifier.toString())
    }

    protected suspend fun del(): Long = coroutineScope {
        return@coroutineScope jedis.del(cacheID)
    }

    protected suspend fun exists(): Boolean = coroutineScope {
        return@coroutineScope jedis.exists(cacheID)
    }

    protected suspend fun exists(identifier: String): Boolean = coroutineScope {
        return@coroutineScope jedis.exists(cacheID + identifier)
    }

    open suspend fun updateCache(identifier: String, document: Document) {
        set(cacheID + identifier, document.toJson())
    }

    suspend fun updateCache(identifier: String, document: Document, expiration: Long) {
        setex(cacheID + identifier, expiration, document.toJson())
    }

    open suspend fun updateCache(identifier: String, `object`: JSONObject) {
        set(cacheID + identifier, `object`.toString())
    }

    suspend fun updateCache(identifier: String, `object`: JSONObject, expiration: Long) {
        setex(cacheID + identifier, expiration, `object`.toString())
    }

    open suspend fun updateCacheObjects(objects: HashMap<String, JSONObject>) {
        val documents = HashMap<String, Document>()
        objects.forEach { (key: String, `object`: JSONObject) ->
            documents[key] = Document.parse(`object`.toString())
        }
        updateCache(documents)
    }

    open suspend fun updateCache(documents: HashMap<String, Document>) {
        for ((key, value) in documents)
            jedis.setex(cacheID + key, 3600, value.toJson())
    }

    suspend fun removeFromCache(id: String) {
        del(id)
    }

    suspend fun getCacheJSON(identifier: String): JSONObject {
        return JSONObject(get(identifier))
    }

    open suspend fun getJSON(id: String): JSONObject? {
        val source = get(id) ?: return null
        return JSONObject(source)
    }

    open suspend fun getJSONByGuild(gid: String): JSONObject? {
        return getJSON(gid)
    }

    suspend fun getJSONByGuild(gid: Long): JSONObject? {
        return getJSON(gid.toString())
    }

    private fun collectionToJSON(collection: MongoCollection<Document>): JSONObject {
        val collectionObj = JSONObject()
        val documentArr = JSONArray()
        collection.find().forEach(Consumer { doc: Document ->
            documentArr.put(
                JSONObject(doc.toJson())
            )
        })
        collectionObj.put(DatabaseRedisCache.CacheField.DOCUMENTS.toString(), documentArr)
        return collectionObj
    }

    open suspend fun getCache(id: String): JSONObject {
        return JSONObject(get(cacheID + id))
    }

}