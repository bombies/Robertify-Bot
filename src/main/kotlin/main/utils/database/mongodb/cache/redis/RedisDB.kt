package main.utils.database.mongodb.cache.redis

import dev.minn.jda.ktx.util.SLF4J
import main.main.Config
import redis.clients.jedis.JedisPooled

object RedisDB {

    private val logger by SLF4J

    val jedis: JedisPooled = if (Config.REDIS_PASSWORD.isNotBlank()) {
        JedisPooled(
            Config.REDIS_HOSTNAME,
            Config.REDIS_PORT.toInt(),
            null,
            Config.REDIS_PASSWORD
        )
    } else {
        JedisPooled(
            Config.REDIS_HOSTNAME,
            Config.REDIS_PORT.toInt(),
        )
    }

}