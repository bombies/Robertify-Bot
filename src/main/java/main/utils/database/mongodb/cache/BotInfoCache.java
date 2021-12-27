package main.utils.database.mongodb.cache;

import lombok.Getter;
import main.utils.database.mongodb.BotInfoDB;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BotInfoCache extends AbstractMongoCache {
    private final static Logger logger = LoggerFactory.getLogger(TestMongoCache.class);
    @Getter
    private static BotInfoCache instance;

    private BotInfoCache() {
        super(BotInfoDB.ins());
        this.init();
    }

    public String getJSON(boolean indented) {
        return indented ? getCache().toString(4) : getCache().toString();
    }

    public String getJSON() {
        return getCache().toString(4);
    }

    public JSONObject getJSONObject() {
        return new JSONObject(getJSON(false));
    }

    public void setLastStartup(long time) {
        JSONObject jsonObject = getDocument();
        jsonObject.put(BotInfoDB.Fields.LAST_BOOTED.toString(), time);
        updateCache(jsonObject, "identifier", "robertify_main_config");
    }

    public long getLastStartup() {
        return getDocument().getLong(BotInfoDB.Fields.LAST_BOOTED.toString());
    }

    private JSONObject getDocument() {
        return getCache().getJSONObject(0);
    }

    public static void initCache() {
        instance = new BotInfoCache();
    }
}
