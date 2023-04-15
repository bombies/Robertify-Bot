package main.utils.json.toggles

import main.commands.slashcommands.SlashCommandManager
import main.constants.ToggleKt
import main.utils.component.interactions.AbstractSlashCommand
import main.utils.database.mongodb.cache.GuildDBCacheKt
import main.utils.database.mongodb.databases.GuildDBKt
import main.utils.json.AbstractGuildConfigKt
import main.utils.json.logs.LogTypeKt
import net.dv8tion.jda.api.entities.Guild
import org.bson.Document
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class TogglesConfigKt(private val guild: Guild) : AbstractGuildConfigKt(guild) {

    fun getToggle(toggle: ToggleKt): Boolean {
        if (!guildHasInfo()) loadGuild()
        return try {
            getTogglesObject().getBoolean(toggle.toString())
        } catch (e: JSONException) {
            if (e.message!!.contains("is not a")) {
                getTogglesObject().getBoolean(toggle.toString())
            } else {
                val togglesObject = getTogglesObject()
                togglesObject.put(toggle.toString(), true)
                cache.setField(guild.idLong, GuildDBKt.Field.TOGGLES_OBJECT, togglesObject)
                true
            }
        }
    }

    fun getDJToggles(): HashMap<String, Boolean> {
        val ret = HashMap<String, Boolean>()
        val obj = getTogglesObject()
            .getJSONObject(GuildDBKt.Field.TOGGLES_DJ.toString())
        for (key in obj.keySet())
            ret[key] = false
        ret.replaceAll { k, _ -> obj.getBoolean(k) }
        return ret
    }

    fun getLogToggles(): HashMap<String, Boolean> {
        val ret = HashMap<String, Boolean>()
        val obj = getTogglesObject()
            .getJSONObject(ToggleKt.TogglesConfigField.LOG_TOGGLES.toString())
        for (key in obj.keySet()) ret[key] = true
        ret.replaceAll { k, _ -> obj.getBoolean(k) }
        return ret
    }

    fun getDJToggle(cmd: AbstractSlashCommand): Boolean {
        val djToggles = getDJToggles()
        return if (!djToggles.containsKey(cmd.name)) {
            if (SlashCommandManager.getInstance().isMusicCommand(cmd)) {
                setDJToggle(cmd, false)
                false
            } else {
                throw NullPointerException("Invalid command passed! [Command: " + cmd.name + "]")
            }
        } else djToggles[cmd.name.lowercase(Locale.getDefault())]!!
    }

    fun setToggle(toggle: ToggleKt, value: Boolean) {
        val obj = getGuildObject()
        obj.getJSONObject(GuildDBKt.Field.TOGGLES_OBJECT.toString()).put(toggle.toString(), value)
        cache.updateGuild(obj, guild.idLong)
    }

    fun setDJToggle(command: AbstractSlashCommand, value: Boolean) {
        val obj = getGuildObject()
        obj.getJSONObject(GuildDBKt.Field.TOGGLES_OBJECT.toString())
            .getJSONObject(GuildDBKt.Field.TOGGLES_DJ.toString())
            .put(command.name, value)
        cache.updateGuild(obj, guild.idLong)
    }

    fun setLogToggle(type: LogTypeKt, state: Boolean) {
        if (!isLogToggleSet(type)) update()
        val obj = getGuildObject()
        obj.getJSONObject(GuildDBKt.Field.TOGGLES_OBJECT.toString())
            .getJSONObject(GuildDBKt.Field.TOGGLES_LOGS.toString())
            .put(type.name.lowercase(Locale.getDefault()), state)
        cache.updateGuild(obj, guild.idLong)
    }

    fun getLogToggle(type: LogTypeKt): Boolean {
        if (!isLogToggleSet(type)) update()
        val obj = getGuildObject()
        return obj.getJSONObject(GuildDBKt.Field.TOGGLES_OBJECT.toString())
            .getJSONObject(GuildDBKt.Field.TOGGLES_LOGS.toString())
            .getBoolean(type.name.lowercase(Locale.getDefault()))
    }

    fun isDJToggleSet(cmd: AbstractSlashCommand): Boolean =
        getDJToggles().containsKey(cmd.name.lowercase(Locale.getDefault()))


    fun isDJToggleSet(cmd: String): Boolean =
        getDJToggles().containsKey(cmd.lowercase(Locale.getDefault()))

    fun isLogToggleSet(type: LogTypeKt): Boolean = try {
        getLogToggles().containsKey(type.name.lowercase(Locale.getDefault()))
    } catch (e: JSONException) {
        false
    }

    private fun getTogglesObject(): JSONObject =
        getGuildObject().getJSONObject(GuildDBKt.Field.TOGGLES_OBJECT.toString())

    override fun update() {
        if (!guildHasInfo()) loadGuild()
        val cacheArr = GuildDBCacheKt.ins!!.getCache()
        val `object` = cacheArr.getJSONObject(getIndexOfObjectInArray(cacheArr, GuildDBKt.Field.GUILD_ID, guild.idLong))
        for (toggle in ToggleKt.values()) {
            try {
                val toggleObj = `object`.getJSONObject(GuildDBKt.Field.TOGGLES_OBJECT.toString())
                getTogglesObject(toggleObj, toggle)
            } catch (e: JSONException) {
                for (errToggles in ToggleKt.values()) when (errToggles) {
                    ToggleKt.RESTRICTED_VOICE_CHANNELS, ToggleKt.RESTRICTED_TEXT_CHANNELS -> `object`.getJSONObject(
                        GuildDBKt.Field.TOGGLES_OBJECT.toString()
                    )
                        .put(errToggles.toString(), false)

                    else -> `object`.getJSONObject(GuildDBKt.Field.TOGGLES_OBJECT.toString())
                        .put(errToggles.toString(), true)
                }
            }
        }
        cache.updateCache(guild.id, Document.parse(`object`.toString()))
    }

    fun getDefaultToggleObject(): JSONObject {
        val toggleObj = JSONObject()
        for (toggle in ToggleKt.values()) try {
            getTogglesObject(toggleObj, toggle)
        } catch (e: JSONException) {
            for (errToggles in ToggleKt.values()) when (errToggles) {
                ToggleKt.RESTRICTED_VOICE_CHANNELS, ToggleKt.RESTRICTED_TEXT_CHANNELS -> toggleObj.put(
                    errToggles.toString(),
                    false
                )

                else -> toggleObj.put(errToggles.toString(), true)
            }
        }
        return toggleObj
    }

    private fun getTogglesObject(toggleObj: JSONObject, toggle: ToggleKt) {
        if (!toggleObj.has(toggle.toString())) when (toggle) {
            ToggleKt.RESTRICTED_VOICE_CHANNELS, ToggleKt.RESTRICTED_TEXT_CHANNELS ->
                toggleObj.put(toggle.toString(), false)
            else -> toggleObj.put(toggle.toString(), true)
        }

        if (!toggleObj.has(ToggleKt.TogglesConfigField.DJ_TOGGLES.toString())) {
            val djTogglesObj = JSONObject()
            for (musicCommand in SlashCommandManager.getInstance().musicCommands) djTogglesObj.put(
                musicCommand.name.lowercase(
                    Locale.getDefault()
                ), false
            )
            toggleObj.put(ToggleKt.TogglesConfigField.DJ_TOGGLES.toString(), djTogglesObj)
        } else {
            val djTogglesObj = toggleObj.getJSONObject(ToggleKt.TogglesConfigField.DJ_TOGGLES.toString())
            // TODO: Use SlashCommandManager Kotlin implementation
            for (musicCommand in SlashCommandManager.getInstance().musicCommands)
                if (!djTogglesObj.has(musicCommand.name))
                    djTogglesObj.put(musicCommand.name, false)
            toggleObj.put(ToggleKt.TogglesConfigField.DJ_TOGGLES.toString(), djTogglesObj)
        }

        if (!toggleObj.has(ToggleKt.TogglesConfigField.LOG_TOGGLES.toString())) {
            val logObj = JSONObject()
            for (type in LogTypeKt.values()) logObj.put(type.name.lowercase(Locale.getDefault()), true)
            toggleObj.put(ToggleKt.TogglesConfigField.LOG_TOGGLES.toString(), logObj)
        } else {
            val logObj = toggleObj.getJSONObject(ToggleKt.TogglesConfigField.LOG_TOGGLES.toString())
            for (type in LogTypeKt.values()) if (!logObj.has(type.name.lowercase(Locale.getDefault()))) logObj.put(
                type.name.lowercase(
                    Locale.getDefault()
                ), true
            )
            toggleObj.put(ToggleKt.TogglesConfigField.LOG_TOGGLES.toString(), logObj)
        }
    }
}