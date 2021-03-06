package main.utils.json.permissions;

import main.constants.Permission;
import main.utils.database.mongodb.cache.GuildDBCache;
import main.utils.database.mongodb.databases.GuildDB;
import main.utils.json.AbstractGuildConfig;
import net.dv8tion.jda.api.entities.Guild;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PermissionsConfig extends AbstractGuildConfig {
    private final Guild guild;
    private final long gid;
    
    public PermissionsConfig(Guild guild) {
        super(guild);
        this.guild = guild;
        this.gid = guild.getIdLong();
    }

    public void addPermissionToUser(long userID, Permission p) {
        var obj = getGuildObject();
        var usersObj = obj.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
                .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString());

        try {
            if (userHasPermission(userID, p))
                throw new IllegalArgumentException("User with id \"" + userID + "\" already has " + p.name() + "");
        } catch (NullPointerException e) {
            usersObj.put(String.valueOf(userID), new JSONArray());
            getCache().updateCache(obj, GuildDB.Field.GUILD_ID, gid);
        }

        obj = getGuildObject();
        usersObj = obj.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
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

        getCache().updateCache(obj, GuildDB.Field.GUILD_ID, gid);
    }

    public boolean userHasPermission(long userID, Permission p) {
        var userObj = getGuildObject()
                .getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
                .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString());

        if (!userObj.has(String.valueOf(userID)))
            return false;

        return userObj.getJSONArray(String.valueOf(userID)).toList().contains(p.getCode());
    }

    public void removePermissionFromUser(long userID, Permission p) {
        if (!userHasPermission(userID, p))
            throw new IllegalArgumentException("User with id \""+userID+"\" doesn't have "+p.name()+"");

        var obj = getGuildObject();
        var usersObj = obj.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
                .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString());

        JSONArray array = usersObj.getJSONArray(String.valueOf(userID));
        array.remove(getIndexOfObjectInArray(array, p.getCode()));

        usersObj.put(String.valueOf(userID), array);
        getCache().updateCache(obj, GuildDB.Field.GUILD_ID, gid);
    }

    public void removeRoleFromPermission(long rid, Permission p) throws IllegalAccessException, IOException {
        if (!getRolesForPermission(p).contains(rid))
            throw new IllegalAccessException("The role "+rid+" doesn't have access to Permission with code "+p.getCode());

        JSONObject obj = getGuildObject();
        JSONArray permArr = obj.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
                .getJSONArray(String.valueOf(p.getCode()));
        permArr.remove(getIndexOfObjectInArray(permArr, rid));
        getCache().updateCache(obj, GuildDB.Field.GUILD_ID, gid);
    }

    public void addRoleToPermission(long rid, Permission p) throws IllegalAccessException {
        if (getRolesForPermission(p).contains(rid))
            throw new IllegalAccessException("The role "+rid+" already has access to Permission with code "+ p.getCode());

        JSONObject obj = getGuildObject();
        JSONArray permArr = obj.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
                .getJSONArray(String.valueOf(p.getCode()));
        permArr.put(rid);

        getCache().updateCache(obj, GuildDB.Field.GUILD_ID, gid);
    }

    public List<Long> getRolesForPermission(Permission p) {
        List<Long> ret = new ArrayList<>();
        JSONObject object = getGuildObject()
                .getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString());

        JSONArray arr;

        try {
            arr = object.getJSONArray(String.valueOf(p.getCode()));
        } catch (JSONException e) {
            object.put(String.valueOf(p.getCode()), new JSONArray());
            arr = object.getJSONArray(String.valueOf(p.getCode()));
        }

        for (int i = 0; i < arr.length(); i++)
            ret.add(arr.getLong(i));

        return ret;
    }

    public List<Long> getRolesForPermission(String p) {
        if (!Permission.getPermissions().contains(p.toUpperCase()))
            throw new NullPointerException("There is no enum with the name \""+p+"\"");

        int code = -1;

        for (Permission perm : Permission.values())
            if (perm.name().equalsIgnoreCase(p)) {
                code = perm.getCode(); break;
            }

        return getRolesForPermission(Permission.parse(code));
    }

    public List<Long> getUsersForPermission(String p) {
        if (!Permission.getPermissions().contains(p.toUpperCase()))
            throw new NullPointerException("There is no enum with the name \""+p+"\"");

        int code = -1;

        for (Permission perm : Permission.values())
            if (perm.name().equalsIgnoreCase(p)) {
                code = perm.getCode(); break;
            }

        List<Long> ret = new ArrayList<>();

        try {
            JSONObject object = getGuildObject()
                    .getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
                    .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString());

            for (String s : object.keySet())
                if (object.getJSONArray(s).toList().contains(code))
                    ret.add(Long.parseLong(s));

            return ret;
        } catch (JSONException e) {
            final var obj = getGuildObject();
            obj.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
                    .put(PermissionConfigField.USER_PERMISSIONS.toString(), new JSONObject());
            getCache().updateCache(obj, GuildDB.Field.GUILD_ID, gid);
            return ret;
        }
    }

    public List<Integer> getPermissionsForRoles(long rid) {
        List<Integer> codes = new ArrayList<>();
        JSONObject obj = getGuildObject();
        final var permObj = obj.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString());

        for (int i = 0; i < permObj.length()-1; i++) {
            JSONArray arr;
            try {
                arr = permObj.getJSONArray(String.valueOf(i));
            } catch (JSONException e) {
                update();
                try {
                    arr = permObj.getJSONArray(String.valueOf(i));
                } catch (JSONException e2) {
                    continue;
                }
            }

            for (int j = 0; j < arr.length(); j++)
                if (arr.getLong(j) == rid) {
                    codes.add(i); break;
                }
        }

        return codes;
    }

    public List<Integer> getPermissionsForUser(long uid) {
        List<Integer> codes = new ArrayList<>();
        JSONObject obj = getGuildObject()
                .getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
                .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString());

        JSONArray arr;
        try {
            arr = obj.getJSONArray(String.valueOf(uid));
        } catch (JSONException e) {
            return new ArrayList<>();
        }

        for (int i = 0; i < arr.length(); i++)
            codes.add(arr.getInt(i));

        return codes;
    }

    @Override
    public void update() {
        if (!guildHasInfo())
            loadGuild();

        final JSONArray cacheArr = GuildDBCache.getInstance().getCache();
        JSONObject object = cacheArr.getJSONObject(getIndexOfObjectInArray(cacheArr, GuildDB.Field.GUILD_ID, gid));

        for (int code : Permission.getCodes())
            if (!object.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
                    .has(String.valueOf(code))) {
                object.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
                        .put(String.valueOf(code), new JSONArray());
            }

        if (!object.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
                .has(PermissionConfigField.USER_PERMISSIONS.toString())) {
            object.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
                    .put(PermissionConfigField.USER_PERMISSIONS.toString(), new JSONObject());
        }

        getCache().updateCache(String.valueOf(gid), Document.parse(object.toString()));
    }
}
