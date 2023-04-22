package main.utils.database.mongodb.cache.redis

import main.constants.ENVKt
import main.main.ConfigKt
import redis.clients.jedis.JedisPooled

object RedisDBKt {

    val jedis: JedisPooled = if (ConfigKt.REDIS_PASSWORD.isNotBlank())
        JedisPooled(
            ConfigKt.REDIS_HOSTNAME,
            ConfigKt.REDIS_PORT.toInt(),
            null,
            ConfigKt.REDIS_PASSWORD
        )
    else JedisPooled(
        ConfigKt.REDIS_HOSTNAME,
        ConfigKt.REDIS_PORT.toInt(),
    )

}