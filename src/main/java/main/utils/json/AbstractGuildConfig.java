package main.utils.json;

import lombok.Getter;
import main.utils.database.mongodb.cache.GuildDBCache;
import main.utils.database.mongodb.cache.redis.GuildRedisCache;
import main.utils.database.mongodb.databases.GuildDB;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGuildConfig implements AbstractJSON {
    private final static Logger logger = LoggerFactory.getLogger(AbstractGuildConfig.class);
    @Getter
    private static GuildRedisCache cache;
    private final Guild guild;
    private final long gid;
    protected AbstractGuildConfig(Guild guild) {
        this.guild = guild;
        this.gid = guild.getIdLong();
    }

    public abstract void update();

    public JSONObject getGuildObject() {
        if (!guildHasInfo())
            loadGuild();
        return cache.getGuildInfo(gid);
    }

    public static void initCache() {
        logger.debug("Instantiating Abstract Guild cache");
        cache = GuildRedisCache.getInstance();
    }

    public boolean guildHasInfo() {
        return cache.guildHasInfo(gid);
    }

    public void loadGuild() {
        cache.loadGuild(gid);
    }

    public void unloadGuild() {
        cache.unloadGuild(gid);
    }

    public GuildDB getDatabase() {
        return ((GuildDB) cache.getMongoDB());
    }
}
