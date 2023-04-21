package main.utils.database.mongodb.cache.redis

import com.mongodb.client.MongoCollection
import main.constants.ENVKt
import main.main.ConfigKt
import main.utils.json.AbstractJSONKt
import org.bson.Document
import org.json.JSONArray
import org.json.JSONObject
import redis.clients.jedis.JedisPooled
import java.util.function.Consumer

abstract class RedisCacheKt protected constructor(cacheID: String) : AbstractJSONKt {
    
    private val jedis: JedisPooled = RedisDBKt.instance?.jedis ?: throw IllegalArgumentException("There was no connection to the Redis database!")
    protected val cacheID = "$cacheID#${ConfigKt.MONGO_DATABASE_NAME}#"

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
        jedis.hset(cacheID, hash)
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

    protected fun setex(identifier: String, seconds: Int, value: String) {
        jedis.setex(cacheID + identifier, seconds.toLong(), value)
    }

    protected open operator fun set(identifier: String, value: String) {
        jedis[cacheID + identifier] = value
    }

    protected fun set(value: String): String {
        return jedis.set(cacheID, value)
    }

    protected fun setex(identifier: String, seconds: Int, value: JSONObject) {
        setex(identifier, seconds, value.toString())
    }

    protected fun setex(identifier: Long, seconds: Int, value: JSONObject) {
        setex(identifier.toString(), seconds, value.toString())
    }

    protected open operator fun get(identifier: String): String? {
        return jedis[cacheID + identifier]
    }

    protected open operator fun get(identifier: Long): String? {
        return get(identifier.toString())
    }

    protected fun get(): String? {
        return jedis[cacheID]
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
        jedis.del(identifier)
        jedis[cacheID + identifier] = document.toJson()
    }

    fun updateCache(identifier: String, document: Document, expiration: Int) {
        jedis.del(identifier)
        jedis.setex(cacheID + identifier, expiration.toLong(), document.toJson())
    }

    open fun updateCache(identifier: String, `object`: JSONObject) {
        jedis.del(identifier)
        jedis[cacheID + identifier] = `object`.toString()
    }

    fun updateCache(identifier: String, `object`: JSONObject, expiration: Int) {
        jedis.del(identifier)
        jedis.setex(cacheID + identifier, expiration.toLong(), `object`.toString())
    }

    open fun updateCacheObjects(objects: HashMap<String, JSONObject>) {
        val documents = HashMap<String, Document>()
        objects.forEach { (key: String, `object`: JSONObject) ->
            documents[key] = Document.parse(`object`.toString())
        }
        updateCache(documents)
    }

    open fun updateCache(documents: HashMap<String, Document>) {
        for ((key, value) in documents) {
            jedis.del(key)
            jedis.setex(cacheID + key, 3600, value.toJson())
        }
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
        collectionObj.put(DatabaseRedisCacheKt.CacheField.DOCUMENTS.toString(), documentArr)
        return collectionObj
    }

    open fun getCache(id: String): JSONObject {
        return JSONObject(get(cacheID + id))
    }
    
}