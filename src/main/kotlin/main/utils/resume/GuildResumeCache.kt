package main.utils.resume

import main.utils.database.mongodb.cache.redis.RedisCache
import org.slf4j.LoggerFactory

class GuildResumeCache(guildId: String) : RedisCache("resume:$guildId") {

    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
    }

    var data: ResumeData?
        get() {
            val cachedData = get() ?: return null
            val tracks = ResumeData.fromJSON(cachedData)
            if (del() == 0L)
                logger.warn("Attempted to delete tracks to resume after load but nothing was deleted! Cache ID: $cacheID")
            return tracks
        }
        set(value) {
            set(value.toString())
        }

    val hasTracks: Boolean = exists()
}