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

    private val cacheID = "$cacheID#${Config.MONGO_DATABASE_NAME}#"

    protected fun hsetJSON(identifier: String, hash: HashMap<String, JSONObject>) {
        val newHash = HashMap<String, String>()
        hash.forEach { (key: String, `val`: JSONObject) ->
            newHash[key] = `val`.toString()
        }

        hset(identifier, newHash)
    }

    protected fun hset(identifier: String, hash: HashMap<String, String>) {
        jedis.hset(cacheID + identifier, hash)
    }

    protected fun hset(hash: HashMap<String, String>) {
        hset(cacheID, hash)
    }

    protected fun hget(identifier: String, key: String): String? {
        return jedis.hget(cacheID + identifier, key)
    }

    protected fun hgetJSON(identifier: String, key: String): JSONObject {
        return JSONObject(hget(identifier, key))
    }

    protected fun hgetAll(key: String): Map<String, String> {
        return jedis.hgetAll(key)
    }

    protected fun setex(identifier: String, seconds: Long, value: String): String {
        return jedis.setex(cacheID + identifier, seconds, value)
    }

    protected open fun set(identifier: String, value: String) {
        jedis.set(cacheID + identifier, value)
    }

    protected fun set(value: String): String {
        return jedis.set(cacheID, value)
    }

    protected fun setex(identifier: String, seconds: Long, value: JSONObject) {
        setex(identifier, seconds, value.toString())
    }

    protected fun setex(identifier: Long, seconds: Long, value: JSONObject) {
        setex(identifier.toString(), seconds, value.toString())
    }

    protected open fun get(identifier: String): String? {
        val item = jedis.get(cacheID + identifier)
        return if (item == "nil") null else item
    }

    protected open fun get(identifier: Long): String? {
        return get(identifier.toString())
    }

    protected fun get(): String? {
        return jedis.get(cacheID)
    }

    protected fun del(identifier: String): Long {
        return jedis.del(cacheID + identifier)
    }

    protected fun del(identifier: Long): Long {
        return del(identifier.toString())
    }

    protected fun del(): Long {
        return jedis.del(cacheID)
    }

    protected fun exists(): Boolean {
        return jedis.exists(cacheID)
    }

    protected fun exists(identifier: String): Boolean {
        return jedis.exists(cacheID + identifier)
    }

    open fun updateCache(identifier: String, document: Document) {
        set(cacheID + identifier, document.toJson())
    }

    fun updateCache(identifier: String, document: Document, expiration: Long) {
        setex(cacheID + identifier, expiration, document.toJson())
    }

    open fun updateCache(identifier: String, `object`: JSONObject) {
        set(cacheID + identifier, `object`.toString())
    }

    fun updateCache(identifier: String, `object`: JSONObject, expiration: Long) {
        setex(cacheID + identifier, expiration, `object`.toString())
    }

    open fun updateCacheObjects(objects: HashMap<String, JSONObject>) {
        val documents = HashMap<String, Document>()
        objects.forEach { (key: String, `object`: JSONObject) ->
            documents[key] = Document.parse(`object`.toString())
        }
        updateCache(documents)
    }

    open fun updateCache(documents: HashMap<String, Document>) {
        for ((key, value) in documents)
            jedis.setex(cacheID + key, 3600, value.toJson())
    }

    fun removeFromCache(id: String) {
        del(id)
    }

    fun getCacheJSON(identifier: String): JSONObject {
        return JSONObject(get(identifier))
    }

    open fun getJSON(id: String): JSONObject? {
        val source = get(id) ?: return null
        return JSONObject(source)
    }

    open fun getJSONByGuild(gid: String): JSONObject? {
        return getJSON(gid)
    }

    fun getJSONByGuild(gid: Long): JSONObject? {
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

    open fun getCache(id: String): JSONObject {
        return JSONObject(get(cacheID + id))
    }

}