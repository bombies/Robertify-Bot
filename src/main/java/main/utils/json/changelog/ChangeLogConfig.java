package main.utils.json.changelog;

import main.constants.JSONConfigFile;
import main.constants.TimeFormat;
import main.utils.GeneralUtils;
import main.utils.json.AbstractJSONFile;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Deprecated
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
        obj.put(ChangeLogConfigField.TITLE.toString(), "");
        var pastLogsArr = new JSONArray();
        pastLogsArr.put(new JSONObject());
        obj.put(ChangeLogConfigField.PAST_LOGS.toString(), pastLogsArr);

        setJSON(obj);
    }

    public void addChangeLog(LogType type, String changelog) {
        var obj = getJSONObject();
        obj.getJSONArray(ChangeLogConfigField.CURRENT_LOG.toString()).put(new JSONObject()
                .put(ChangeLogConfigField.LOG_TYPE.toString(), type.name())
                .put(ChangeLogConfigField.LOG_STRING.toString(), changelog));
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

    public List<Pair<LogType, String>> getCurrentChangelog() {
        var obj = getJSONObject();
        var logArr = obj.getJSONArray(ChangeLogConfigField.CURRENT_LOG.toString());
        var ret = new ArrayList<Pair<LogType, String>>();

        for (int i = 0; i < logArr.length(); i++) {
            JSONObject jsonObject = logArr.getJSONObject(i);
            ret.add(Pair.of(LogType.parse(jsonObject.getString(ChangeLogConfigField.LOG_TYPE.toString())), jsonObject.getString(ChangeLogConfigField.LOG_STRING.toString())));
        }

        return ret;
    }

    public void setTitle(String title) {
        JSONObject jsonObject = getJSONObject();
        jsonObject.put(ChangeLogConfigField.TITLE.toString(), title);
        setJSON(jsonObject);
    }

    public String getTitle() {
        try {
            return getJSONObject().getString(ChangeLogConfigField.TITLE.toString());
        } catch (JSONException e) {
            return "";
        }
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
        for (var s : changelog)
            pastLogObjectLogArray.put(new JSONObject()
                    .put(ChangeLogConfigField.LOG_TYPE.toString(), s.getLeft())
                    .put(ChangeLogConfigField.LOG_STRING.toString(), s.getRight()));

        setJSON(obj);
    }

    public enum LogType {
        FEATURE,
        BUGFIX;

        public static LogType parse(String s) {
            switch (s.toLowerCase().strip()) {
                case "feature" -> {
                    return FEATURE;
                }
                case "bugfix" -> {
                    return BUGFIX;
                }
                default -> throw new IllegalArgumentException("There is no such log type that matches \""+s+"\"");
            }
        }
    }
}
