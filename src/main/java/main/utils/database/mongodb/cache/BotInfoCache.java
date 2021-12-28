package main.utils.database.mongodb.cache;

import lombok.Getter;
import main.utils.database.mongodb.BotInfoDB;
import main.utils.database.mongodb.GuildsDB;
import main.utils.json.legacy.suggestions.SuggestionsConfigField;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class BotInfoCache extends AbstractMongoCache {
    private final static Logger logger = LoggerFactory.getLogger(TestMongoCache.class);
    @Getter
    private static BotInfoCache instance;

    private BotInfoCache() {
        super(BotInfoDB.ins());
        this.init();
    }

    public void setLastStartup(long time) {
        JSONObject jsonObject = getDocument();
        jsonObject.put(BotInfoDB.Fields.LAST_BOOTED.toString(), time);
        update(jsonObject);
    }

    public long getLastStartup() {
        return getDocument().getLong(BotInfoDB.Fields.LAST_BOOTED.toString());
    }

    public void initSuggestionChannels(long categoryID, long pendingChannel, long acceptedChanel, long deniedChannel) {
        if (isSuggestionsSetup())
            throw new IllegalStateException("The channels have already been setup!");

        final var obj = getDocument();
        final var suggestionObj = obj.getJSONObject(BotInfoDB.Fields.SUGGESTIONS_OBJECT.toString());

        suggestionObj.put(SuggestionsConfigField.SUGGESTIONS_CATEGORY.toString(), categoryID);
        suggestionObj.put(SuggestionsConfigField.PENDING_CHANNEL.toString(), pendingChannel);
        suggestionObj.put(SuggestionsConfigField.ACCEPTED_CHANNEL.toString(), acceptedChanel);
        suggestionObj.put(SuggestionsConfigField.DENIED_CHANNEL.toString(), deniedChannel);

        update(obj);
    }

    public void resetSuggestionsConfig() {
        final var obj = getDocument();
        final var suggestionObj = obj.getJSONObject(BotInfoDB.Fields.SUGGESTIONS_OBJECT.toString());

        suggestionObj.put(SuggestionsConfigField.SUGGESTIONS_CATEGORY.toString(), -1L);
        suggestionObj.put(SuggestionsConfigField.PENDING_CHANNEL.toString(), -1L);
        suggestionObj.put(SuggestionsConfigField.ACCEPTED_CHANNEL.toString(), -1L);
        suggestionObj.put(SuggestionsConfigField.DENIED_CHANNEL.toString(), -1L);

        update(obj);
    }

    public long getSuggestionsCategoryID() {
        return getDocument().getJSONObject(BotInfoDB.Fields.SUGGESTIONS_OBJECT.toString())
                .getLong(SuggestionsConfigField.SUGGESTIONS_CATEGORY.toString());
    }

    public long getSuggestionsPendingChannelID() {
        return getDocument().getJSONObject(BotInfoDB.Fields.SUGGESTIONS_OBJECT.toString())
                .getLong(SuggestionsConfigField.PENDING_CHANNEL.toString());
    }

    public long getSuggestionsAcceptedChannelID() {
        return getDocument().getJSONObject(BotInfoDB.Fields.SUGGESTIONS_OBJECT.toString())
                .getLong(SuggestionsConfigField.ACCEPTED_CHANNEL.toString());
    }

    public long getSuggestionsDeniedChannelID() {
        return getDocument().getJSONObject(BotInfoDB.Fields.SUGGESTIONS_OBJECT.toString())
                .getLong(SuggestionsConfigField.DENIED_CHANNEL.toString());
    }

    public void banSuggestionsUser(long id) {
        if (userIsSuggestionBanned(id))
            throw new IllegalStateException("This user is already banned!");

        final var obj = getDocument();
        obj.getJSONObject(BotInfoDB.Fields.SUGGESTIONS_OBJECT.toString())
                .getJSONArray(SuggestionsConfigField.BANNED_USERS.toString()).put(id);

        update(obj);
    }

    public void unbanSuggestionUser(long id) {
        if (!userIsSuggestionBanned(id))
            throw new IllegalStateException("This user is not banned!");

        final var obj = getDocument();
        JSONArray jsonArray = obj.getJSONObject(BotInfoDB.Fields.SUGGESTIONS_OBJECT.toString())
                .getJSONArray(SuggestionsConfigField.BANNED_USERS.toString());
        jsonArray.remove(getIndexOfObjectInArray(jsonArray, id));

        update(obj);
    }

    public List<Long> getBannedSuggestionUsers() {
        final List<Long> ret = new ArrayList<>();
        final var arr = getDocument().getJSONObject(BotInfoDB.Fields.SUGGESTIONS_OBJECT.toString())
                .getJSONArray(SuggestionsConfigField.BANNED_USERS.toString());

        for (int i = 0; i < arr.length(); i++)
            ret.add(arr.getLong(i));

        return ret;
    }

    public boolean userIsSuggestionBanned(long id) {
        JSONArray array = getDocument().getJSONObject(BotInfoDB.Fields.SUGGESTIONS_OBJECT.toString())
                .getJSONArray(SuggestionsConfigField.BANNED_USERS.toString());
        return arrayHasObject(array, id);
    }

    public boolean isSuggestionsSetup() {
        return getDocument().getJSONObject(BotInfoDB.Fields.SUGGESTIONS_OBJECT.toString())
                .getLong(SuggestionsConfigField.SUGGESTIONS_CATEGORY.toString()) != -1L;
    }

    private JSONObject getDocument() {
        return getCache().getJSONObject(0);
    }

    private void update(JSONObject jsonObject) {
        updateCache(jsonObject, "identifier", "robertify_main_config");
    }

    public static void initCache() {
        instance = new BotInfoCache();
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
}
