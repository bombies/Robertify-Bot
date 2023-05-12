package main.utils.database.mongodb.cache.redis

import main.main.Config
import redis.clients.jedis.JedisPooled

object RedisDBKt {

    val jedis: JedisPooled = if (Config.REDIS_PASSWORD.isNotBlank())
        JedisPooled(
            Config.REDIS_HOSTNAME,
            Config.REDIS_PORT.toInt(),
            null,
            Config.REDIS_PASSWORD
        )
    else JedisPooled(
        Config.REDIS_HOSTNAME,
        Config.REDIS_PORT.toInt(),
    )

}