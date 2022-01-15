package main.utils.json.permissions;

import main.constants.Permission;
import main.utils.database.mongodb.databases.GuildsDB;
import main.utils.json.AbstractGuildConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PermissionsConfig extends AbstractGuildConfig {

    public void addPermissionToUser(long guildID, long userID, Permission p) {
        var obj = getGuildObject(guildID);
        var usersObj = obj.getJSONObject(GuildsDB.Field.PERMISSIONS_OBJECT.toString())
                .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString());

        try {
            if (userHasPermission(guildID, userID, p))
                throw new IllegalArgumentException("User with id \"" + userID + "\" already has " + p.name() + "");
        } catch (NullPointerException e) {
            usersObj.put(String.valueOf(userID), new JSONArray());
            getCache().updateCache(obj, GuildsDB.Field.GUILD_ID, guildID);
        }

        obj = getGuildObject(guildID);
        usersObj = obj.getJSONObject(GuildsDB.Field.PERMISSIONS_OBJECT.toString())
                .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString());

        JSONArray array;

        try {
            array = usersObj.getJSONArray(String.valueOf(userID));
        } catch (JSONException e) {
            usersObj.put(String.valueOf(userID), new JSONArray());
            array = usersObj.getJSONArray(String.valueOf(userID));
        }

        array.put(p.getCode());

        usersObj.put(String.valueOf(userID), array);

        getCache().updateCache(obj, GuildsDB.Field.GUILD_ID, guildID);
    }

    public boolean userHasPermission(long guildID, long userID, Permission p) {
        var userObj = getGuildObject(guildID)
                .getJSONObject(GuildsDB.Field.PERMISSIONS_OBJECT.toString())
                .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString());

        if (!userObj.has(String.valueOf(userID)))
            return false;

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
        getCache().updateCache(obj, GuildsDB.Field.GUILD_ID, guildID);
    }

    public void removeRoleFromPermission(long gid, long rid, Permission p) throws IllegalAccessException, IOException {
        if (!getRolesForPermission(gid, p).contains(rid))
            throw new IllegalAccessException("The role "+rid+" doesn't have access to Permission with code "+p.getCode());

        JSONObject obj = getGuildObject(gid);
        JSONArray permArr = obj.getJSONObject(GuildsDB.Field.PERMISSIONS_OBJECT.toString())
                .getJSONArray(String.valueOf(p.getCode()));
        permArr.remove(getIndexOfObjectInArray(permArr, rid));
        getCache().updateCache(obj, GuildsDB.Field.GUILD_ID, gid);
    }

    public void addRoleToPermission(long gid, long rid, Permission p) throws IllegalAccessException {
        if (getRolesForPermission(gid, p).contains(rid))
            throw new IllegalAccessException("The role "+rid+" already has access to Permission with code "+ p.getCode());

        JSONObject obj = getGuildObject(gid);
        JSONArray permArr = obj.getJSONObject(GuildsDB.Field.PERMISSIONS_OBJECT.toString())
                .getJSONArray(String.valueOf(p.getCode()));
        permArr.put(rid);

        getCache().updateCache(obj, GuildsDB.Field.GUILD_ID, gid);
    }

    public List<Long> getRolesForPermission(long gid, Permission p) {
        List<Long> ret = new ArrayList<>();
        JSONObject object = getGuildObject(gid)
                .getJSONObject(GuildsDB.Field.PERMISSIONS_OBJECT.toString());

        JSONArray arr;

        try {
            arr = object.getJSONArray(String.valueOf(p.getCode()));
        } catch (JSONException e) {
            object.put(String.valueOf(p.getCode()), new JSONObject());
            arr = object.getJSONArray(String.valueOf(p.getCode()));
        }

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
            obj.getJSONObject(GuildsDB.Field.PERMISSIONS_OBJECT.toString())
                    .put(PermissionConfigField.USER_PERMISSIONS.toString(), new JSONObject());
            getCache().updateCache(obj, GuildsDB.Field.GUILD_ID, gid);
            return ret;
        }
    }

    public List<Integer> getPermissionsForRoles(long gid, long rid) {
        List<Integer> codes = new ArrayList<>();
        JSONObject obj = getGuildObject(gid);

        for (int i = 0; i < obj.length()-1; i++) {
            JSONArray arr = obj.getJSONObject(GuildsDB.Field.PERMISSIONS_OBJECT.toString())
                    .getJSONArray(String.valueOf(i));
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

    @Override
    public void update() {
        final var cacheArr = getCache().getCache();
        final List<JSONObject> objectToUpdate = new ArrayList<>();

        boolean globalChangesMade = false;
        for (int i = 0; i < cacheArr.length(); i++) {
            boolean localChangesMade = false;
            final var guildObj = cacheArr.getJSONObject(i);

            for (int code : Permission.getCodes())
                if (!guildObj.getJSONObject(GuildsDB.Field.PERMISSIONS_OBJECT.toString())
                        .has(String.valueOf(code))) {
                    globalChangesMade = true;
                    localChangesMade = true;
                    guildObj.getJSONObject(GuildsDB.Field.PERMISSIONS_OBJECT.toString())
                            .put(String.valueOf(code), new JSONArray());
                }

            if (!guildObj.getJSONObject(GuildsDB.Field.PERMISSIONS_OBJECT.toString())
                    .has(PermissionConfigField.USER_PERMISSIONS.toString())) {
                globalChangesMade = true;
                localChangesMade = true;
                guildObj.getJSONObject(GuildsDB.Field.PERMISSIONS_OBJECT.toString())
                        .put(PermissionConfigField.USER_PERMISSIONS.toString(), new JSONObject());
            }

            if (localChangesMade) objectToUpdate.add(guildObj);
        }
        if (globalChangesMade) getCache().updateCacheObjects(objectToUpdate);
    }
}
