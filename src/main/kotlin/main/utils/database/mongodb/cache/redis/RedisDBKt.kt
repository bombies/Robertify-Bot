package main.utils.database.mongodb.cache.redis

import main.constants.ENVKt
import main.main.ConfigKt
import redis.clients.jedis.JedisPooled

class RedisDBKt private constructor() {

    companion object {
        var instance: RedisDBKt? = null
            get() {
                if (field == null)
                    field = RedisDBKt()
                return field
            }
            private set
    }

    val jedis: JedisPooled = if (ConfigKt.get(ENVKt.REDIS_PASSWORD).isNotBlank())
        JedisPooled(
            ConfigKt.get(ENVKt.REDIS_HOSTNAME),
            ConfigKt.getInt(ENVKt.REDIS_PORT),
            null,
            ConfigKt.get(ENVKt.REDIS_PASSWORD)
        )
    else JedisPooled(
        ConfigKt.get(ENVKt.REDIS_HOSTNAME),
        ConfigKt.getInt(ENVKt.REDIS_PORT),
    )


}