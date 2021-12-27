package main.utils.json.legacy.reports;

import main.constants.JSONConfigFile;
import main.utils.json.legacy.AbstractJSONFile;
import org.json.JSONArray;
import org.json.JSONObject;

@Deprecated
public class LegacyReportsConfig extends AbstractJSONFile {
    public LegacyReportsConfig() {
        super(JSONConfigFile.REPORTS);
    }

    @Override
    public void initConfig() {
        try {
            makeConfigFile();
        } catch (IllegalStateException e) {
            return;
        }

        final var obj = new JSONObject();

        obj.put(ReportsConfigField.CATEGORY.toString(), -1L);
        obj.put(ReportsConfigField.CHANNEL.toString(), -1L);
        obj.put(ReportsConfigField.BANNED_USERS.toString(), new JSONArray());

        setJSON(obj);
    }

    public void initChannels(long categoryID, long channelID) {
        if (isSetup())
            throw new IllegalStateException("The reports category has already been setup!");

        final var obj = getJSONObject();

        obj.put(ReportsConfigField.CATEGORY.toString(), categoryID);
        obj.put(ReportsConfigField.CHANNEL.toString(), channelID);

        setJSON(obj);
    }

    public void resetConfig() {
        final var obj = getJSONObject();

        obj.put(ReportsConfigField.CATEGORY.toString(), -1L);
        obj.put(ReportsConfigField.CHANNEL.toString(), -1L);

        setJSON(obj);
    }

    public boolean isSetup() {
        return getJSONObject().getLong(ReportsConfigField.CATEGORY.toString()) != -1L;
    }

    public long getID(ReportsConfigField field) {
        return getJSONObject().getLong(field.toString());
    }

    public boolean isUserBanned(long id) {
        return arrayHasObject(getJSONObject().getJSONArray(ReportsConfigField.BANNED_USERS.toString()), id);
    }

    public void banUser(long id) {
        if (isUserBanned(id))
            throw new IllegalStateException("This user has already been banned!");

        final var obj = getJSONObject();

        obj.getJSONArray(ReportsConfigField.BANNED_USERS.toString()).put(id);

        setJSON(obj);
    }

    public void unbanUser(long id) {
        if (!isUserBanned(id))
            throw new IllegalStateException("This user isn't banned!");

        final var obj = getJSONObject();
        final var arr = obj.getJSONArray(ReportsConfigField.BANNED_USERS.toString());

        arr.remove(getIndexOfObjectInArray(arr, id));

        setJSON(obj);
    }
}
