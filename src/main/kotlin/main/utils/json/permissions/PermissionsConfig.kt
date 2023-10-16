package main.utils.json.permissions

import main.constants.RobertifyPermission
import main.utils.database.mongodb.cache.GuildDBCache
import main.utils.database.mongodb.databases.GuildDB
import main.utils.json.AbstractGuildConfig
import main.utils.json.getIndexOfObjectInArray
import main.utils.json.remove
import net.dv8tion.jda.api.entities.Guild
import org.bson.Document
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*

class PermissionsConfig(private val guild: Guild) : AbstractGuildConfig(guild) {
    fun addPermissionToUser(userID: Long, p: RobertifyPermission) {
        var obj = getGuildModel().toJsonObject()
        var usersObj = obj.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
            .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString())
        try {
            require(!userHasPermission(userID, p)) { "User with id \"" + userID + "\" already has " + p.name + "" }
        } catch (e: NullPointerException) {
            usersObj.put(userID.toString(), JSONArray())
            cache.updateCache(obj, GuildDB.Field.GUILD_ID, guild.idLong)
        }
        obj = getGuildModel().toJsonObject()
        usersObj = obj.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
            .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString())
        val array: JSONArray = try {
            usersObj.getJSONArray(userID.toString())
        } catch (e: JSONException) {
            usersObj.put(userID.toString(), JSONArray())
            usersObj.getJSONArray(userID.toString())
        }
        array.put(p.code)
        usersObj.put(userID.toString(), array)
        cache.updateCache(obj, GuildDB.Field.GUILD_ID, guild.idLong)
    }

    fun userHasPermission(userID: Long, p: RobertifyPermission): Boolean {
        val userObj = getGuildModel().toJsonObject()
            .getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
            .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString())
        return if (!userObj.has(userID.toString())) false else userObj.getJSONArray(userID.toString()).toList()
            .contains(p.code)
    }

    fun removePermissionFromUser(userID: Long, p: RobertifyPermission) {
        require(userHasPermission(userID, p)) { "User with id \"$userID\" doesn't have ${p.name}" }
        val obj = getGuildModel().toJsonObject()
        val usersObj = obj.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
            .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString())
        val array = usersObj.getJSONArray(userID.toString())
        array.remove(p.code)
        usersObj.put(userID.toString(), array)
        cache.updateCache(obj, GuildDB.Field.GUILD_ID, guild.idLong)
    }

    @Throws(IllegalAccessException::class, IOException::class)
    fun removeRoleFromPermission(rid: Long, p: RobertifyPermission) {
        if (!getRolesForPermission(p).contains(rid)) throw IllegalAccessException("The role $rid doesn't have access to PermissionKt with code ${p.code}")
        val obj = getGuildModel().toJsonObject()
        val permArr = obj.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
            .getJSONArray(p.code.toString())
        permArr.remove(getIndexOfObjectInArray(permArr, rid))
        cache.updateCache(obj, GuildDB.Field.GUILD_ID, guild.idLong)
    }

    @Throws(IllegalAccessException::class)
    fun addRoleToPermission(rid: Long, p: RobertifyPermission) {
        if (getRolesForPermission(p).contains(rid)) throw IllegalAccessException("The role $rid already has access to PermissionKt with code ${p.code}")
        val obj = getGuildModel().toJsonObject()
        val permArr = obj.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
            .getJSONArray(p.code.toString())
        permArr.put(rid)
        cache.updateCache(obj, GuildDB.Field.GUILD_ID, guild.idLong)
    }

    fun getRolesForPermission(p: RobertifyPermission): List<Long> {
        val ret: MutableList<Long> = ArrayList()
        val obj = getGuildModel().toJsonObject()
            .getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())

        val arr: JSONArray = try {
            obj.getJSONArray(p.code.toString())
        } catch (e: JSONException) {
            obj.put(p.code.toString(), JSONArray())
            obj.getJSONArray(p.code.toString())
        }

        for (i in 0 until arr.length())
            ret.add(arr.getLong(i))
        return ret
    }

    fun getRolesForPermission(p: String): List<Long> {
        if (!RobertifyPermission.permissions.contains(p.uppercase(Locale.getDefault())))
            throw NullPointerException("There is no enum with the name \"$p\"")

        val code = RobertifyPermission.entries
            .firstOrNull { permission -> permission.name.equals(p, ignoreCase = true) }
            ?.code ?: -1

        return getRolesForPermission(RobertifyPermission.parse(code))
    }

    fun getUsersForPermission(p: RobertifyPermission): List<Long> {
        val code = p.code
        val ret: MutableList<Long> = ArrayList()
        return try {
            val obj = getGuildModel().toJsonObject()
                .getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
                .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString())
            for (s in obj.keySet())
                if (obj.getJSONArray(s).toList().contains(code))
                    ret.add(s.toLong())
            ret
        } catch (e: JSONException) {
            val obj = getGuildModel().toJsonObject()
            obj.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
                .put(PermissionConfigField.USER_PERMISSIONS.toString(), JSONObject())
            cache.updateCache(obj, GuildDB.Field.GUILD_ID, guild.idLong)
            ret
        }
    }

    fun getUsersForPermission(p: String): List<Long> {
        if (!RobertifyPermission.permissions.contains(p.uppercase(Locale.getDefault())))
            throw NullPointerException("There is no enum with the name \"$p\"")
        return getUsersForPermission(RobertifyPermission.valueOf(p.uppercase()))
    }

    fun getPermissionsForRoles(rid: Long): List<Int> {
        val codes: MutableList<Int> = ArrayList()
        val obj = getGuildModel().toJsonObject()
        val permObj = obj.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
        for (i in 0 until permObj.length() - 1) {
            val arr: JSONArray = try {
                permObj.getJSONArray(i.toString())
            } catch (e: JSONException) {
                update()
                try {
                    permObj.getJSONArray(i.toString())
                } catch (e2: JSONException) {
                    continue
                }
            }
            for (j in 0 until arr.length()) if (arr.getLong(j) == rid) {
                codes.add(i)
                break
            }
        }
        return codes
    }

    fun getPermissionsForUser(uid: Long): List<Int> {
        val codes: MutableList<Int> = ArrayList()
        val obj = getGuildModel().toJsonObject()
            .getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
            .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString())
        val arr: JSONArray = try {
            obj.getJSONArray(uid.toString())
        } catch (e: JSONException) {
            return ArrayList()
        }
        for (i in 0 until arr.length()) codes.add(arr.getInt(i))
        return codes
    }

    override fun update() {
        if (!guildHasInfo()) loadGuild()
        val cacheArr = GuildDBCache.ins.getCache()
        val obj = cacheArr.getJSONObject(getIndexOfObjectInArray(cacheArr, GuildDB.Field.GUILD_ID, guild.idLong))

        for (code in RobertifyPermission.codes)
            if (!obj.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString()).has(code.toString())) {
                obj.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
                    .put(code.toString(), JSONArray())
            }

        if (!obj.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
                .has(PermissionConfigField.USER_PERMISSIONS.toString())
        ) {
            obj.getJSONObject(GuildDB.Field.PERMISSIONS_OBJECT.toString())
                .put(PermissionConfigField.USER_PERMISSIONS.toString(), JSONObject())
        }

        cache.updateCache(guild.idLong.toString(), Document.parse(obj.toString()))
    }
}