package main.utils.json.suggestions;

import main.constants.JSONConfigFile;
import main.utils.json.legacy.AbstractJSONFile;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SuggestionsConfig extends AbstractJSONFile {
    public SuggestionsConfig() {
        super(JSONConfigFile.SUGGESTIONS);
    }

    @Override
    public void initConfig() {
        try {
            makeConfigFile();
        } catch (IllegalStateException e) {
            return;
        }

        resetConfig();
    }

    public void initChannels(long categoryID, long pendingChannel, long acceptedChanel, long deniedChannel) {
        if (isSetup())
            throw new IllegalStateException("The channels have already been setup!");

        final var obj = getJSONObject();

        obj.put(SuggestionsConfigField.SUGGESTIONS_CATEGORY.toString(), categoryID);
        obj.put(SuggestionsConfigField.PENDING_CHANNEL.toString(), pendingChannel);
        obj.put(SuggestionsConfigField.ACCEPTED_CHANNEL.toString(), acceptedChanel);
        obj.put(SuggestionsConfigField.DENIED_CHANNEL.toString(), deniedChannel);

        setJSON(obj);
    }

    public void resetConfig() {
        final var obj = new JSONObject();

        obj.put(SuggestionsConfigField.SUGGESTIONS_CATEGORY.toString(), -1L);
        obj.put(SuggestionsConfigField.PENDING_CHANNEL.toString(), -1L);
        obj.put(SuggestionsConfigField.ACCEPTED_CHANNEL.toString(), -1L);
        obj.put(SuggestionsConfigField.DENIED_CHANNEL.toString(), -1L);

        setJSON(obj);
    }

    public long getCategoryID() {
        return getJSONObject().getLong(SuggestionsConfigField.SUGGESTIONS_CATEGORY.toString());
    }

    public long getPendingChannelID() {
        return getJSONObject().getLong(SuggestionsConfigField.PENDING_CHANNEL.toString());
    }

    public long getAcceptedChannelID() {
        return getJSONObject().getLong(SuggestionsConfigField.ACCEPTED_CHANNEL.toString());
    }

    public long getDeniedChannelID() {
        return getJSONObject().getLong(SuggestionsConfigField.DENIED_CHANNEL.toString());
    }

    public void banUser(long id) {
        if (userIsBanned(id))
            throw new IllegalStateException("This user is already banned!");

        final var obj = getJSONObject();
        obj.getJSONArray(SuggestionsConfigField.BANNED_USERS.toString()).put(id);

        setJSON(obj);
    }

    public void unbanUser(long id) {
        if (!userIsBanned(id))
            throw new IllegalStateException("This user is not banned!");

        final var obj = getJSONObject();
        JSONArray jsonArray = obj.getJSONArray(SuggestionsConfigField.BANNED_USERS.toString());
        jsonArray.remove(getIndexOfObjectInArray(jsonArray, id));

        setJSON(obj);
    }

    public List<Long> getBannedUsers() {
        final List<Long> ret = new ArrayList<>();
        final var arr = getJSONObject().getJSONArray(SuggestionsConfigField.BANNED_USERS.toString());

        for (int i = 0; i < arr.length(); i++)
            ret.add(arr.getLong(i));

        return ret;
    }

    public boolean userIsBanned(long id) {
        JSONArray array = getJSONObject().getJSONArray(SuggestionsConfigField.BANNED_USERS.toString());
        return arrayHasObject(array, id);
    }

    public boolean isSetup() {
        return getJSONObject().getLong(SuggestionsConfigField.SUGGESTIONS_CATEGORY.toString()) != -1L;
    }
}
