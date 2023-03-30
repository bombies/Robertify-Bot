package main.utils.apis.robertify.models;

import lombok.Getter;
import main.commands.slashcommands.SlashCommandManager;
import main.constants.Permission;
import main.constants.RobertifyTheme;
import main.utils.json.logs.LogType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RobertifyGuild {
    private final JSONObject dedicated_channel;
    private final JSONObject restricted_channels;
    private final String prefix;
    private final JSONObject permissions;
    private final JSONObject toggles;
    private final JSONArray eight_ball;
    private final JSONObject announcement_channel;
    private final String theme;
    private final JSONObject server_id;
    private final JSONArray banned_users;

    public RobertifyGuild(JSONObject dedicated_channel, JSONObject restricted_channels, String prefix,
                          JSONObject permissions, JSONObject toggles, JSONArray eight_ball,
                          JSONObject announcement_channel, String theme, JSONObject server_id,
                          JSONArray banned_users) {
        this.dedicated_channel = dedicated_channel;
        this.restricted_channels = restricted_channels;
        this.prefix = prefix;
        this.permissions = permissions;
        this.toggles = toggles;
        this.eight_ball = eight_ball;
        this.announcement_channel = announcement_channel;
        this.theme = theme;
        this.server_id = server_id;
        this.banned_users = banned_users;
    }

    public DedicatedChannel getDedicatedChannel() {
        return new DedicatedChannel(
                dedicated_channel.getLong("message_id"),
                dedicated_channel.getLong("channel_id")
        );
    }

    public RestictedChannels getRestrictedChannels() {
        List<Object> voice_channels = restricted_channels.getJSONArray("voice_channels").toList();
        List<Object> text_channels = restricted_channels.getJSONArray("text_channels").toList();
        return new RestictedChannels(
                voice_channels.stream().map(o -> (Long)o).toList(),
                text_channels.stream().map(o -> (Long) o).toList()
        );
    }

    public String getPrefix() {
        return prefix;
    }

    public Permissions getPermissions() {
        HashMap<Integer, List<Long>> permissionMap = new HashMap<>();
        for (Permission p : Permission.values()) {
            if (!permissions.has(String.valueOf(p.getCode())))
                continue;

            final List<Long> l = new ArrayList<>();
            final var array = permissions.getJSONArray(String.valueOf(p.getCode()));
            for (var o : array)
                l.add((Long)o);
            permissionMap.put(p.getCode(), l);
        }

        return new Permissions(
                permissionMap,
                permissions.has("users") ? permissions.getJSONObject("users") : null
        );
    }

    public Toggles getToggles() {
        final HashMap<main.constants.Toggles, Boolean> togglesMap = new HashMap<>();
        final HashMap<String, Boolean> djToggles = new HashMap<>();
        final HashMap<LogType, Boolean> logToggles = new HashMap<>();

        for (final var toggle : main.constants.Toggles.values())
            togglesMap.put(toggle, toggles.getBoolean(toggle.toString()));

        JSONObject djTogglesObj = toggles.getJSONObject("dj_toggles");
        for (final var cmd : SlashCommandManager.getInstance().getMusicCommands())
            djToggles.put(cmd.getName(), djTogglesObj.getBoolean(cmd.getName()));

        JSONObject logTogglesObj = toggles.getJSONObject("log_toggles");
        for (final var logToggle : LogType.values())
            logToggles.put(logToggle, logTogglesObj.getBoolean(logToggle.name().toLowerCase()));

        return new Toggles(
                togglesMap,
                djToggles,
                logToggles
        );
    }

    public List<String> getEightBallResponses() {
        return eight_ball.toList().stream().map(o -> (String)o).toList();
    }

    public long getAnnouncementChannel() {
        final long TWO_PWR_32_DBL = (long) (1 << 16) * (1 << 16);

        if (announcement_channel.getBoolean("unsigned"))
            return ((announcement_channel.getInt("high") >>> 0) * TWO_PWR_32_DBL) + (announcement_channel.getInt("low") >>> 0);
        return (announcement_channel.getInt("high") * TWO_PWR_32_DBL) + (announcement_channel.getInt("low") >>> 0);
    }

    public RobertifyTheme getTheme() {
        return RobertifyTheme.parse(theme);
    }

    public long getGuildID() {
        final long TWO_PWR_32_DBL = (long) (1 << 16) * (1 << 16);

        if (server_id.getBoolean("unsigned"))
            return ((server_id.getInt("high") >>> 0) * TWO_PWR_32_DBL) + (server_id.getInt("low") >>> 0);
        return (server_id.getInt("high") * TWO_PWR_32_DBL) + (server_id.getInt("low") >>> 0);
    }

    public List<Long> getBannedUsers() {
        return banned_users.toList().stream().map(o -> (Long)o).toList();
    }

    public static class DedicatedChannel {
        @Getter
        final long messageID, channelID;

        protected DedicatedChannel(long messageID, long channelID) {
            this.messageID = messageID;
            this.channelID = channelID;
        }
    }

    public static class RestictedChannels {
        @Getter
        final List<Long> voiceChannels, textChannels;

        protected RestictedChannels(List<Long> voiceChannels, List<Long> textChannels) {
            this.voiceChannels = voiceChannels;
            this.textChannels = textChannels;
        }
    }

    public static class Permissions {
        @Getter
        final HashMap<Integer, List<Long>> permissions;
        @Getter
        final JSONObject users;

        protected Permissions(HashMap<Integer, List<Long>> permissions, JSONObject users) {
            this.permissions = permissions;
            this.users = users;
        }
    }

    public static class Toggles {
        @Getter
        final HashMap<main.constants.Toggles, Boolean> toggles;
        final HashMap<String, Boolean>djToggles;
        final HashMap<LogType, Boolean>logToggles;

        protected Toggles(HashMap<main.constants.Toggles, Boolean> toggles, HashMap<String, Boolean> djToggles, HashMap<LogType, Boolean> logToggles) {
            this.toggles = toggles;
            this.djToggles = djToggles;
            this.logToggles = logToggles;
        }
    }
}
