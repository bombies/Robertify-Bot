package main.utils.json.toggles

import main.commands.slashcommands.SlashCommandManager
import main.constants.Toggle
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.database.mongodb.cache.GuildDBCache
import main.utils.database.mongodb.databases.GuildDB
import main.utils.json.AbstractGuildConfig
import main.utils.json.logs.LogType
import net.dv8tion.jda.api.entities.Guild
import org.bson.Document
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class TogglesConfig(private val guild: Guild) : AbstractGuildConfig(guild) {

    companion object {
        fun getDefaultToggleObject(): JSONObject {
            val toggleObj = JSONObject()
            for (toggle in Toggle.values()) try {
                getTogglesObject(toggleObj, toggle)
            } catch (e: JSONException) {
                for (errToggles in Toggle.values()) when (errToggles) {
                    Toggle.RESTRICTED_VOICE_CHANNELS, Toggle.RESTRICTED_TEXT_CHANNELS -> toggleObj.put(
                        errToggles.toString(),
                        false
                    )

                    else -> toggleObj.put(errToggles.toString(), true)
                }
            }
            return toggleObj
        }

        private fun getTogglesObject(toggleObj: JSONObject, toggle: Toggle) {
            if (!toggleObj.has(toggle.toString())) when (toggle) {
                Toggle.RESTRICTED_VOICE_CHANNELS, Toggle.RESTRICTED_TEXT_CHANNELS ->
                    toggleObj.put(toggle.toString(), false)

                else -> toggleObj.put(toggle.toString(), true)
            }

            if (!toggleObj.has(Toggle.TogglesConfigField.DJ_TOGGLES.toString())) {
                val djTogglesObj = JSONObject()
                for (musicCommand in SlashCommandManager.musicCommands) djTogglesObj.put(
                    musicCommand.info.name.lowercase(
                        Locale.getDefault()
                    ), false
                )
                toggleObj.put(Toggle.TogglesConfigField.DJ_TOGGLES.toString(), djTogglesObj)
            } else {
                val djTogglesObj = toggleObj.getJSONObject(Toggle.TogglesConfigField.DJ_TOGGLES.toString())
                for (musicCommand in SlashCommandManager.musicCommands)
                    if (!djTogglesObj.has(musicCommand.info.name))
                        djTogglesObj.put(musicCommand.info.name, false)
                toggleObj.put(Toggle.TogglesConfigField.DJ_TOGGLES.toString(), djTogglesObj)
            }

            if (!toggleObj.has(Toggle.TogglesConfigField.LOG_TOGGLES.toString())) {
                val logObj = JSONObject()
                for (type in LogType.values()) logObj.put(type.name.lowercase(Locale.getDefault()), true)
                toggleObj.put(Toggle.TogglesConfigField.LOG_TOGGLES.toString(), logObj)
            } else {
                val logObj = toggleObj.getJSONObject(Toggle.TogglesConfigField.LOG_TOGGLES.toString())
                for (type in LogType.values()) if (!logObj.has(type.name.lowercase(Locale.getDefault()))) logObj.put(
                    type.name.lowercase(
                        Locale.getDefault()
                    ), true
                )
                toggleObj.put(Toggle.TogglesConfigField.LOG_TOGGLES.toString(), logObj)
            }
        }
    }

    operator fun get(toggle: Toggle): Boolean {
        if (!guildHasInfo()) loadGuild()
        return try {
            getTogglesObject().getBoolean(toggle.toString())
        } catch (e: JSONException) {
            if (e.message!!.contains("is not a")) {
                getTogglesObject().getBoolean(toggle.toString())
            } else {
                val togglesObject = getTogglesObject()
                togglesObject.put(toggle.toString(), true)
                cache.setField(guild.idLong, GuildDB.Field.TOGGLES_OBJECT, togglesObject)
                true
            }
        }
    }

    fun getToggle(toggle: Toggle): Boolean = get(toggle)

    fun getDJToggles(): HashMap<String, Boolean> {
        val ret = HashMap<String, Boolean>()
        val obj = getTogglesObject()
            .getJSONObject(GuildDB.Field.TOGGLES_DJ.toString())
        for (key in obj.keySet())
            ret[key] = false
        ret.replaceAll { k, _ -> obj.getBoolean(k) }
        return ret
    }

    fun getLogToggles(): HashMap<String, Boolean> {
        val ret = HashMap<String, Boolean>()
        val obj = getTogglesObject()
            .getJSONObject(Toggle.TogglesConfigField.LOG_TOGGLES.toString())
        for (key in obj.keySet()) ret[key] = true
        ret.replaceAll { k, _ -> obj.getBoolean(k) }
        return ret
    }

    fun getDJToggle(cmd: AbstractSlashCommand): Boolean {
        if (!SlashCommandManager.isMusicCommand(cmd))
            return false

        val djToggles = getDJToggles()
        return if (!djToggles.containsKey(cmd.info.name)) {
            setDJToggle(cmd, false)
            return false
        } else djToggles[cmd.info.name.lowercase(Locale.getDefault())]!!
    }

    fun setToggle(toggle: Toggle, value: Boolean) {
        val obj = getGuildObject()
        obj.getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString()).put(toggle.toString(), value)
        cache.updateGuild(obj, guild.idLong)
    }

    fun setDJToggle(command: AbstractSlashCommand, value: Boolean) {
        val obj = getGuildObject()
        obj.getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString())
            .getJSONObject(GuildDB.Field.TOGGLES_DJ.toString())
            .put(command.info.name, value)
        cache.updateGuild(obj, guild.idLong)
    }

    fun setDJToggle(commands: List<AbstractSlashCommand>, value: Boolean) {
        val obj = getGuildObject()
        val djObj = obj.getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString())
            .getJSONObject(GuildDB.Field.TOGGLES_DJ.toString())
        commands.forEach { command -> djObj.put(command.info.name, value) }
        cache.updateGuild(obj, guild.idLong)
    }

    fun setLogToggle(type: LogType, state: Boolean) {
        if (!isLogToggleSet(type)) update()
        val obj = getGuildObject()
        obj.getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString())
            .getJSONObject(GuildDB.Field.TOGGLES_LOGS.toString())
            .put(type.name.lowercase(Locale.getDefault()), state)
        cache.updateGuild(obj, guild.idLong)
    }

    fun getLogToggle(type: LogType): Boolean {
        if (!isLogToggleSet(type)) update()
        val obj = getGuildObject()
        return obj.getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString())
            .getJSONObject(GuildDB.Field.TOGGLES_LOGS.toString())
            .getBoolean(type.name.lowercase(Locale.getDefault()))
    }

    fun isDJToggleSet(cmd: AbstractSlashCommand): Boolean =
        getDJToggles().containsKey(cmd.info.name.lowercase(Locale.getDefault()))


    fun isDJToggleSet(cmd: String): Boolean =
        getDJToggles().containsKey(cmd.lowercase(Locale.getDefault()))

    fun isLogToggleSet(type: LogType): Boolean = try {
        getLogToggles().containsKey(type.name.lowercase(Locale.getDefault()))
    } catch (e: JSONException) {
        false
    }

    private fun getTogglesObject(): JSONObject =
        getGuildObject().getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString())

    override fun update() {
        if (!guildHasInfo()) loadGuild()
        val cacheArr = GuildDBCache.ins.getCache()
        val `object` = cacheArr.getJSONObject(getIndexOfObjectInArray(cacheArr, GuildDB.Field.GUILD_ID, guild.idLong))
        for (toggle in Toggle.values()) {
            try {
                val toggleObj = `object`.getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString())
                getTogglesObject(toggleObj, toggle)
            } catch (e: JSONException) {
                for (errToggles in Toggle.values()) when (errToggles) {
                    Toggle.RESTRICTED_VOICE_CHANNELS, Toggle.RESTRICTED_TEXT_CHANNELS -> `object`.getJSONObject(
                        GuildDB.Field.TOGGLES_OBJECT.toString()
                    )
                        .put(errToggles.toString(), false)

                    else -> `object`.getJSONObject(GuildDB.Field.TOGGLES_OBJECT.toString())
                        .put(errToggles.toString(), true)
                }
            }
        }
        cache.updateCache(guild.id, Document.parse(`object`.toString()))
    }
}