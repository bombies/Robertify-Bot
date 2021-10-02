package main.utils.json.permissions;

import lombok.SneakyThrows;
import main.commands.commands.management.permissions.Permission;
import main.constants.ENV;
import main.constants.JSONConfigFile;
import main.main.Config;
import main.utils.database.BotUtils;
import main.utils.json.JSONConfig;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class PermissionsConfig extends JSONConfig {
    public PermissionsConfig() {
        super(JSONConfigFile.PERMISSIONS);
    }

    @SneakyThrows
    @Override
    public void initConfig() {
        boolean newConfig = false;

        if (!Files.exists(Config.getPath(ENV.JSON_DIR, JSONConfigFile.PERMISSIONS))) {
            new File(String.valueOf(Config.getPath(ENV.JSON_DIR, JSONConfigFile.PERMISSIONS)))
                    .createNewFile();
            newConfig = true;
        }

        if (!newConfig) return;

        JSONObject obj = new JSONObject();

        for (Guild guild : new BotUtils().getGuilds()) {
            JSONObject serverObj = new JSONObject();
            for (int i : Permission.getCodes())
                serverObj.put(String.valueOf(i), new JSONArray());
            obj.put(guild.getId(), serverObj);
        }

        setJSON(obj);
    }

    /**
     * Initialize a new guild in the JSON file.
     * @param gid ID of the guild
     * @throws IOException
     */
    public void initGuild(String gid) throws IOException {
        JSONObject obj = getJSONObject();

        if (obj.has(gid)) return;
        
        JSONObject serverObj = new JSONObject();
        for (int i : Permission.getCodes())
            serverObj.put(String.valueOf(i), new JSONArray());
        obj.put(gid, serverObj);

        setJSON(obj);
    }

    /**
     * Remove a guild from the JSON file
     * @param gid ID of the guild
     * @throws IOException
     */
    public void removeGuild(String gid) throws IOException {
        JSONObject obj = getJSONObject();
        obj.remove(gid);
        setJSON(obj);
    }

    /**
     * Adds a specific Permissions to all servers
     * @param p Permission to be added
     * @throws IOException
     */
    public void addPermission(Permission p) throws IOException {
        JSONObject obj = getJSONObject();
        for (Guild g : new BotUtils().getGuilds())
            obj.getJSONObject(g.getId()).put(String.valueOf(p.getCode()), new JSONArray());
        setJSON(obj);
    }

    /**
     * Adds a specific Permission to a specific server
     * @param gid ID of the guild
     * @param p Permission to be added
     * @throws IOException
     */
    public void addPermission(String gid, Permission p) throws IOException {
        JSONObject obj = getJSONObject();
        obj.getJSONObject(gid).put(String.valueOf(p.getCode()), new JSONArray());
        setJSON(obj);
    }

    /**
     * Add multiple Permissions to all servers
     * @param p Permission to be added
     * @throws IOException
     */
    public void addPermissions(Permission... p) throws IOException {
        JSONObject obj = getJSONObject();
        for (Permission perm : p) {
            for (Guild g : new BotUtils().getGuilds())
                obj.getJSONObject(g.getId()).put(String.valueOf(perm.getCode()), new JSONArray());
        }
        setJSON(obj);
    }

    /**
     * Adds specific Permissions to a specific server
     * @param gid ID of the guild
     * @param p Permissions to be added
     * @throws IOException
     */
    public void addPermissions(String gid, Permission... p) throws IOException {
        JSONObject obj = getJSONObject();
        for (Permission perm : p)
            obj.getJSONObject(gid).put(String.valueOf(perm.getCode()), new JSONArray());
        setJSON(obj);
    }

    /**
     * Gives a specific role access to a specific Permission set in a specific server
     * @param gid ID of the guild
     * @param rid ID of the role
     * @param p Permission to be added
     */
    public void addRoleToPermission(String gid, String rid, Permission p) throws IllegalAccessException, IOException {
        if (getRolesForPermission(gid, p).contains(rid))
            throw new IllegalAccessException("The role "+rid+" already has access to Permission with code "+ p.getCode());

        JSONObject obj = getJSONObject();
        JSONArray permArr = obj.getJSONObject(gid).getJSONArray(String.valueOf(p.getCode()));
        permArr.put(rid);
        setJSON(obj);
    }

    /**
     * Gives a specific role in a specifc server access to multiple Permission sets
     * @param gid ID of the guild
     * @param rid ID of the role
     * @param p Permissions to be added
     */
    public void addRoleToPermissions(String gid, String rid, Permission... p) throws IOException {
        JSONObject obj = getJSONObject();
        for (Permission perm : p) {
            if (!getRolesForPermission(gid, perm).contains(rid)) {
                JSONArray permArr = obj.getJSONObject(gid).getJSONArray(String.valueOf(perm.getCode()));
                permArr.put(rid);
            }
        }
        setJSON(obj);
    }

    /**
     * Remove a specific role from a Permission in a specific server
     * @param gid ID of the guild
     * @param rid ID of the role
     * @param p Permission to be removed
     * @throws IllegalAccessException
     * @throws IOException
     */
    public void removeRoleFromPermission(String gid, String rid, Permission p) throws IllegalAccessException, IOException {
        if (!getRolesForPermission(gid, p).contains(rid))
            throw new IllegalAccessException("The role "+rid+" doesn't have access to Permission with code "+p.getCode());

        JSONObject obj = getJSONObject();
        JSONArray permArr = obj.getJSONObject(gid).getJSONArray(String.valueOf(p.getCode()));
        permArr.remove(getIndexOfObjectInArray(permArr, rid));
        setJSON(obj);
    }

    /**
     * Removes specific Permissions from a specific role in a specific server
     * @param gid ID of the guild
     * @param rid ID of the role
     * @param p Permissions to be removed
     * @throws IOException
     */
    public void removeRoleFromPermissions(String gid, String rid, Permission... p) throws IOException {
        JSONObject obj = getJSONObject();
        for (Permission perm : p) {
            if (getRolesForPermission(gid, perm).contains(rid)) {
                JSONArray permArr = obj.getJSONObject(gid).getJSONArray(String.valueOf(perm.getCode()));
                permArr.remove(getIndexOfObjectInArray(permArr, rid));
            }
        }
        setJSON(obj);
    }

    /**
     * Gets all the roles with access to a specific Permission
     * @param gid ID of the guild
     * @param p Permission to query
     * @return A list of roles with access to the passed Permission
     */
    public List<String> getRolesForPermission(String gid, Permission p) {
        List<String> ret = new ArrayList<>();
        JSONArray arr = getJSONObject()
                .getJSONObject(gid)
                .getJSONArray(String.valueOf(p.getCode()));
        for (int i = 0; i < arr.length(); i++)
            ret.add(arr.getString(i));
        return ret;
    }

    public List<String> getRolesForPermission(String gid, String p) {
        if (!Permission.getPermissions().contains(p.toUpperCase()))
            throw new NullPointerException("There is no enum with the name \""+p+"\"");

        int code = -1;

        for (Permission perm : Permission.values())
            if (perm.name().equalsIgnoreCase(p)) {
                code = perm.getCode(); break;
            }

        List<String> ret = new ArrayList<>();
        JSONArray arr = getJSONObject()
                .getJSONObject(gid)
                .getJSONArray(String.valueOf(code));
        for (int i = 0; i < arr.length(); i++)
            ret.add(arr.getString(i));
        return ret;
    }

    /**
     * Gets all the Permission that a specific role has in a specific server.
     * @param gid ID of the guild
     * @param rid ID of the role
     * @return A list of integer codes of the Permission the role has access to
     */
    public List<Integer> getPermissionsForRoles(String gid, String rid) {
        List<Integer> codes = new ArrayList<>();
        JSONObject obj = getJSONObject()
                .getJSONObject(gid);

        for (int i = 0; i < obj.length(); i++) {
            JSONArray arr = obj.getJSONArray(String.valueOf(i));
            for (int j = 0; j < arr.length(); j++)
                if (arr.getString(j).equals(rid)) {
                    codes.add(i); break;
                }
        }

        return codes;
    }
}
