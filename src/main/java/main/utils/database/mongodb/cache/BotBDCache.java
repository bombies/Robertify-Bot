package main.utils.database.mongodb.cache;

import lombok.Getter;
import main.utils.database.mongodb.databases.BotDB;
import main.utils.json.GenericJSONField;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class BotBDCache extends AbstractMongoCache {
    private final static Logger logger = LoggerFactory.getLogger(BotBDCache.class);
    @Getter
    private static BotBDCache instance;

    private BotBDCache() {
        super(BotDB.ins());
        this.init();
        updateCache();
        logger.debug("Done instantiating Bot Info cache");
    }

    public void setLastStartup(long time) {
        JSONObject jsonObject = getDocument();
        jsonObject.put(BotDB.Fields.LAST_BOOTED.toString(), time);
        update(jsonObject);
    }

    public long getLastStartup() {
        return getDocument().getLong(BotDB.Fields.LAST_BOOTED.toString());
    }

    public void initSuggestionChannels(long categoryID, long pendingChannel, long acceptedChanel, long deniedChannel) {
        if (isSuggestionsSetup())
            throw new IllegalStateException("The channels have already been setup!");

        final var obj = getDocument();
        final var suggestionObj = obj.getJSONObject(BotDB.Fields.SUGGESTIONS_OBJECT.toString());

        suggestionObj.put(SuggestionsConfigField.SUGGESTIONS_CATEGORY.toString(), categoryID);
        suggestionObj.put(SuggestionsConfigField.PENDING_CHANNEL.toString(), pendingChannel);
        suggestionObj.put(SuggestionsConfigField.ACCEPTED_CHANNEL.toString(), acceptedChanel);
        suggestionObj.put(SuggestionsConfigField.DENIED_CHANNEL.toString(), deniedChannel);

        update(obj);
    }

    public void resetSuggestionsConfig() {
        final var obj = getDocument();
        final var suggestionObj = obj.getJSONObject(BotDB.Fields.SUGGESTIONS_OBJECT.toString());

        suggestionObj.put(SuggestionsConfigField.SUGGESTIONS_CATEGORY.toString(), -1L);
        suggestionObj.put(SuggestionsConfigField.PENDING_CHANNEL.toString(), -1L);
        suggestionObj.put(SuggestionsConfigField.ACCEPTED_CHANNEL.toString(), -1L);
        suggestionObj.put(SuggestionsConfigField.DENIED_CHANNEL.toString(), -1L);

        update(obj);
    }

    public long getSuggestionsCategoryID() {
        return getDocument().getJSONObject(BotDB.Fields.SUGGESTIONS_OBJECT.toString())
                .getLong(SuggestionsConfigField.SUGGESTIONS_CATEGORY.toString());
    }

    public long getSuggestionsPendingChannelID() {
        return getDocument().getJSONObject(BotDB.Fields.SUGGESTIONS_OBJECT.toString())
                .getLong(SuggestionsConfigField.PENDING_CHANNEL.toString());
    }

    public long getSuggestionsAcceptedChannelID() {
        return getDocument().getJSONObject(BotDB.Fields.SUGGESTIONS_OBJECT.toString())
                .getLong(SuggestionsConfigField.ACCEPTED_CHANNEL.toString());
    }

    public long getSuggestionsDeniedChannelID() {
        return getDocument().getJSONObject(BotDB.Fields.SUGGESTIONS_OBJECT.toString())
                .getLong(SuggestionsConfigField.DENIED_CHANNEL.toString());
    }

    public void banSuggestionsUser(long id) {
        if (userIsSuggestionBanned(id))
            throw new IllegalStateException("This user is already banned!");

        final var obj = getDocument();
        obj.getJSONObject(BotDB.Fields.SUGGESTIONS_OBJECT.toString())
                .getJSONArray(SuggestionsConfigField.BANNED_USERS.toString()).put(id);

        update(obj);
    }

    public void unbanSuggestionUser(long id) {
        if (!userIsSuggestionBanned(id))
            throw new IllegalStateException("This user is not banned!");

        final var obj = getDocument();
        JSONArray jsonArray = obj.getJSONObject(BotDB.Fields.SUGGESTIONS_OBJECT.toString())
                .getJSONArray(SuggestionsConfigField.BANNED_USERS.toString());
        jsonArray.remove(getIndexOfObjectInArray(jsonArray, id));

        update(obj);
    }

    public List<Long> getBannedSuggestionUsers() {
        final List<Long> ret = new ArrayList<>();
        final var arr = getDocument().getJSONObject(BotDB.Fields.SUGGESTIONS_OBJECT.toString())
                .getJSONArray(SuggestionsConfigField.BANNED_USERS.toString());

        for (int i = 0; i < arr.length(); i++)
            ret.add(arr.getLong(i));

        return ret;
    }

    public boolean userIsSuggestionBanned(long id) {
        JSONArray array = getDocument().getJSONObject(BotDB.Fields.SUGGESTIONS_OBJECT.toString())
                .getJSONArray(SuggestionsConfigField.BANNED_USERS.toString());
        return arrayHasObject(array, id);
    }

    public boolean isSuggestionsSetup() {
        return getDocument().getJSONObject(BotDB.Fields.SUGGESTIONS_OBJECT.toString())
                .getLong(SuggestionsConfigField.SUGGESTIONS_CATEGORY.toString()) != -1L;
    }

    public void initReportChannels(long categoryID, long channelID) {
        if (isReportsSetup())
            throw new IllegalStateException("The reports category has already been setup!");

        final var obj = getDocument();

        obj.getJSONObject(BotDB.Fields.REPORTS_OBJECT.toString())
                .put(ReportsConfigField.CATEGORY.toString(), categoryID);
        obj.getJSONObject(BotDB.Fields.REPORTS_OBJECT.toString())
                .put(ReportsConfigField.CHANNEL.toString(), channelID);

        update(obj);
    }

    public void resetReportsConfig() {
        final var obj = getDocument();

        obj.getJSONObject(BotDB.Fields.REPORTS_OBJECT.toString())
                .put(ReportsConfigField.CATEGORY.toString(), -1L);
        obj.getJSONObject(BotDB.Fields.REPORTS_OBJECT.toString())
                .put(ReportsConfigField.CHANNEL.toString(), -1L);

        update(obj);
    }

    public boolean isReportsSetup() {
        return getDocument().getJSONObject(BotDB.Fields.REPORTS_OBJECT.toString())
                .getLong(ReportsConfigField.CATEGORY.toString()) != -1L;
    }

    public long getReportsID(ReportsConfigField field) {
        return getDocument().getJSONObject(BotDB.Fields.REPORTS_OBJECT.toString())
                .getLong(field.toString());
    }

    public boolean isUserReportsBanned(long id) {
        return arrayHasObject(getDocument().getJSONObject(BotDB.Fields.REPORTS_OBJECT.toString())
                .getJSONArray(ReportsConfigField.BANNED_USERS.toString()), id);
    }

    public void banReportsUser(long id) {
        if (isUserReportsBanned(id))
            throw new IllegalStateException("This user has already been banned!");

        final var obj = getDocument();

        obj.getJSONObject(BotDB.Fields.REPORTS_OBJECT.toString())
                .getJSONArray(ReportsConfigField.BANNED_USERS.toString()).put(id);

        update(obj);
    }

    public void unbanReportsUser(long id) {
        if (!isUserReportsBanned(id))
            throw new IllegalStateException("This user isn't banned!");

        final var obj = getDocument();
        final var arr = obj.getJSONObject(BotDB.Fields.REPORTS_OBJECT.toString())
                .getJSONArray(ReportsConfigField.BANNED_USERS.toString());

        arr.remove(getIndexOfObjectInArray(arr, id));

        update(obj);
    }

    public void addDeveloper(long developer) {
        if (isDeveloper(developer))
            throw new NullPointerException("This user is already a developer!");

        final var obj = getDocument();
        final var devArr = obj.getJSONArray(BotDB.Fields.DEVELOPERS_ARRAY.toString());

        devArr.put(developer);

        update(obj);
    }

    public void removeDeveloper(long developer) {
        if (!isDeveloper(developer))
            throw new NullPointerException("This user isn't a developer!");

        final var obj = getDocument();
        final var devArr = obj.getJSONArray(BotDB.Fields.DEVELOPERS_ARRAY.toString());

        devArr.remove(getIndexOfObjectInArray(devArr, developer));

        update(obj);
    }

    public boolean isDeveloper(long developer) {
        return arrayHasObject(getDocument().getJSONArray(BotDB.Fields.DEVELOPERS_ARRAY.toString()), developer);
    }

    public List<Long> getDevelopers() {
        final List<Long> ret = new ArrayList<>();
        JSONArray array = getDocument().getJSONArray(BotDB.Fields.DEVELOPERS_ARRAY.toString());
        array.forEach(val -> ret.add((long) val));
        return ret;
    }

    public void addRandomMessage(String s) {
        final var obj = getDocument();

        if (!obj.has(BotDB.Fields.RANDOM_MESSAGES.toString()))
            obj.put(BotDB.Fields.RANDOM_MESSAGES.toString(), new JSONArray());

        obj.getJSONArray(BotDB.Fields.RANDOM_MESSAGES.toString()).put(s);

        update(obj);
    }

    public List<String> getRandomMessages() {
        final List<String> ret = new ArrayList<>();
        final var arr = getDocument().getJSONArray(BotDB.Fields.RANDOM_MESSAGES.toString());

        for (int i = 0; i < arr.length(); i++)
            ret.add(arr.getString(i));

        return ret;
    }

    public void removeMessage(int id) {
        final var obj = getDocument();
        final var arr = obj.getJSONArray(BotDB.Fields.RANDOM_MESSAGES.toString());

        if (id < 0 || id > arr.length()-1)
            throw new IndexOutOfBoundsException("The ID passed exceeds the bounds of the array!");

        arr.remove(id);

        update(obj);
    }

    public void clearMessages() {
        final var obj = getDocument();
        final var arr = obj.getJSONArray(BotDB.Fields.RANDOM_MESSAGES.toString());

        arr.clear();

        update(obj);
    }

    public void setLatestAlert(String alert) {
        final var obj = getDocument();
        final var alertObj = obj.getJSONObject(BotDB.Fields.LATEST_ALERT.toString());
        alertObj.put(BotDB.Fields.SubFields.ALERT.toString(), alert.replaceAll("\\\\n", "\n").replaceAll("\\\\t", "\t"));
        alertObj.put(BotDB.Fields.SubFields.ALERT_TIME.toString(), System.currentTimeMillis());
        update(obj);
        clearAlertViewers();
    }

    public Pair<String, Long> getLatestAlert() {
        final var obj = getDocument();
        final var alertObj = obj.getJSONObject(BotDB.Fields.LATEST_ALERT.toString());
        final var alert = alertObj.getString(BotDB.Fields.SubFields.ALERT.toString());
        final var alertTime = alertObj.getLong(BotDB.Fields.SubFields.ALERT_TIME.toString());
        return Pair.of(alert, alertTime);
    }

    public void addAlertViewer(long id) {
        if (userHasViewedAlert(id))
            return;

        final var obj = getDocument();
        final var viewerArr = obj.getJSONArray(BotDB.Fields.ALERT_VIEWERS.toString());
        viewerArr.put(id);

        update(obj);
    }

    public boolean userHasViewedAlert(long id) {
        if (getLatestAlert().getLeft().isBlank() || getLatestAlert().getLeft().isEmpty())
            return true;

        final var obj = getDocument();
        final var viewerArr = obj.getJSONArray(BotDB.Fields.ALERT_VIEWERS.toString());
        return arrayHasObject(viewerArr, id);
    }

    public int getAlertViewerCount() {
        final var obj = getDocument();
        final var viewerArr = obj.getJSONArray(BotDB.Fields.ALERT_VIEWERS.toString());
        return viewerArr.length();
    }

    public int getPosOfAlertViewer(long id) {
        if (!userHasViewedAlert(id))
            return -1;
        final var obj = getDocument();
        final var viewerArr = obj.getJSONArray(BotDB.Fields.ALERT_VIEWERS.toString());
        return getIndexOfObjectInArray(viewerArr, id) + 1;
    }

    public void clearAlertViewers() {
        final var obj = getDocument();
        final var viewerArr = obj.getJSONArray(BotDB.Fields.ALERT_VIEWERS.toString());
        viewerArr.clear();
        update(obj);
    }

    private JSONObject getDocument() {
        return getCache().getJSONObject(0);
    }

    private void update(JSONObject jsonObject) {
        updateCache(jsonObject, "identifier", "robertify_main_config");
    }

    public static void initCache() {
        logger.debug("Instantiating new Bot Info cache");
        instance = new BotBDCache();
        logger.debug("BOT INFO CACHE = {}", instance.getCache());
    }

    public String getJSON(boolean indented) {
        return indented ? getCache().toString(4) : getCache().toString();
    }

    public enum ReportsConfigField implements GenericJSONField {
        CHANNEL("reports_channel"),
        CATEGORY("reports_category"),
        BANNED_USERS("banned_users");

        private final String str;

        ReportsConfigField(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    public enum SuggestionsConfigField implements GenericJSONField {
        SUGGESTIONS_CATEGORY("suggestions_category"),
        ACCEPTED_CHANNEL("accepted_channel"),
        PENDING_CHANNEL("pending_channel"),
        DENIED_CHANNEL("denied_channel"),
        BANNED_USERS("banned_users");

        private final String str;

        SuggestionsConfigField(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
