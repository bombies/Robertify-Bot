package main.utils.json.permissions;

import main.commands.commands.management.permissions.Permission;
import main.utils.database.mongodb.GuildsDB;
import main.utils.database.mongodb.cache.GuildsDBCache;
import main.utils.json.AbstractJSON;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.legacy.permissions.PermissionConfigField;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PermissionsConfig implements AbstractJSON {
    private static final GuildsDBCache cache = GuildsDBCache.getInstance();

    public void addPermissionToUser(long guildID, long userID, Permission p) {
        var obj = getGuildObject(guildID);
        var usersObj = obj.getJSONObject(GuildsDB.Field.PERMISSIONS_OBJECT.toString())
                .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString());

        try {
            if (userHasPermission(guildID, userID, p))
                throw new IllegalArgumentException("User with id \"" + userID + "\" already has " + p.name() + "");
        } catch (NullPointerException e) {
            usersObj.put(String.valueOf(userID), new JSONArray());
            cache.updateCache(obj, GuildsDB.Field.GUILD_ID, guildID);
        }

        obj = getGuildObject(guildID);
        usersObj = obj.getJSONObject(GuildsDB.Field.PERMISSIONS_OBJECT.toString())
                .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString());

        JSONArray array = usersObj.getJSONArray(String.valueOf(userID));
        array.put(p.getCode());

        usersObj.put(String.valueOf(userID), array);

        cache.updateCache(obj, GuildsDB.Field.GUILD_ID, guildID);
    }

    public boolean userHasPermission(long guildID, long userID, Permission p) {
        var userObj = getGuildObject(guildID)
                .getJSONObject(GuildsDB.Field.PERMISSIONS_OBJECT.toString())
                .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString());

        if (!userObj.has(String.valueOf(userID)))
            throw new NullPointerException("There is no user with ID: \""+userID+"\"");

        return userObj.getJSONArray(String.valueOf(userID)).toList().contains(p.getCode());
    }

    public void removePermissionFromUser(long guildID, long userID, Permission p) {
        if (!userHasPermission(guildID, userID, p))
            throw new IllegalArgumentException("User with id \""+userID+"\" doesn't have "+p.name()+"");

        var obj = getGuildObject(guildID);
        var usersObj = obj.getJSONObject(GuildsDB.Field.PERMISSIONS_OBJECT.toString())
                .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString());

        JSONArray array = usersObj.getJSONArray(String.valueOf(userID));
        array.remove(getIndexOfObjectInArray(array, p.getCode()));

        usersObj.put(String.valueOf(userID), array);
        cache.updateCache(obj, GuildsDB.Field.GUILD_ID, guildID);
    }

    public void removeRoleFromPermission(long gid, long rid, Permission p) throws IllegalAccessException, IOException {
        if (!getRolesForPermission(gid, p).contains(rid))
            throw new IllegalAccessException("The role "+rid+" doesn't have access to Permission with code "+p.getCode());

        JSONObject obj = getGuildObject(gid);
        JSONArray permArr = obj.getJSONObject(GuildsDB.Field.PERMISSIONS_OBJECT.toString())
                .getJSONArray(String.valueOf(p.getCode()));
        permArr.remove(getIndexOfObjectInArray(permArr, rid));
        cache.updateCache(obj, GuildsDB.Field.GUILD_ID, gid);
    }

    public void addRoleToPermission(long gid, long rid, Permission p) throws IllegalAccessException, IOException {
        if (getRolesForPermission(gid, p).contains(rid))
            throw new IllegalAccessException("The role "+rid+" already has access to Permission with code "+ p.getCode());

        JSONObject obj = getGuildObject(gid);
        JSONArray permArr = obj.getJSONObject(GuildsDB.Field.PERMISSIONS_OBJECT.toString())
                .getJSONArray(String.valueOf(p.getCode()));
        permArr.put(rid);

        cache.updateCache(obj, GuildsDB.Field.GUILD_ID, gid);
    }

    public List<Long> getRolesForPermission(long gid, Permission p) {
        List<Long> ret = new ArrayList<>();
        JSONArray arr = getGuildObject(gid)
                .getJSONObject(GuildsDB.Field.PERMISSIONS_OBJECT.toString())
                .getJSONArray(String.valueOf(p.getCode()));

        for (int i = 0; i < arr.length(); i++)
            ret.add(arr.getLong(i));

        return ret;
    }

    public List<Long> getRolesForPermission(long gid, String p) {
        if (!Permission.getPermissions().contains(p.toUpperCase()))
            throw new NullPointerException("There is no enum with the name \""+p+"\"");

        int code = -1;

        for (Permission perm : Permission.values())
            if (perm.name().equalsIgnoreCase(p)) {
                code = perm.getCode(); break;
            }

        return getRolesForPermission(gid, Permission.parse(code));
    }

    public List<Long> getUsersForPermission(long gid, String p) {
        if (!Permission.getPermissions().contains(p.toUpperCase()))
            throw new NullPointerException("There is no enum with the name \""+p+"\"");

        int code = -1;

        for (Permission perm : Permission.values())
            if (perm.name().equalsIgnoreCase(p)) {
                code = perm.getCode(); break;
            }

        List<Long> ret = new ArrayList<>();

        try {
            JSONObject object = getGuildObject(gid)
                    .getJSONObject(GuildsDB.Field.PERMISSIONS_OBJECT.toString())
                    .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString());

            for (String s : object.keySet())
                if (object.getJSONArray(s).toList().contains(code))
                    ret.add(Long.parseLong(s));

            return ret;
        } catch (JSONException e) {
            final var obj = getGuildObject(gid);
            obj.put(PermissionConfigField.USER_PERMISSIONS.toString(), new JSONObject());
            cache.updateCache(obj, GuildsDB.Field.GUILD_ID, gid);
            return ret;
        }
    }

    public List<Integer> getPermissionsForRoles(long gid, long rid) {
        List<Integer> codes = new ArrayList<>();
        JSONObject obj = getGuildObject(gid);

        for (int i = 0; i < obj.length()-1; i++) {
            JSONArray arr = obj.getJSONArray(String.valueOf(i));
            for (int j = 0; j < arr.length(); j++)
                if (arr.getLong(j) == rid) {
                    codes.add(i); break;
                }
        }

        return codes;
    }

    public List<Integer> getPermissionsForUser(long gid, long uid) {
        List<Integer> codes = new ArrayList<>();
        JSONObject obj = getGuildObject(gid)
                .getJSONObject(GuildsDB.Field.PERMISSIONS_OBJECT.toString())
                .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString());
        JSONArray arr = obj.getJSONArray(String.valueOf(uid));

        for (int i = 0; i < arr.length(); i++)
            codes.add(arr.getInt(i));

        return codes;
    }

    private JSONObject getGuildObject(long gid) {
        return new GuildConfig().getGuildObject(gid);
    }

    public void update() {
        final var cacheArr = cache.getCache();

        for (int i = 0; i < cacheArr.length(); i++) {
            final var guildObj = cacheArr.getJSONObject(i);
            boolean changesMade = false;

            for (int code : Permission.getCodes())
                if (!guildObj.has(String.valueOf(code))) {
                    changesMade = true;
                    guildObj.put(String.valueOf(code), new JSONArray());
                }

            if (!guildObj.has(PermissionConfigField.USER_PERMISSIONS.toString())) {
                changesMade = true;
                guildObj.put(PermissionConfigField.USER_PERMISSIONS.toString(), new JSONObject());
            }

            if (changesMade) cache.updateCache(Document.parse(guildObj.toString()));
        }
    }
}
