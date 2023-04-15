package main.utils.json.permissions

import main.constants.PermissionKt
import main.utils.database.mongodb.cache.GuildDBCacheKt
import main.utils.database.mongodb.databases.GuildDBKt
import main.utils.json.AbstractGuildConfigKt
import net.dv8tion.jda.api.entities.Guild
import org.bson.Document
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*

class PermissionsConfigKt(private val guild: Guild) : AbstractGuildConfigKt(guild) {
    fun addPermissionToUser(userID: Long, p: PermissionKt) {
        var obj = getGuildObject()
        var usersObj = obj.getJSONObject(GuildDBKt.Field.PERMISSIONS_OBJECT.toString())
            .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString())
        try {
            require(!userHasPermission(userID, p)) { "User with id \"" + userID + "\" already has " + p.name + "" }
        } catch (e: NullPointerException) {
            usersObj.put(userID.toString(), JSONArray())
            cache.updateCache(obj, GuildDBKt.Field.GUILD_ID, guild.idLong)
        }
        obj = getGuildObject()
        usersObj = obj.getJSONObject(GuildDBKt.Field.PERMISSIONS_OBJECT.toString())
            .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString())
        val array: JSONArray = try {
            usersObj.getJSONArray(userID.toString())
        } catch (e: JSONException) {
            usersObj.put(userID.toString(), JSONArray())
            usersObj.getJSONArray(userID.toString())
        }
        array.put(p.code)
        usersObj.put(userID.toString(), array)
        cache.updateCache(obj, GuildDBKt.Field.GUILD_ID, guild.idLong)
    }

    fun userHasPermission(userID: Long, p: PermissionKt): Boolean {
        val userObj = getGuildObject()
            .getJSONObject(GuildDBKt.Field.PERMISSIONS_OBJECT.toString())
            .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString())
        return if (!userObj.has(userID.toString())) false else userObj.getJSONArray(userID.toString()).toList()
            .contains(p.code)
    }

    fun removePermissionFromUser(userID: Long, p: PermissionKt) {
        require(userHasPermission(userID, p)) { "User with id \"$userID\" doesn't have ${p.name}" }
        val obj = getGuildObject()
        val usersObj = obj.getJSONObject(GuildDBKt.Field.PERMISSIONS_OBJECT.toString())
            .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString())
        val array = usersObj.getJSONArray(userID.toString())
        array.remove(getIndexOfObjectInArray(array, p.code))
        usersObj.put(userID.toString(), array)
        cache.updateCache(obj, GuildDBKt.Field.GUILD_ID, guild.idLong)
    }

    @Throws(IllegalAccessException::class, IOException::class)
    fun removeRoleFromPermission(rid: Long, p: PermissionKt) {
        if (!getRolesForPermission(p).contains(rid)) throw IllegalAccessException("The role $rid doesn't have access to PermissionKt with code ${p.code}")
        val obj = getGuildObject()
        val permArr = obj.getJSONObject(GuildDBKt.Field.PERMISSIONS_OBJECT.toString())
            .getJSONArray(p.code.toString())
        permArr.remove(getIndexOfObjectInArray(permArr, rid))
        cache.updateCache(obj, GuildDBKt.Field.GUILD_ID, guild.idLong)
    }

    @Throws(IllegalAccessException::class)
    fun addRoleToPermission(rid: Long, p: PermissionKt) {
        if (getRolesForPermission(p).contains(rid)) throw IllegalAccessException("The role $rid already has access to PermissionKt with code ${p.code}")
        val obj = getGuildObject()
        val permArr = obj.getJSONObject(GuildDBKt.Field.PERMISSIONS_OBJECT.toString())
            .getJSONArray(p.code.toString())
        permArr.put(rid)
        cache.updateCache<Long>(obj, GuildDBKt.Field.GUILD_ID, guild.idLong)
    }

    fun getRolesForPermission(p: PermissionKt): List<Long> {
        val ret: MutableList<Long> = ArrayList()
        val obj = getGuildObject()
            .getJSONObject(GuildDBKt.Field.PERMISSIONS_OBJECT.toString())

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
        if (!PermissionKt.permissions.contains(p.uppercase(Locale.getDefault())))
            throw NullPointerException("There is no enum with the name \"$p\"")

        val code = PermissionKt.values()
            .firstOrNull { permission -> permission.name.equals(p, ignoreCase = true) }
            ?.code ?: -1

        return getRolesForPermission(PermissionKt.parse(code))
    }

    fun getUsersForPermission(p: String): List<Long> {
        if (!PermissionKt.permissions.contains(p.uppercase(Locale.getDefault())))
            throw NullPointerException("There is no enum with the name \"$p\"")

        val code = PermissionKt.values()
            .firstOrNull { permission -> permission.name.equals(p, ignoreCase = true) }
            ?.code ?: -1

        val ret: MutableList<Long> = ArrayList()
        return try {
            val obj = getGuildObject()
                .getJSONObject(GuildDBKt.Field.PERMISSIONS_OBJECT.toString())
                .getJSONObject(PermissionConfigField.USER_PERMISSIONS.toString())
            for (s in obj.keySet())
                if (obj.getJSONArray(s).toList().contains(code))
                    ret.add(s.toLong())
            ret
        } catch (e: JSONException) {
            val obj = getGuildObject()
            obj.getJSONObject(GuildDBKt.Field.PERMISSIONS_OBJECT.toString())
                .put(PermissionConfigField.USER_PERMISSIONS.toString(), JSONObject())
            cache.updateCache(obj, GuildDBKt.Field.GUILD_ID, guild.idLong)
            ret
        }
    }

    fun getPermissionsForRoles(rid: Long): List<Int> {
        val codes: MutableList<Int> = ArrayList()
        val obj = getGuildObject()
        val permObj = obj.getJSONObject(GuildDBKt.Field.PERMISSIONS_OBJECT.toString())
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
        val obj = getGuildObject()
            .getJSONObject(GuildDBKt.Field.PERMISSIONS_OBJECT.toString())
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
        val cacheArr = GuildDBCacheKt.ins!!.getCache()
        val obj = cacheArr.getJSONObject(getIndexOfObjectInArray(cacheArr, GuildDBKt.Field.GUILD_ID, guild.idLong))

        for (code in PermissionKt.codes)
            if (!obj.getJSONObject(GuildDBKt.Field.PERMISSIONS_OBJECT.toString()).has(code.toString())) {
                obj.getJSONObject(GuildDBKt.Field.PERMISSIONS_OBJECT.toString())
                    .put(code.toString(), JSONArray())
            }

        if (!obj.getJSONObject(GuildDBKt.Field.PERMISSIONS_OBJECT.toString())
                .has(PermissionConfigField.USER_PERMISSIONS.toString())
        ) {
            obj.getJSONObject(GuildDBKt.Field.PERMISSIONS_OBJECT.toString())
                .put(PermissionConfigField.USER_PERMISSIONS.toString(), JSONObject())
        }

        cache.updateCache(guild.idLong.toString(), Document.parse(obj.toString()))
    }
}