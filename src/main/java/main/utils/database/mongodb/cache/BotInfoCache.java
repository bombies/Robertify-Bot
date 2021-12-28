package main.utils.database.mongodb.cache;

import lombok.Getter;
import main.utils.database.mongodb.BotInfoDB;
import main.utils.database.mongodb.GuildsDB;
import main.utils.json.legacy.reports.ReportsConfigField;
import main.utils.json.legacy.suggestions.SuggestionsConfigField;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class BotInfoCache extends AbstractMongoCache {
    private final static Logger logger = LoggerFactory.getLogger(BotInfoCache.class);
    @Getter
    private static BotInfoCache instance;

    private BotInfoCache() {
        super(BotInfoDB.ins());
        this.init();
        updateCache();
        logger.debug("Done instantiating Bot Info cache");
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

    public void initReportChannels(long categoryID, long channelID) {
        if (isReportsSetup())
            throw new IllegalStateException("The reports category has already been setup!");

        final var obj = getDocument();

        obj.getJSONObject(BotInfoDB.Fields.REPORTS_OBJECT.toString())
                .put(ReportsConfigField.CATEGORY.toString(), categoryID);
        obj.getJSONObject(BotInfoDB.Fields.REPORTS_OBJECT.toString())
                .put(ReportsConfigField.CHANNEL.toString(), channelID);

        update(obj);
    }

    public void resetReportsConfig() {
        final var obj = getDocument();

        obj.getJSONObject(BotInfoDB.Fields.REPORTS_OBJECT.toString())
                .put(ReportsConfigField.CATEGORY.toString(), -1L);
        obj.getJSONObject(BotInfoDB.Fields.REPORTS_OBJECT.toString())
                .put(ReportsConfigField.CHANNEL.toString(), -1L);

        update(obj);
    }

    public boolean isReportsSetup() {
        return getDocument().getJSONObject(BotInfoDB.Fields.REPORTS_OBJECT.toString())
                .getLong(ReportsConfigField.CATEGORY.toString()) != -1L;
    }

    public long getReportsID(ReportsConfigField field) {
        return getDocument().getJSONObject(BotInfoDB.Fields.REPORTS_OBJECT.toString())
                .getLong(field.toString());
    }

    public boolean isUserReportsBanned(long id) {
        return arrayHasObject(getDocument().getJSONObject(BotInfoDB.Fields.REPORTS_OBJECT.toString())
                .getJSONArray(ReportsConfigField.BANNED_USERS.toString()), id);
    }

    public void banReportsUser(long id) {
        if (isUserReportsBanned(id))
            throw new IllegalStateException("This user has already been banned!");

        final var obj = getDocument();

        obj.getJSONObject(BotInfoDB.Fields.REPORTS_OBJECT.toString())
                .getJSONArray(ReportsConfigField.BANNED_USERS.toString()).put(id);

        update(obj);
    }

    public void unbanReportsUser(long id) {
        if (!isUserReportsBanned(id))
            throw new IllegalStateException("This user isn't banned!");

        final var obj = getDocument();
        final var arr = obj.getJSONObject(BotInfoDB.Fields.REPORTS_OBJECT.toString())
                .getJSONArray(ReportsConfigField.BANNED_USERS.toString());

        arr.remove(getIndexOfObjectInArray(arr, id));

        update(obj);
    }

    public void addDeveloper(long developer) {
        if (isDeveloper(developer))
            throw new NullPointerException("This user is already a developer!");

        final var obj = getDocument();
        final var devArr = obj.getJSONArray(BotInfoDB.Fields.DEVELOPERS_ARRAY.toString());

        devArr.put(developer);

        update(obj);
    }

    public void removeDeveloper(long developer) {
        if (!isDeveloper(developer))
            throw new NullPointerException("This user isn't a developer!");

        final var obj = getDocument();
        final var devArr = obj.getJSONArray(BotInfoDB.Fields.DEVELOPERS_ARRAY.toString());

        devArr.remove(getIndexOfObjectInArray(devArr, developer));

        update(obj);
    }

    public boolean isDeveloper(long developer) {
        return arrayHasObject(getDocument().getJSONArray(BotInfoDB.Fields.DEVELOPERS_ARRAY.toString()), developer);
    }

    private JSONObject getDocument() {
        return getCache().getJSONObject(0);
    }

    private void update(JSONObject jsonObject) {
        updateCache(jsonObject, "identifier", "robertify_main_config");
    }

    public static void initCache() {
        logger.debug("Instantiating new Bot Info cache");
        instance = new BotInfoCache();
        logger.debug("BOT INFO CACHE = {}", instance.getCache());
    }

    public String getJSON(boolean indented) {
        return indented ? getCache().toString(4) : getCache().toString();
    }
}
