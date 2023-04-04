package main.utils.resume;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import main.utils.database.mongodb.cache.redis.RedisCache;

@Slf4j
public class GuildResumeCache extends RedisCache {

    public GuildResumeCache(String guildID) {
        super("resume:" + guildID);
    }

    public void setTracks(ResumeData resumeData) {
        set(resumeData.toString());
    }

    public ResumeData loadData() throws JsonProcessingException {
        final var tracks = ResumeData.fromJSON(get());
        if (del() == 0L)
            log.warn("Attempted to delete tracks to resume after load but nothing was deleted! Cache ID: {}", getCacheID());
        return tracks;
    }

    public boolean hasTracks() {
        return exists();
    }
}
