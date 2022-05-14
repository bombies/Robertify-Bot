package main.utils.database.mongodb.cache.redis;

import lombok.Getter;
import main.constants.ENV;
import main.main.Config;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPooled;

public class RedisDB {

    private static RedisDB instance;
    @Getter
    private final JedisPooled jedis;

    private RedisDB() {
        this.jedis = new JedisPooled(
                Config.get(ENV.REDIS_HOSTNAME),
                Config.getInt(ENV.REDIS_PORT),
                null,
                Config.get(ENV.REDIS_PASSWORD)
        );
    }

    public static RedisDB getInstance() {
        if (instance == null)
            instance = new RedisDB();
        return instance;
    }
}
