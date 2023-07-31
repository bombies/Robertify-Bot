package main.utils.json

import main.utils.database.mongodb.cache.redis.GuildRedisCache
import main.utils.database.mongodb.databases.GuildDB
import net.dv8tion.jda.api.entities.Guild
import org.json.JSONObject
import org.slf4j.LoggerFactory

abstract class AbstractGuildConfig protected constructor(private val guild: Guild) {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
        @JvmStatic
        protected val cache: GuildRedisCache by lazy { GuildRedisCache.ins }

        fun initCache() {
            logger.debug("Instantiating Abstract Guild cache")
            cache
        }
    }


    abstract fun update()

    fun getGuildObject(): JSONObject {
        if (!guildHasInfo())
            loadGuild()
        return cache.getGuildInfo(guild.idLong)!!
    }

    fun guildHasInfo(): Boolean = cache.guildHasInfo(guild.idLong)

    fun loadGuild() = cache.loadGuild(guild.idLong)

    fun unloadGuild() = cache.unloadGuild(guild.idLong)

    fun getDatabase(): GuildDB = cache.mongoDB as GuildDB
}