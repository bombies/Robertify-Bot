package main.utils.database.mongodb.cache.redis

import main.constants.ENV
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

    val jedis: JedisPooled = if (ConfigKt.get(ENV.REDIS_PASSWORD).isNotBlank())
        JedisPooled(
            ConfigKt.get(ENV.REDIS_HOSTNAME),
            ConfigKt.getInt(ENV.REDIS_PORT),
            null,
            ConfigKt.get(ENV.REDIS_PASSWORD)
        )
    else JedisPooled(
        ConfigKt.get(ENV.REDIS_HOSTNAME),
        ConfigKt.getInt(ENV.REDIS_PORT),
    )


}