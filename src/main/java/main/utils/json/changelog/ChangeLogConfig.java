package main.utils.json.changelog;

import main.constants.JSONConfigFile;
import main.constants.TimeFormat;
import main.utils.GeneralUtils;
import main.utils.json.legacy.AbstractJSONFile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChangeLogConfig extends AbstractJSONFile {
    public ChangeLogConfig() {
        super(JSONConfigFile.CHANGELOG);
    }

    @Override
    public void initConfig() {
        try {
            makeConfigFile();
        } catch (IllegalStateException e) {
            return;
        }

        var obj = new JSONObject();

        obj.put(ChangeLogConfigField.CURRENT_LOG.toString(), new JSONArray());
        var pastLogsArr = new JSONArray();
        pastLogsArr.put(new JSONObject());
        obj.put(ChangeLogConfigField.PAST_LOGS.toString(), pastLogsArr);

        setJSON(obj);
    }

    public void addChangeLog(String changelog) {
        var obj = getJSONObject();
        obj.getJSONArray(ChangeLogConfigField.CURRENT_LOG.toString()).put(changelog);
        setJSON(obj);
    }

    public void removeChangeLog(int id) {
        var obj = getJSONObject();
        obj.getJSONArray(ChangeLogConfigField.CURRENT_LOG.toString()).remove(id);
        setJSON(obj);
    }

    public void clearChangeLog() {
        var obj = getJSONObject();
        obj.put(ChangeLogConfigField.CURRENT_LOG.toString(), new JSONArray());
        setJSON(obj);
    }

    public List<String> getCurrentChangelog() {
        var obj = getJSONObject();
        var logArr = obj.getJSONArray(ChangeLogConfigField.CURRENT_LOG.toString());
        var ret = new ArrayList<String>();

        for (int i = 0; i < logArr.length(); i++)
            ret.add(logArr.getString(i));

        return ret;
    }

    public void sendLog() {
        var currentDate = GeneralUtils.formatDate(new Date().getTime(), TimeFormat.DD_MMMM_YYYY);
        var changelog = getCurrentChangelog();

        clearChangeLog();
        var obj = getJSONObject();

        var pastLogs = obj.getJSONArray(ChangeLogConfigField.PAST_LOGS.toString()).getJSONObject(0);
        try {
            pastLogs.getJSONObject(currentDate.toLowerCase());
        } catch (JSONException e) {
            var logsArray = new JSONArray();
            var pastLogObject = new JSONObject();
            pastLogObject.put(ChangeLogConfigField.LOGS.toString(), logsArray);
            pastLogs.put(currentDate.toLowerCase(), pastLogObject);
        }

        var pastLogObject = pastLogs.getJSONObject(currentDate.toLowerCase());
        var pastLogObjectLogArray = pastLogObject.getJSONArray(ChangeLogConfigField.LOGS.toString());
        for (String s : changelog)
            pastLogObjectLogArray.put(s);

        setJSON(obj);
    }
}
