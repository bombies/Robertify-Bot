package main.utils.database.mongodb.cache.redis;

import lombok.Getter;
import main.constants.ENV;
import main.main.Config;
import redis.clients.jedis.JedisPooled;

public class RedisDB {

    private static RedisDB instance;
    @Getter
    private final JedisPooled jedis;

    private RedisDB() {
        if (!Config.get(ENV.REDIS_PASSWORD).isEmpty() && !Config.get(ENV.REDIS_PASSWORD).isBlank())
            this.jedis = new JedisPooled(
                    Config.get(ENV.REDIS_HOSTNAME),
                    Config.getInt(ENV.REDIS_PORT),
                    null,
                    Config.get(ENV.REDIS_PASSWORD)
            );
        else
            this.jedis = new JedisPooled(
                    Config.get(ENV.REDIS_HOSTNAME),
                    Config.getInt(ENV.REDIS_PORT)
            );
    }

    public static RedisDB getInstance() {
        if (instance == null)
            instance = new RedisDB();
        return instance;
    }
}
