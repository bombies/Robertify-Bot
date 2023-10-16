package main.utils.json

import main.utils.database.mongodb.cache.redis.guild.GuildDatabaseModel
import main.utils.database.mongodb.cache.redis.guild.GuildRedisCache
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

    protected fun getGuildModel(): GuildDatabaseModel {
        return cache.getGuildModel(guild.id)!!
    }

    protected fun guildHasInfo(): Boolean = cache.guildHasInfo(guild.idLong)

    protected fun loadGuild() = cache.loadGuild(guild.idLong)

    protected fun unloadGuild() = cache.unloadGuild(guild.idLong)

    protected fun getDatabase(): GuildDB = cache.mongoDB as GuildDB
}